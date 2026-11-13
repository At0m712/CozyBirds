package atom.cozybirds.registry;

import atom.cozybirds.CozyBirds;
import atom.cozybirds.entity.BirdEntity;
import atom.cozybirds.entity.magpie.MagpieEntity;
import atom.cozybirds.entity.pigeon.PigeonEntity;
import atom.cozybirds.entity.spawner.BirdSpawnConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;

public class ModEntities {
    public static final EntityType<PigeonEntity> PIGEON = registerEntity("pigeon",
            EntityType.Builder.of(PigeonEntity::new, MobCategory.AMBIENT).sized(0.4F, 0.5F).clientTrackingRange(10).updateInterval(2));

    public static final EntityType<MagpieEntity> MAGPIE = registerEntity("magpie",
            EntityType.Builder.of(MagpieEntity::new, MobCategory.AMBIENT).sized(0.35F, 0.35F).clientTrackingRange(10).updateInterval(2));

    /* V2 birds: kept in codebase, commented for V1 perfection
    public static final EntityType<PeregrineFalconEntity> FALCON = registerEntity("falcon",
            EntityType.Builder.of(PeregrineFalconEntity::new, MobCategory.AMBIENT).sized(0.5F, 0.6F).clientTrackingRange(10).updateInterval(1));

    public static final EntityType<FlamingoEntity> FLAMINGO = registerEntity("flamingo",
            EntityType.Builder.of(FlamingoEntity::new, MobCategory.AMBIENT).sized(0.6F, 1.45F).clientTrackingRange(10).updateInterval(1));

    public static final EntityType<GoldenEagleEntity> GOLDEN_EAGLE = registerEntity("golden_eagle",
            EntityType.Builder.of(GoldenEagleEntity::new, MobCategory.AMBIENT).sized(0.7F, 0.85F).clientTrackingRange(10).updateInterval(1));
    */

    private static <T extends Animal> EntityType<T> registerEntity(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(CozyBirds.MOD_ID, name));
        EntityType<T> entityType = builder.build(key);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, entityType);
    }

    public static boolean checkBirdSpawnRules(
            EntityType<? extends Animal> type,
            net.minecraft.world.level.LevelAccessor level,
            net.minecraft.world.entity.EntitySpawnReason reason,
            net.minecraft.core.BlockPos pos,
            net.minecraft.util.RandomSource random) {
        if (type == PIGEON && !BirdSpawnConfig.PIGEON_SPAWN_ENABLED) return false;
        if (type == MAGPIE && !BirdSpawnConfig.MAGPIE_SPAWN_ENABLED) return false;

        net.minecraft.core.BlockPos belowPos = pos.below();
        net.minecraft.world.level.block.state.BlockState belowState = level.getBlockState(belowPos);
        boolean validGround = !belowState.isAir() && belowState.getFluidState().isEmpty();
        return validGround && level.getRawBrightness(pos, 0) > 4;
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(PIGEON, PigeonEntity.createPigeonAttributes());
        FabricDefaultAttributeRegistry.register(MAGPIE, MagpieEntity.createMagpieAttributes());

        SpawnPlacements.register(PIGEON, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, ModEntities::checkBirdSpawnRules);
        SpawnPlacements.register(MAGPIE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, ModEntities::checkBirdSpawnRules);

        // Vanilla Chunk Generation & Spawning
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                MobCategory.CREATURE,
                PIGEON,
                140, 2, 6
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                MobCategory.CREATURE,
                MAGPIE,
                160, 20, 40
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                MobCategory.AMBIENT,
                PIGEON,
                140, 2, 6
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                MobCategory.AMBIENT,
                MAGPIE,
                160, 20, 40
        );
    }
}
