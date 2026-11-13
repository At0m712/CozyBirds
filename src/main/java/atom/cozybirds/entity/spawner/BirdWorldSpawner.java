package atom.cozybirds.entity.spawner;

import atom.cozybirds.CozyBirds;
import atom.cozybirds.entity.BirdEntity;
import atom.cozybirds.entity.magpie.MagpieEntity;
import atom.cozybirds.entity.pigeon.PigeonEntity;
import atom.cozybirds.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public class BirdWorldSpawner {
    private static int tickCounter = 0;

    public static void init() {
        ServerTickEvents.END_LEVEL_TICK.register(BirdWorldSpawner::tickWorld);
    }

    private static void tickWorld(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        // Check if any bird spawning is enabled
        if (!BirdSpawnConfig.PIGEON_SPAWN_ENABLED && !BirdSpawnConfig.MAGPIE_SPAWN_ENABLED) {
            return;
        }

        tickCounter++;
        if (tickCounter % 40 != 0) {
            return;
        }

        RandomSource random = level.getRandom();
        List<ServerPlayer> players = level.players();

        for (ServerPlayer player : players) {
            if (player.isSpectator()) {
                continue;
            }

            AABB area = player.getBoundingBox().inflate(48.0, 32.0, 48.0);
            List<BirdEntity> nearbyBirds = level.getEntitiesOfClass(BirdEntity.class, area);

            if (nearbyBirds.size() < 45) {
                List<MagpieEntity> nearbyMagpies = level.getEntitiesOfClass(MagpieEntity.class, area);
                if (nearbyMagpies.size() < 25) {
                    spawnFlockNearPlayer(level, player, random);
                }
            }
        }
    }

    private static void spawnFlockNearPlayer(ServerLevel level, ServerPlayer player, RandomSource random) {
        long dayTime = level.getOverworldClockTime() % 24000;
        boolean isNight = dayTime >= 12500;

        float playerYaw = player.getYRot();
        net.minecraft.world.phys.Vec3 playerEye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 lookVec = player.getViewVector(1.0F).normalize();

        for (int attempt = 0; attempt < 16; attempt++) {
            // NEVER spawn in the player's field of view!
            // Sample from the rear hemisphere behind the player (180° ± 60° relative to player gaze)
            double rearYawRad = Math.toRadians(playerYaw + 180.0F);
            double angleOffset = (random.nextDouble() - 0.5) * Math.toRadians(120.0);
            double spawnAngle = rearYawRad + angleOffset;

            // Spawn at realistic distance (30 to 55 blocks away behind the player)
            double dist = 30.0 + random.nextDouble() * 25.0;
            double dirX = -Math.sin(spawnAngle);
            double dirZ = Math.cos(spawnAngle);

            int spawnX = (int) (player.getX() + dirX * dist);
            int spawnZ = (int) (player.getZ() + dirZ * dist);

            if (!level.hasChunk(spawnX >> 4, spawnZ >> 4)) {
                continue;
            }

            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, spawnX, spawnZ);
            if (surfaceY < level.getMinY() + 2 || surfaceY > level.getMaxY() - 10) {
                continue;
            }

            // Strict FOV verification: reject any candidate point in the player's visual cone
            net.minecraft.world.phys.Vec3 toSpawn = new net.minecraft.world.phys.Vec3(
                    spawnX + 0.5 - playerEye.x,
                    (surfaceY + 14.0) - playerEye.y,
                    spawnZ + 0.5 - playerEye.z
            ).normalize();

            // dot > 0.15 means inside front/peripheral vision (cos(80°) ≈ 0.17). Only allow behind or blind spots!
            if (lookVec.dot(toSpawn) > 0.15) {
                continue;
            }

            BlockPos groundPos = new BlockPos(spawnX, surfaceY - 1, spawnZ);
            BlockState groundState = level.getBlockState(groundPos);

            if (groundState.isAir() || !groundState.getFluidState().isEmpty()) {
                continue;
            }

            BlockPos airPos = groundPos.above();
            BlockState airState = level.getBlockState(airPos);
            if (!airState.getFluidState().isEmpty() || airState.isSolid()) {
                continue;
            }

            int brightness = level.getRawBrightness(airPos, 0);
            if (!isNight && brightness < 4) {
                continue;
            }

            boolean isStructure = isNearStructureOrVillage(level, airPos);
            Holder<Biome> biomeHolder = level.getBiome(airPos);
            EntityType<? extends BirdEntity> species = chooseSpeciesForBiome(biomeHolder, isNight, isStructure, random);
            if (species == null) {
                continue;
            }

            boolean inAir;
            int flockSize;
            if (species == ModEntities.MAGPIE) {
                flockSize = 20 + random.nextInt(21);
                inAir = true;
            } else if (species == ModEntities.PIGEON) {
                flockSize = isStructure ? (4 + random.nextInt(6)) : (3 + random.nextInt(4));
                inAir = false; // Pigeons naturally spawn on rooftops, chains, fences, or ground in feeding groups
            } else {
                flockSize = 3 + random.nextInt(5);
                inAir = random.nextBoolean();
            }

            // Fly forward into the world (often sweeping naturally past or above the player from behind)
            double swarmHeading = Math.toRadians(playerYaw + (random.nextDouble() - 0.5) * 80.0);
            double vx = -Math.sin(swarmHeading) * 0.54;
            double vz = Math.cos(swarmHeading) * 0.54;
            UUID flockUuid = UUID.randomUUID();
            MagpieEntity magpieLeader = null;

            for (int i = 0; i < flockSize; i++) {
                int spread = (species == ModEntities.MAGPIE) ? 5 : (isStructure ? 10 : 8);
                int ox = (i == 0) ? 0 : (random.nextInt(spread) - (spread / 2));
                int oz = (i == 0) ? 0 : (random.nextInt(spread) - (spread / 2));
                int sy = level.getHeight(Heightmap.Types.WORLD_SURFACE, spawnX + ox, spawnZ + oz);

                BirdEntity bird = species.create(level, EntitySpawnReason.NATURAL);
                if (bird != null) {
                    if (species == ModEntities.PIGEON) {
                        // Check if we can perch on a rooftop, fence, chain, wall, or leaves
                        BlockPos perch = findSpawnPerch(level, spawnX + ox, sy, spawnZ + oz, random);
                        boolean shouldPerch = perch != null && (isStructure ? (random.nextFloat() < 0.65F) : (random.nextFloat() < 0.40F));

                        if (shouldPerch) {
                            bird.setPos(perch.getX() + 0.5, perch.getY() + 1.0, perch.getZ() + 0.5);
                            bird.setYRot((float) (random.nextDouble() * 360.0));
                            level.addFreshEntity(bird);
                            bird.startPerching(perch, 400 + random.nextInt(600));
                            continue;
                        } else {
                            bird.setPos(spawnX + ox + 0.5, sy, spawnZ + oz + 0.5);
                            bird.setYRot((float) (random.nextDouble() * 360.0));
                            bird.setOnGround(true);
                            level.addFreshEntity(bird);
                            continue;
                        }
                    }

                    double spawnPosY = inAir ? (sy + 12.0 + (random.nextDouble() - 0.5) * 4.0) : sy;
                    bird.setPos(spawnX + ox + 0.5, spawnPosY, spawnZ + oz + 0.5);
                    bird.setYRot((float) (swarmHeading * (180.0 / Math.PI)));

                    if (inAir) {
                        bird.setDeltaMovement(
                                vx + (random.nextDouble() - 0.5) * 0.1,
                                0.05,
                                vz + (random.nextDouble() - 0.5) * 0.1
                        );
                    }

                    if (bird instanceof MagpieEntity magpie) {
                        magpie.setFlockId(flockUuid);
                        if (magpieLeader == null) {
                            magpieLeader = magpie;
                        } else {
                            magpie.setFlockLeader(magpieLeader);
                        }
                    }

                    level.addFreshEntity(bird);
                }
            }

            CozyBirds.LOGGER.info("Naturally spawned flock of {} {} near player {}", flockSize, species.getDescriptionId(), player.getName().getString());
            break;
        }
    }

    private static BlockPos findSpawnPerch(ServerLevel level, int centerX, int centerY, int centerZ, RandomSource random) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = random.nextInt(11) - 5;
            int dy = random.nextInt(12) - 4;
            int dz = random.nextInt(11) - 5;
            mutable.set(centerX + dx, centerY + dy, centerZ + dz);
            if (BirdEntity.isValidPerch(level, mutable)) {
                return mutable.immutable();
            }
        }
        return null;
    }

    private static boolean isNearStructureOrVillage(ServerLevel level, BlockPos pos) {
        try {
            if (level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE).isValid()) {
                return true;
            }
            if (!level.structureManager().getAllStructuresAt(pos).isEmpty()) {
                return true;
            }
        } catch (Exception ignored) {
        }

        // Check for man-made village / structure blocks
        for (int dx = -5; dx <= 5; dx += 5) {
            for (int dz = -5; dz <= 5; dz += 5) {
                BlockPos check = pos.offset(dx, 0, dz);
                BlockState bs = level.getBlockState(check);
                if (bs.is(BlockTags.PLANKS) || bs.is(BlockTags.FENCES) || bs.is(BlockTags.WALLS)
                        || bs.is(Blocks.COBBLESTONE) || bs.is(Blocks.STONE_BRICKS) || bs.is(Blocks.BELL)
                        || bs.getBlock() instanceof ChainBlock || bs.getBlock() instanceof LanternBlock) {
                    return true;
                }
            }
        }
        return false;
    }

    private static EntityType<? extends BirdEntity> chooseSpeciesForBiome(Holder<Biome> biomeHolder, boolean isNight, boolean isStructure, RandomSource random) {
        boolean pigeonOk = BirdSpawnConfig.PIGEON_SPAWN_ENABLED;
        boolean magpieOk = BirdSpawnConfig.MAGPIE_SPAWN_ENABLED;

        if (!pigeonOk && !magpieOk) return null;
        if (!pigeonOk) return ModEntities.MAGPIE;
        if (!magpieOk) return ModEntities.PIGEON;

        // In villages and structures: pigeons strongly preferred!
        if (isStructure) {
            return random.nextInt(10) < 8 ? ModEntities.PIGEON : ModEntities.MAGPIE;
        }

        // Both are enabled: distribute according to biome
        if (biomeHolder.is(BiomeTags.IS_FOREST) || biomeHolder.is(BiomeTags.IS_TAIGA)) {
            return random.nextInt(10) < 7 ? ModEntities.MAGPIE : ModEntities.PIGEON;
        }

        if (random.nextInt(10) < 6) {
            return ModEntities.PIGEON;
        }
        return ModEntities.MAGPIE;
    }
}
