package atom.cozybirds.entity.pigeon;

import atom.cozybirds.entity.BirdEntity;
import atom.cozybirds.entity.ai.BirdFleeExplosionGoal;
import atom.cozybirds.entity.ai.PerchOnBlockGoal;
import atom.cozybirds.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class PigeonEntity extends BirdEntity {
    public static final int ACTION_NONE = 0;
    public static final int ACTION_PECKING = 1;
    public static final int ACTION_COOING = 2;
    public static final int ACTION_PREENING = 3;

    private static final EntityDataAccessor<Integer> DATA_VARIANT_ID =
            SynchedEntityData.defineId(PigeonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTION =
            SynchedEntityData.defineId(PigeonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(PigeonEntity.class, EntityDataSerializers.BOOLEAN);

    private int actionTimer = 0;
    private int startleTicks = 0;
    private Vec3 startleThreatPos = null;
    private boolean relocating = false;

    public PigeonEntity(EntityType<? extends BirdEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new PigeonMoveControl(this);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    public static AttributeSupplier.Builder createPigeonAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.FLYING_SPEED, 0.75)
                .add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT_ID, PigeonVariant.CITY.getId());
        builder.define(DATA_ACTION, ACTION_NONE);
        builder.define(DATA_FLYING, false);
    }

    @Override
    public boolean isFlying() {
        if (this.isPerched()) {
            return false;
        }
        return this.entityData.get(DATA_FLYING);
    }

    public void setFlying(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
    }

    public boolean isRelocating() {
        return this.relocating;
    }

    public void setRelocating(boolean relocating) {
        this.relocating = relocating;
    }

    public boolean isBirdOnGround() {
        if (this.onGround() || this.verticalCollisionBelow) {
            return true;
        }
        Level level = this.level();
        if (level == null) return false;

        if (!level.noCollision(this, this.getBoundingBox().expandTowards(0.0, -0.35, 0.0))) {
            return true;
        }

        double entityBottom = this.getY();
        BlockPos feetPos = BlockPos.containing(this.getX(), entityBottom, this.getZ());
        BlockState feetState = level.getBlockState(feetPos);
        if (feetState.isSolid() || isValidPerch(level, feetPos)) {
            return true;
        }

        BlockPos belowPos = feetPos.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (belowState.isSolid() || isValidPerch(level, belowPos)) {
            double topY = belowPos.getY() + 1.0;
            if (entityBottom - topY <= 0.40 && entityBottom - topY >= -0.15) {
                return true;
            }
        }
        return false;
    }

    public PigeonVariant getVariant() {
        return PigeonVariant.byId(this.entityData.get(DATA_VARIANT_ID));
    }

    public void setVariant(PigeonVariant variant) {
        this.entityData.set(DATA_VARIANT_ID, variant.getId());
    }

    @Override
    public float getFlapFrequency() {
        return 1.73F; // 5.5 Hz (real-world pigeon: 5 to 6 Hz, rapid steady wing beats)
    }

    public int getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    public void setAction(int action) {
        this.entityData.set(DATA_ACTION, action);
    }

    public boolean isPecking() {
        return getAction() == ACTION_PECKING;
    }

    public boolean isCooing() {
        return getAction() == ACTION_COOING;
    }

    public boolean isPreening() {
        return getAction() == ACTION_PREENING;
    }

    public void triggerAction(int action, int duration) {
        this.actionTimer = duration;
        this.setAction(action);
    }

    public void triggerStartle(Vec3 threatPos) {
        if (this.startleTicks > 0) return;
        this.startleTicks = 80;
        this.startleThreatPos = threatPos;
        this.playSound(SoundEvents.PARROT_FLY, 0.40F, 1.30F + this.random.nextFloat() * 0.20F);

        // Alert nearby flock peers within 5.5 blocks so the group scatters together in a burst
        List<PigeonEntity> flock = this.level().getEntitiesOfClass(
                PigeonEntity.class,
                this.getBoundingBox().inflate(5.5),
                p -> p != this && p.isAlive() && p.onGround() && !p.isPerched() && p.startleTicks <= 0
        );
        for (PigeonEntity peer : flock) {
            peer.triggerStartleDirect(threatPos);
        }
    }

    public void triggerStartleDirect(Vec3 threatPos) {
        this.startleTicks = 80;
        this.startleThreatPos = threatPos;
        this.playSound(SoundEvents.PARROT_FLY, 0.35F, 1.25F + this.random.nextFloat() * 0.20F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason spawnReason, SpawnGroupData spawnGroupData) {
        PigeonVariant[] variants = PigeonVariant.values();
        this.setVariant(variants[this.random.nextInt(variants.length)]);
        return super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BirdFleeExplosionGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new PigeonStartleFleeGoal(this));
        this.goalSelector.addGoal(5, new PigeonRelocateFlightGoal(this));
        this.goalSelector.addGoal(6, new PigeonAirDescentGoal(this));
        this.goalSelector.addGoal(7, new PerchOnBlockGoal(this));
        this.goalSelector.addGoal(8, new PigeonPeckGoal(this));
        this.goalSelector.addGoal(9, new PigeonCooGoal(this));
        this.goalSelector.addGoal(10, new PigeonPreenGoal(this));
        this.goalSelector.addGoal(11, new PigeonGroundWanderGoal(this, 1.0));
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(13, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS)
                || stack.is(Items.MELON_SEEDS)
                || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.BREAD);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        PigeonEntity baby = ModEntities.PIGEON.create(level, EntitySpawnReason.BREEDING);
        if (baby != null && otherParent instanceof PigeonEntity otherPigeon) {
            baby.setVariant(this.random.nextBoolean() ? this.getVariant() : otherPigeon.getVariant());
        }
        return baby;
    }

    @Override
    public float getFlightProgress(float partialTick) {
        if (!this.isFlying() || this.isPerched() || this.isBirdOnGround()) {
            return 0.0F;
        }
        return super.getFlightProgress(partialTick);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            boolean grounded = this.isPerched() || this.isBirdOnGround();
            if (grounded) {
                if (this.isFlying()) {
                    this.setFlying(false);
                }
                if (this.isNoGravity()) {
                    this.setNoGravity(false);
                }
            } else if (!this.isFlying()) {
                if (this.fallDistance > 1.2F || (this.getDeltaMovement().y < -0.30 && !this.isBirdOnGround())) {
                    this.setFlying(true);
                }
            }

            if (this.actionTimer > 0) {
                this.actionTimer--;
                if (this.actionTimer == 0) {
                    this.setAction(ACTION_NONE);
                }
            }

            if (this.startleTicks > 0) {
                this.startleTicks--;
                if (this.startleTicks == 0) {
                    this.startleThreatPos = null;
                }
            }

            // Real-life pigeon startle: sprint near them or walk too close without crouching -> instant takeoff
            if (this.onGround() && !this.isPerched() && this.startleTicks <= 0) {
                if (this.tickCount % 4 == 0) {
                    Player player = this.level().getNearestPlayer(this, 6.0);
                    if (player != null && !player.isSpectator() && !player.isCreative()) {
                        double distSq = this.distanceToSqr(player);
                        boolean sprintScare = player.isSprinting() && distSq < 32.0;
                        boolean proximityScare = distSq < 5.0 && !player.isShiftKeyDown() && player.getDeltaMovement().horizontalDistanceSqr() > 0.005;
                        if (sprintScare || proximityScare) {
                            this.triggerStartle(player.position());
                        }
                    }
                }
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", this.getVariant().getId());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(PigeonVariant.byId(input.getIntOr("Variant", 0)));
    }

    public static class PigeonMoveControl extends net.minecraft.world.entity.ai.control.MoveControl {
        private final PigeonEntity pigeon;

        public PigeonMoveControl(PigeonEntity pigeon) {
            super(pigeon);
            this.pigeon = pigeon;
        }

        @Override
        public void tick() {
            if (this.pigeon.isFlying()) {
                // When in flight, physics and velocities are governed directly by purposeful flight goals
                return;
            }
            super.tick();
        }
    }

    // --- GOAL: STARTLE FLEE FLIGHT (Scatters when player sprints nearby) ---
    static class PigeonStartleFleeGoal extends Goal {
        private final PigeonEntity pigeon;
        private Vec3 targetLandingPos;
        private BlockPos targetPerchPos;
        private int flightTicks;
        private int stuckTicks;
        private Vec3 lastPos;

        public PigeonStartleFleeGoal(PigeonEntity pigeon) {
            this.pigeon = pigeon;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.pigeon.startleTicks > 0 && this.pigeon.startleThreatPos != null;
        }

        @Override
        public void start() {
            this.pigeon.setRelocating(true);
            this.flightTicks = 0;
            this.stuckTicks = 0;
            this.lastPos = this.pigeon.position();
            Vec3 myPos = this.pigeon.position();
            Vec3 threat = this.pigeon.startleThreatPos;
            Vec3 fleeDir = myPos.subtract(threat).normalize();
            if (fleeDir.lengthSqr() < 0.01) {
                fleeDir = new Vec3(this.pigeon.getRandom().nextDouble() - 0.5, 0, this.pigeon.getRandom().nextDouble() - 0.5).normalize();
            }

            this.targetPerchPos = null;
            this.targetLandingPos = findFleeLandingSpot(fleeDir);
            if (this.targetLandingPos == null) {
                this.targetLandingPos = myPos.add(fleeDir.scale(18.0));
            }

            // Rapid, explosive takeoff impulse
            this.pigeon.setFlying(true);
            this.pigeon.setOnGround(false);
            this.pigeon.setDeltaMovement(fleeDir.x * 0.55, 0.50, fleeDir.z * 0.55);
            this.pigeon.playSound(SoundEvents.PARROT_FLY, 0.45F, 1.35F);
        }

        private Vec3 findFleeLandingSpot(Vec3 fleeDir) {
            Level level = this.pigeon.level();
            RandomSource random = this.pigeon.getRandom();
            BlockPos current = this.pigeon.blockPosition();
            double baseAngle = Math.atan2(fleeDir.z, fleeDir.x);
            BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

            for (int i = 0; i < 12; i++) {
                double angle = baseAngle + (random.nextDouble() - 0.5) * 1.2; // Cone away from threat
                double dist = 14.0 + random.nextDouble() * 16.0;
                int x = (int) (current.getX() + Math.cos(angle) * dist);
                int z = (int) (current.getZ() + Math.sin(angle) * dist);

                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (topY <= level.getMinY() || topY >= level.getMaxY() - 2) continue;

                // Priority: perches (roof edges, chains, fences, leaves, slabs, walls)
                mut.set(x, topY - 1, z);
                if (BirdEntity.isValidPerch(level, mut)) {
                    this.targetPerchPos = mut.immutable();
                    return BirdEntity.getPerchPosition(level, this.targetPerchPos);
                }
                mut.set(x, topY - 2, z);
                if (BirdEntity.isValidPerch(level, mut)) {
                    this.targetPerchPos = mut.immutable();
                    return BirdEntity.getPerchPosition(level, this.targetPerchPos);
                }

                BlockState state = level.getBlockState(mut.set(x, topY - 1, z));
                if (state.isAir() || !state.getFluidState().isEmpty() || state.is(BlockTags.FIRE)) continue;

                BlockState airState = level.getBlockState(mut.set(x, topY, z));
                if (!airState.isAir() && airState.isSolid()) continue;

                this.targetPerchPos = null;
                return new Vec3(x + 0.5, topY, z + 0.5);
            }
            return null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetLandingPos != null && this.flightTicks < 130 && !this.pigeon.isPerched();
        }

        @Override
        public void tick() {
            this.flightTicks++;
            Vec3 myPos = this.pigeon.position();
            Vec3 toTarget = this.targetLandingPos.subtract(myPos);
            double horizDist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);

            // Stuck detection: if blocked against wall or under ceiling
            if (this.lastPos != null && myPos.distanceToSqr(this.lastPos) < 0.005) {
                this.stuckTicks++;
            } else {
                this.stuckTicks = 0;
            }
            this.lastPos = myPos;

            if (this.stuckTicks > 12 || (this.flightTicks > 20 && this.pigeon.horizontalCollision)) {
                land();
                return;
            }

            // Land if close to destination or touching ground after initial takeoff
            if ((horizDist < 1.2 && Math.abs(toTarget.y) < 1.6) || (this.flightTicks > 15 && this.pigeon.isBirdOnGround())) {
                land();
                return;
            }

            // High speed panic flight (0.85 blocks/tick = ~60 km/h)
            double speed = horizDist < 3.5 ? 0.45 : 0.85;

            // Only update yaw when horizDist > 0.8 to eliminate "la toupie" spinning
            if (horizDist > 0.8) {
                float targetYaw = (float)(Mth.atan2(toTarget.z, toTarget.x) * (180.0 / Math.PI)) - 90.0F;
                float smoothedYaw = Mth.rotLerp(0.30F, this.pigeon.getYRot(), targetYaw);
                this.pigeon.setYRot(smoothedYaw);
                this.pigeon.yBodyRot = smoothedYaw;
                this.pigeon.yHeadRot = smoothedYaw;
            }

            float currentYaw = this.pigeon.getYRot();
            double rad = Math.toRadians(currentYaw);
            double targetVy = (horizDist > 5.0) ? (toTarget.y > 1.0 ? 0.35 : 0.08) : Mth.clamp(toTarget.y * 0.28, -0.38, 0.22);
            this.pigeon.setDeltaMovement(-Math.sin(rad) * speed, targetVy, Math.cos(rad) * speed);
        }

        private void land() {
            this.pigeon.setDeltaMovement(Vec3.ZERO);
            this.pigeon.setFlying(false);
            this.pigeon.setNoGravity(false);
            this.pigeon.setOnGround(true);
            if (this.targetPerchPos != null && BirdEntity.isValidPerch(this.pigeon.level(), this.targetPerchPos)) {
                this.pigeon.startPerching(this.targetPerchPos, 300 + this.pigeon.getRandom().nextInt(400));
            }
            this.pigeon.startleTicks = 0;
            this.pigeon.startleThreatPos = null;
            this.targetLandingPos = null;
            this.targetPerchPos = null;
        }

        @Override
        public void stop() {
            this.pigeon.setFlying(false);
            this.pigeon.setNoGravity(false);
            this.targetLandingPos = null;
            this.targetPerchPos = null;
            this.pigeon.startleTicks = 0;
            this.pigeon.startleThreatPos = null;
        }
    }

    // --- GOAL: SPONTANEOUS RELOCATION FLIGHT (Fly swiftly to land on another spot) ---
    static class PigeonRelocateFlightGoal extends Goal {
        private final PigeonEntity pigeon;
        private Vec3 targetLandingPos;
        private BlockPos targetPerchPos;
        private int flightTicks;
        private int stuckTicks;
        private Vec3 lastPos;
        private int cooldown = 350 + (int)(Math.random() * 300);

        public PigeonRelocateFlightGoal(PigeonEntity pigeon) {
            this.pigeon = pigeon;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.pigeon.onGround() || this.pigeon.isPerched() || this.pigeon.getAction() != ACTION_NONE) {
                return false;
            }
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            if (this.pigeon.getRandom().nextInt(40) != 0) {
                return false;
            }

            this.targetLandingPos = findLandingSpot();
            return this.targetLandingPos != null;
        }

        private Vec3 findLandingSpot() {
            Level level = this.pigeon.level();
            RandomSource random = this.pigeon.getRandom();
            BlockPos current = this.pigeon.blockPosition();
            BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

            // Try 12 candidates at distance 12 to 28 blocks
            for (int i = 0; i < 12; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double dist = 12.0 + random.nextDouble() * 18.0;
                int x = (int) (current.getX() + Math.cos(angle) * dist);
                int z = (int) (current.getZ() + Math.sin(angle) * dist);

                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (topY <= level.getMinY() || topY >= level.getMaxY() - 2) continue;

                // Priority: check for perches (rooftops, roof edges, chains, fences, leaves, slabs)
                mut.set(x, topY - 1, z);
                if (BirdEntity.isValidPerch(level, mut)) {
                    this.targetPerchPos = mut.immutable();
                    return BirdEntity.getPerchPosition(level, this.targetPerchPos);
                }
                mut.set(x, topY - 2, z);
                if (BirdEntity.isValidPerch(level, mut)) {
                    this.targetPerchPos = mut.immutable();
                    return BirdEntity.getPerchPosition(level, this.targetPerchPos);
                }

                BlockState state = level.getBlockState(mut.set(x, topY - 1, z));
                if (state.isAir() || !state.getFluidState().isEmpty() || state.is(BlockTags.FIRE)) continue;

                BlockState airState = level.getBlockState(mut.set(x, topY, z));
                if (!airState.isAir() && airState.isSolid()) continue;

                this.targetPerchPos = null;
                return new Vec3(x + 0.5, topY, z + 0.5);
            }
            return null;
        }

        @Override
        public void start() {
            this.pigeon.setRelocating(true);
            this.flightTicks = 0;
            this.stuckTicks = 0;
            this.lastPos = this.pigeon.position();
            Vec3 myPos = this.pigeon.position();
            Vec3 toTarget = this.targetLandingPos.subtract(myPos);
            Vec3 horiz = new Vec3(toTarget.x, 0, toTarget.z).normalize();

            this.pigeon.setFlying(true);
            this.pigeon.setOnGround(false);
            this.pigeon.setDeltaMovement(horiz.x * 0.48, 0.46, horiz.z * 0.48);
            this.pigeon.playSound(SoundEvents.PARROT_FLY, 0.40F, 1.25F + this.pigeon.getRandom().nextFloat() * 0.15F);
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetLandingPos != null && this.flightTicks < 140 && !this.pigeon.isPerched();
        }

        @Override
        public void tick() {
            this.flightTicks++;
            Vec3 myPos = this.pigeon.position();
            Vec3 toTarget = this.targetLandingPos.subtract(myPos);
            double horizDist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);

            // Stuck detection: if barely moved or horizontalCollision
            if (this.lastPos != null && myPos.distanceToSqr(this.lastPos) < 0.005) {
                this.stuckTicks++;
            } else {
                this.stuckTicks = 0;
            }
            this.lastPos = myPos;

            // Abort if stuck under a ceiling or against a wall
            if (this.stuckTicks > 12 || (this.flightTicks > 25 && this.pigeon.horizontalCollision)) {
                land();
                return;
            }

            // Touchdown check
            if ((horizDist < 1.2 && Math.abs(toTarget.y) < 1.6) || (this.flightTicks > 20 && this.pigeon.isBirdOnGround())) {
                land();
                return;
            }

            // Real pigeon swift cruising speed: 0.72 blocks/tick (~52 km/h)
            double speed = horizDist < 3.5 ? 0.42 : 0.72;

            // PREVENT SPINNING LIKE A TOP ("la toupie"):
            // Only update yaw when horizontal distance to target is significant (> 0.8)
            // When close to target, hold current heading to smoothly glide in without atan2 flipping
            if (horizDist > 0.8) {
                float targetYaw = (float)(Mth.atan2(toTarget.z, toTarget.x) * (180.0 / Math.PI)) - 90.0F;
                float smoothedYaw = Mth.rotLerp(0.25F, this.pigeon.getYRot(), targetYaw);
                this.pigeon.setYRot(smoothedYaw);
                this.pigeon.yBodyRot = smoothedYaw;
                this.pigeon.yHeadRot = smoothedYaw;
            }

            float currentYaw = this.pigeon.getYRot();
            double rad = Math.toRadians(currentYaw);

            // Altitude profile
            double targetVy;
            if (horizDist > 6.0) {
                targetVy = (toTarget.y > 0.5) ? 0.32 : 0.08;
            } else {
                targetVy = Mth.clamp(toTarget.y * 0.28, -0.38, 0.22);
            }

            this.pigeon.setDeltaMovement(-Math.sin(rad) * speed, targetVy, Math.cos(rad) * speed);
        }

        private void land() {
            this.pigeon.setRelocating(false);
            this.pigeon.setDeltaMovement(Vec3.ZERO);
            this.pigeon.setFlying(false);
            this.pigeon.setNoGravity(false);
            this.pigeon.setOnGround(true);
            if (this.targetPerchPos != null && BirdEntity.isValidPerch(this.pigeon.level(), this.targetPerchPos)) {
                this.pigeon.startPerching(this.targetPerchPos, 300 + this.pigeon.getRandom().nextInt(400));
            }
            this.targetLandingPos = null;
            this.targetPerchPos = null;
            this.cooldown = 400 + this.pigeon.getRandom().nextInt(500); // 20 to 45 seconds of ground walking before flying
        }

        @Override
        public void stop() {
            this.pigeon.setRelocating(false);
            this.pigeon.setFlying(false);
            this.pigeon.setNoGravity(false);
            this.targetLandingPos = null;
            this.targetPerchPos = null;
            this.cooldown = 350 + this.pigeon.getRandom().nextInt(400);
        }
    }

    // --- GOAL: AIR DESCENT SAFEGUARD (Guarantees zero stationary hovering!) ---
    static class PigeonAirDescentGoal extends Goal {
        private final PigeonEntity pigeon;

        public PigeonAirDescentGoal(PigeonEntity pigeon) {
            this.pigeon = pigeon;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.pigeon.isRelocating() || this.pigeon.startleTicks > 0) {
                return false;
            }
            return !this.pigeon.isBirdOnGround() && !this.pigeon.isPerched();
        }

        @Override
        public boolean canContinueToUse() {
            if (this.pigeon.isRelocating() || this.pigeon.startleTicks > 0) {
                return false;
            }
            return !this.pigeon.isBirdOnGround() && !this.pigeon.isPerched();
        }

        @Override
        public void tick() {
            if (this.pigeon.isBirdOnGround() || (this.pigeon.horizontalCollision && this.pigeon.fallDistance < 0.6F)) {
                land();
                return;
            }

            Level level = this.pigeon.level();
            BlockPos currentPos = this.pigeon.blockPosition();

            // Fast O(1) chunk heightmap lookup for ground/roof beneath the pigeon
            int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, currentPos.getX(), currentPos.getZ());
            double targetFloorY = topY;
            if (currentPos.getY() < topY) {
                // If under a structure or cavern, scan down with a single mutable pos up to 16 blocks
                BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
                for (int dy = 0; dy <= 16; dy++) {
                    mut.set(currentPos.getX(), currentPos.getY() - dy, currentPos.getZ());
                    if (mut.getY() < level.getMinY()) break;
                    BlockState state = level.getBlockState(mut);
                    if (state.isSolid() || BirdEntity.isValidPerch(level, mut)) {
                        targetFloorY = mut.getY() + 1.0;
                        break;
                    }
                }
            }

            double distToFloor = this.pigeon.getY() - targetFloorY;
            if (distToFloor <= 0.45 || this.pigeon.verticalCollisionBelow) {
                land();
                return;
            }

            // Descend smoothly forward with steady glide speed: NEVER HOVER IN PLACE!
            float yaw = this.pigeon.getYRot();
            if (this.pigeon.horizontalCollision) {
                yaw += 60.0F;
                this.pigeon.setYRot(yaw);
            }

            double rad = Math.toRadians(yaw);
            double fwdSpeed = 0.38;
            double fwdX = -Math.sin(rad) * fwdSpeed;
            double fwdZ = Math.cos(rad) * fwdSpeed;
            double vy = -0.26;

            this.pigeon.setDeltaMovement(fwdX, vy, fwdZ);
        }

        private void land() {
            this.pigeon.setDeltaMovement(Vec3.ZERO);
            this.pigeon.setFlying(false);
            this.pigeon.setNoGravity(false);
            this.pigeon.setOnGround(true);
            Level level = this.pigeon.level();
            BlockPos currentPos = this.pigeon.blockPosition();
            if (BirdEntity.isValidPerch(level, currentPos)) {
                this.pigeon.startPerching(currentPos, 250 + this.pigeon.getRandom().nextInt(350));
            } else if (BirdEntity.isValidPerch(level, currentPos.below())) {
                this.pigeon.startPerching(currentPos.below(), 250 + this.pigeon.getRandom().nextInt(350));
            }
        }

        @Override
        public void stop() {
            this.pigeon.setDeltaMovement(Vec3.ZERO);
            this.pigeon.setFlying(false);
            this.pigeon.setNoGravity(false);
            this.pigeon.setOnGround(true);
        }
    }

    // --- GOAL: GROUND WANDERING (Stroll, waddle, and head bob on the ground) ---
    static class PigeonGroundWanderGoal extends WaterAvoidingRandomStrollGoal {
        private final PigeonEntity pigeon;

        public PigeonGroundWanderGoal(PigeonEntity pigeon, double speedModifier) {
            super(pigeon, speedModifier, 35);
            this.pigeon = pigeon;
        }

        @Override
        public boolean canUse() {
            if (this.pigeon.isFlying()) {
                return false;
            }
            return (this.pigeon.isBirdOnGround() || this.pigeon.onGround()) && !this.pigeon.isPerched()
                    && this.pigeon.getAction() == ACTION_NONE
                    && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return (this.pigeon.isBirdOnGround() || this.pigeon.onGround()) && !this.pigeon.isPerched()
                    && !this.pigeon.isFlying()
                    && this.pigeon.getAction() == ACTION_NONE
                    && super.canContinueToUse();
        }

        @Override
        protected Vec3 getPosition() {
            BlockPos current = this.pigeon.blockPosition();
            BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
            var level = this.pigeon.level();
            var random = this.pigeon.getRandom();

            for (int i = 0; i < 14; i++) {
                int dx = random.nextInt(19) - 9;
                int dy = random.nextInt(5) - 2;
                int dz = random.nextInt(19) - 9;
                testPos.set(current.getX() + dx, current.getY() + dy, current.getZ() + dz);
                var state = level.getBlockState(testPos);
                if (state.is(Blocks.DIRT_PATH) || state.is(Blocks.BELL) || state.is(Blocks.COBBLESTONE)) {
                    BlockPos above = testPos.above();
                    if (level.getBlockState(above).isAir()) {
                        return new Vec3(testPos.getX() + 0.5, testPos.getY() + 1.0, testPos.getZ() + 0.5);
                    }
                }
            }
            return super.getPosition();
        }
    }

    // --- GOAL: PECKING AT THE GROUND ---
    static class PigeonPeckGoal extends Goal {
        private final PigeonEntity pigeon;
        private int ticksRemaining;

        public PigeonPeckGoal(PigeonEntity pigeon) {
            this.pigeon = pigeon;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.pigeon.onGround() && !this.pigeon.isPerched()
                    && this.pigeon.getAction() == ACTION_NONE
                    && this.pigeon.getRandom().nextInt(120) == 0;
        }

        @Override
        public void start() {
            this.ticksRemaining = 35 + this.pigeon.getRandom().nextInt(25);
            this.pigeon.triggerAction(ACTION_PECKING, this.ticksRemaining);
            this.pigeon.getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticksRemaining > 0 && this.pigeon.onGround() && !this.pigeon.isPerched();
        }

        @Override
        public void tick() {
            this.ticksRemaining--;
            if (this.ticksRemaining % 8 == 0) {
                this.pigeon.playSound(SoundEvents.CHICKEN_STEP.value(), 0.12F, 1.5F);
            }
        }

        @Override
        public void stop() {
            if (this.pigeon.getAction() == ACTION_PECKING) {
                this.pigeon.setAction(ACTION_NONE);
            }
        }
    }

    // --- GOAL: COOING AND PUFFING CHEST ---
    static class PigeonCooGoal extends Goal {
        private final PigeonEntity pigeon;
        private int ticksRemaining;

        public PigeonCooGoal(PigeonEntity pigeon) {
            this.pigeon = pigeon;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !this.pigeon.isFlying()
                    && this.pigeon.getAction() == ACTION_NONE
                    && this.pigeon.getRandom().nextInt(160) == 0;
        }

        @Override
        public void start() {
            this.ticksRemaining = 45;
            this.pigeon.triggerAction(ACTION_COOING, this.ticksRemaining);
            this.pigeon.getNavigation().stop();
            this.pigeon.playSound(SoundEvents.PARROT_AMBIENT, 0.4F, 0.75F);
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticksRemaining > 0 && !this.pigeon.isFlying();
        }

        @Override
        public void tick() {
            this.ticksRemaining--;
        }

        @Override
        public void stop() {
            if (this.pigeon.getAction() == ACTION_COOING) {
                this.pigeon.setAction(ACTION_NONE);
            }
        }
    }

    // --- GOAL: PREENING FEATHERS ---
    static class PigeonPreenGoal extends Goal {
        private final PigeonEntity pigeon;
        private int ticksRemaining;

        public PigeonPreenGoal(PigeonEntity pigeon) {
            this.pigeon = pigeon;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return (this.pigeon.isPerched() || this.pigeon.onGround())
                    && this.pigeon.getAction() == ACTION_NONE
                    && this.pigeon.getRandom().nextInt(200) == 0;
        }

        @Override
        public void start() {
            this.ticksRemaining = 50;
            this.pigeon.triggerAction(ACTION_PREENING, this.ticksRemaining);
            this.pigeon.getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticksRemaining > 0 && !this.pigeon.isFlying();
        }

        @Override
        public void tick() {
            this.ticksRemaining--;
        }

        @Override
        public void stop() {
            if (this.pigeon.getAction() == ACTION_PREENING) {
                this.pigeon.setAction(ACTION_NONE);
            }
        }
    }
}
