/* V2_DISABLED
package atom.cozybirds.entity.flamingo;

import atom.cozybirds.entity.BirdEntity;
import atom.cozybirds.entity.ai.BirdFleeExplosionGoal;
import atom.cozybirds.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Greater Flamingo (Phoenicopterus roseus)
 * Ornithological realism:
 * - Filter feeder: inverts bill in shallow water to filter brine shrimp, algae, and insect larvae.
 * - Mud stamping: rapidly stamps webbed feet in shallow sediment to stir up hidden organisms.
 * - Thermoregulatory one-leg standing with head nestled in scapular feathers.
 * - Sentinel vigilance and synchronized collective alert / takeoff.
 * - Ritualized courtship: head-flagging and wing-salutes displaying jet-black flight feathers.
 * - Running takeoff: sprints across water before achieving lift-off.
 * - Cruising speed: 50-60 km/h (flying speed 0.52).
 * /
public class FlamingoEntity extends BirdEntity {
    private static final EntityDataAccessor<Boolean> DATA_STANDING_ONE_LEG =
            SynchedEntityData.defineId(FlamingoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LEFT_LEG_RAISED =
            SynchedEntityData.defineId(FlamingoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FILTER_FEEDING =
            SynchedEntityData.defineId(FlamingoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_MUD_STAMPING =
            SynchedEntityData.defineId(FlamingoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HEAD_FLAGGING =
            SynchedEntityData.defineId(FlamingoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SENTINEL_ALERT =
            SynchedEntityData.defineId(FlamingoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING_TAKEOFF =
            SynchedEntityData.defineId(FlamingoEntity.class, EntityDataSerializers.BOOLEAN);

    private FlamingoEntity flockLeader = null;
    private UUID flockId = null;
    private int leaderCheckCooldown = 0;
    private int filterFeedTicks = 0;
    private int alertTicks = 0;

    // Smooth animation transition progress
    private float prevOneLegProgress = 0.0F;
    private float oneLegProgress = 0.0F;
    private float prevFilterFeedProgress = 0.0F;
    private float filterFeedProgress = 0.0F;
    private float prevMudStampProgress = 0.0F;
    private float mudStampProgress = 0.0F;
    private float prevSentinelProgress = 0.0F;
    private float sentinelProgress = 0.0F;
    private float prevHeadFlagProgress = 0.0F;
    private float headFlagProgress = 0.0F;
    private float prevTakeoffProgress = 0.0F;
    private float takeoffProgress = 0.0F;

    public FlamingoEntity(EntityType<? extends BirdEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createFlamingoAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.FLYING_SPEED, 0.52)
                .add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STANDING_ONE_LEG, false);
        builder.define(DATA_LEFT_LEG_RAISED, true);
        builder.define(DATA_FILTER_FEEDING, false);
        builder.define(DATA_MUD_STAMPING, false);
        builder.define(DATA_HEAD_FLAGGING, false);
        builder.define(DATA_SENTINEL_ALERT, false);
        builder.define(DATA_RUNNING_TAKEOFF, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BirdFleeExplosionGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.45));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.15, this::isFood, false));
        this.goalSelector.addGoal(4, new FlamingoSentinelAlertGoal(this));
        this.goalSelector.addGoal(5, new FlamingoHeadFlaggingGoal(this));
        this.goalSelector.addGoal(6, new FlamingoWadeAndFilterFeedGoal(this));
        this.goalSelector.addGoal(7, new FlamingoOneLegRestGoal(this));
        this.goalSelector.addGoal(8, new FlamingoFlockMarchGoal(this));
        this.goalSelector.addGoal(9, new FlamingoWaterRunningTakeoffGoal(this));
        this.goalSelector.addGoal(10, new FlamingoHighFlightGoal(this));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SEAGRASS)
                || stack.is(Items.KELP)
                || stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.COD)
                || stack.is(Items.SALMON)
                || stack.is(Items.TROPICAL_FISH);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        FlamingoEntity baby = ModEntities.FLAMINGO.create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setFlockId(this.getFlockId());
        }
        return baby;
    }

    public boolean isStandingOnOneLeg() {
        return this.entityData.get(DATA_STANDING_ONE_LEG);
    }

    public void setStandingOnOneLeg(boolean standing) {
        this.entityData.set(DATA_STANDING_ONE_LEG, standing);
    }

    public boolean isLeftLegRaised() {
        return this.entityData.get(DATA_LEFT_LEG_RAISED);
    }

    public void setLeftLegRaised(boolean left) {
        this.entityData.set(DATA_LEFT_LEG_RAISED, left);
    }

    public boolean isFilterFeeding() {
        return this.entityData.get(DATA_FILTER_FEEDING);
    }

    public void setFilterFeeding(boolean feeding) {
        this.entityData.set(DATA_FILTER_FEEDING, feeding);
    }

    public boolean isMudStamping() {
        return this.entityData.get(DATA_MUD_STAMPING);
    }

    public void setMudStamping(boolean stamping) {
        this.entityData.set(DATA_MUD_STAMPING, stamping);
    }

    public boolean isHeadFlagging() {
        return this.entityData.get(DATA_HEAD_FLAGGING);
    }

    public void setHeadFlagging(boolean flagging) {
        this.entityData.set(DATA_HEAD_FLAGGING, flagging);
    }

    public boolean isSentinelAlert() {
        return this.entityData.get(DATA_SENTINEL_ALERT);
    }

    public void setSentinelAlert(boolean alert) {
        this.entityData.set(DATA_SENTINEL_ALERT, alert);
    }

    public boolean isRunningTakeoff() {
        return this.entityData.get(DATA_RUNNING_TAKEOFF);
    }

    public void setRunningTakeoff(boolean running) {
        this.entityData.set(DATA_RUNNING_TAKEOFF, running);
    }

    public float getOneLegProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevOneLegProgress, this.oneLegProgress);
    }

    public float getFilterFeedProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevFilterFeedProgress, this.filterFeedProgress);
    }

    public float getMudStampProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevMudStampProgress, this.mudStampProgress);
    }

    public float getSentinelProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevSentinelProgress, this.sentinelProgress);
    }

    public float getHeadFlagProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevHeadFlagProgress, this.headFlagProgress);
    }

    public float getTakeoffProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevTakeoffProgress, this.takeoffProgress);
    }

    public UUID getFlockId() {
        if (this.flockId == null) {
            this.flockId = UUID.randomUUID();
        }
        return this.flockId;
    }

    public void setFlockId(UUID flockId) {
        this.flockId = flockId;
    }

    public FlamingoEntity getFlockLeader() {
        return this.flockLeader;
    }

    public void setFlockLeader(FlamingoEntity leader) {
        this.flockLeader = leader;
        if (leader != null) {
            this.flockId = leader.getFlockId();
        }
    }

    public boolean hasFlockLeader() {
        return this.flockLeader != null && this.flockLeader.isAlive() && !this.flockLeader.isRemoved() && this.flockLeader != this;
    }

    @Override
    public float getFlapFrequency() {
        if (this.isRunningTakeoff()) {
            return 1.45F; // Rapid frantic flurry during running takeoff
        }
        // Greater Flamingo steady cruise wingbeat (~3.2 Hz)
        return 1.0F;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.prevOneLegProgress = this.oneLegProgress;
        this.oneLegProgress = Mth.clamp(this.oneLegProgress + (this.isStandingOnOneLeg() ? 0.08F : -0.08F), 0.0F, 1.0F);

        this.prevFilterFeedProgress = this.filterFeedProgress;
        this.filterFeedProgress = Mth.clamp(this.filterFeedProgress + (this.isFilterFeeding() ? 0.07F : -0.07F), 0.0F, 1.0F);

        this.prevMudStampProgress = this.mudStampProgress;
        this.mudStampProgress = Mth.clamp(this.mudStampProgress + (this.isMudStamping() ? 0.10F : -0.10F), 0.0F, 1.0F);

        this.prevSentinelProgress = this.sentinelProgress;
        this.sentinelProgress = Mth.clamp(this.sentinelProgress + (this.isSentinelAlert() ? 0.12F : -0.12F), 0.0F, 1.0F);

        this.prevHeadFlagProgress = this.headFlagProgress;
        this.headFlagProgress = Mth.clamp(this.headFlagProgress + (this.isHeadFlagging() ? 0.10F : -0.10F), 0.0F, 1.0F);

        this.prevTakeoffProgress = this.takeoffProgress;
        this.takeoffProgress = Mth.clamp(this.takeoffProgress + (this.isRunningTakeoff() ? 0.10F : -0.10F), 0.0F, 1.0F);

        if (this.level().isClientSide()) {
            if (this.isMudStamping()) {
                // Foot stamping in water/mud kicks up bubbles and splash
                this.level().addParticle(ParticleTypes.SPLASH,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getY() + 0.05,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.3,
                        (this.random.nextDouble() - 0.5) * 0.06, 0.12, (this.random.nextDouble() - 0.5) * 0.06);
            }
            if (this.isFilterFeeding()) {
                // Water ripples and splash micro-particles at beak position
                Vec3 look = this.getLookAngle();
                double bx = this.getX() + look.x * 0.65;
                double bz = this.getZ() + look.z * 0.65;
                double by = this.getY() + 0.1;
                this.level().addParticle(ParticleTypes.BUBBLE, bx, by, bz,
                        (this.random.nextDouble() - 0.5) * 0.04, 0.05, (this.random.nextDouble() - 0.5) * 0.04);
                if (this.random.nextInt(3) == 0) {
                    this.level().addParticle(ParticleTypes.SPLASH, bx, by, bz,
                            (this.random.nextDouble() - 0.5) * 0.08, 0.1, (this.random.nextDouble() - 0.5) * 0.08);
                }
            }
            if (this.isRunningTakeoff()) {
                // Heavy water splashing while running on water
                this.level().addParticle(ParticleTypes.SPLASH,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                        this.getY() + 0.1,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                        (this.random.nextDouble() - 0.5) * 0.25, 0.3, (this.random.nextDouble() - 0.5) * 0.25);
            }
        } else {
            // Periodically check and maintain flock leadership
            if (this.leaderCheckCooldown-- <= 0) {
                this.leaderCheckCooldown = 60 + this.random.nextInt(40);
                updateFlockMembership();
            }

            // Sentinel vigilance timer
            if (this.alertTicks > 0) {
                this.alertTicks--;
                if (this.alertTicks <= 0) {
                    this.setSentinelAlert(false);
                }
            }

            // Filter feeding satisfaction and small heal
            if (this.isFilterFeeding()) {
                this.filterFeedTicks++;
                if (this.filterFeedTicks % 30 == 0) {
                    this.playSound(SoundEvents.GENERIC_SWIM, 0.35F, 1.4F);
                }
                if (this.filterFeedTicks > 100) {
                    this.filterFeedTicks = 0;
                    this.heal(1.0F);
                    this.playSound(SoundEvents.PARROT_EAT, 0.45F, 1.05F);
                }
            } else {
                this.filterFeedTicks = 0;
            }
        }
    }

    private void updateFlockMembership() {
        if (this.hasFlockLeader()) {
            if (this.distanceToSqr(this.flockLeader) > 45.0 * 45.0 || !this.flockLeader.isAlive() || this.flockLeader.isRemoved()) {
                this.flockLeader = null;
            } else {
                return;
            }
        }

        UUID myFlock = this.getFlockId();
        List<FlamingoEntity> nearby = this.level().getEntitiesOfClass(
                FlamingoEntity.class,
                this.getBoundingBox().inflate(35.0),
                f -> f != this && f.isAlive() && !f.isRemoved() && myFlock.equals(f.getFlockId())
        );

        if (!nearby.isEmpty()) {
            FlamingoEntity oldest = null;
            for (FlamingoEntity f : nearby) {
                if (f.getId() < this.getId()) {
                    if (oldest == null || f.getId() < oldest.getId()) {
                        oldest = f;
                    }
                }
            }
            this.flockLeader = oldest;
        } else {
            this.flockLeader = null;
        }
    }

    public void triggerColonyAlert() {
        this.setSentinelAlert(true);
        this.alertTicks = 80;
        this.playSound(SoundEvents.PARROT_AMBIENT, 0.8F, 0.72F); // Guttural alarm honk

        // Warn nearby flamingos of the same flock to alert in unison!
        List<FlamingoEntity> nearby = this.level().getEntitiesOfClass(
                FlamingoEntity.class,
                this.getBoundingBox().inflate(14.0),
                f -> f != this && f.isAlive()
        );
        for (FlamingoEntity f : nearby) {
            if (!f.isSentinelAlert()) {
                f.setSentinelAlert(true);
                f.alertTicks = 70;
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        this.triggerColonyAlert();
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        // Deep guttural croak / honk of flamingos
        return 0.70F + (this.random.nextFloat() - 0.5F) * 0.08F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("StandingOneLeg", this.isStandingOnOneLeg());
        output.putBoolean("LeftLegRaised", this.isLeftLegRaised());
        if (this.flockId != null) {
            output.putString("FlockId", this.flockId.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setStandingOnOneLeg(input.getBooleanOr("StandingOneLeg", false));
        this.setLeftLegRaised(input.getBooleanOr("LeftLegRaised", true));
        if (input.getString("FlockId").isPresent()) {
            try {
                this.flockId = UUID.fromString(input.getStringOr("FlockId", ""));
            } catch (Exception ignored) {}
        }
    }

    /**
     * Sentinel Vigilance: raises neck bolt upright to scan for danger and warn colony
     * /
    static class FlamingoSentinelAlertGoal extends Goal {
        private final FlamingoEntity flamingo;

        public FlamingoSentinelAlertGoal(FlamingoEntity flamingo) {
            this.flamingo = flamingo;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.flamingo.isFlying()) return false;
            Player player = this.flamingo.level().getNearestPlayer(this.flamingo, 6.0);
            return player != null && !player.isSpectator() && (player.isSprinting() || player.distanceToSqr(this.flamingo) < 16.0);
        }

        @Override
        public void start() {
            this.flamingo.triggerColonyAlert();
        }

        @Override
        public boolean canContinueToUse() {
            return this.flamingo.alertTicks > 0;
        }
    }

    /**
     * Wading in shallow water, mud-stamping to stir up prey, then filter-feeding
     * /
    static class FlamingoWadeAndFilterFeedGoal extends Goal {
        private final FlamingoEntity flamingo;
        private BlockPos waterTarget = null;
        private int feedDuration = 0;
        private int stampTicks = 0;

        public FlamingoWadeAndFilterFeedGoal(FlamingoEntity flamingo) {
            this.flamingo = flamingo;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.flamingo.isFlying() || this.flamingo.isStandingOnOneLeg() || this.flamingo.isHeadFlagging() || this.flamingo.isSentinelAlert()) {
                return false;
            }
            if (this.flamingo.getRandom().nextInt(25) != 0) {
                return false;
            }

            if (isShallowWater(this.flamingo.blockPosition())) {
                this.waterTarget = this.flamingo.blockPosition();
                return true;
            }

            BlockPos current = this.flamingo.blockPosition();
            for (int i = 0; i < 16; i++) {
                int ox = this.flamingo.getRandom().nextInt(17) - 8;
                int oz = this.flamingo.getRandom().nextInt(17) - 8;
                BlockPos target = current.offset(ox, 0, oz);
                if (isShallowWater(target)) {
                    this.waterTarget = target;
                    return true;
                }
                BlockPos targetDown = target.below();
                if (isShallowWater(targetDown)) {
                    this.waterTarget = targetDown;
                    return true;
                }
            }
            return false;
        }

        private boolean isShallowWater(BlockPos pos) {
            BlockState state = this.flamingo.level().getBlockState(pos);
            BlockState above = this.flamingo.level().getBlockState(pos.above());
            BlockState below = this.flamingo.level().getBlockState(pos.below());
            return state.getFluidState().is(FluidTags.WATER) && above.isAir() && !below.isAir() && below.getFluidState().isEmpty();
        }

        @Override
        public boolean canContinueToUse() {
            return this.feedDuration > 0 && !this.flamingo.isFlying() && !this.flamingo.isSentinelAlert();
        }

        @Override
        public void start() {
            if (this.waterTarget != null) {
                this.flamingo.getNavigation().moveTo(this.waterTarget.getX() + 0.5, this.waterTarget.getY(), this.waterTarget.getZ() + 0.5, 1.0);
            }
            this.feedDuration = 140 + this.flamingo.getRandom().nextInt(120);
            this.stampTicks = 35 + this.flamingo.getRandom().nextInt(25);
        }

        @Override
        public void tick() {
            this.feedDuration--;
            if (this.waterTarget != null && this.flamingo.distanceToSqr(this.waterTarget.getX() + 0.5, this.waterTarget.getY(), this.waterTarget.getZ() + 0.5) < 2.0) {
                this.flamingo.getNavigation().stop();

                // Phase 1: Rapid mud-stamping to stir up benthic brine shrimp/crustaceans
                if (this.stampTicks > 0) {
                    this.stampTicks--;
                    this.flamingo.setMudStamping(true);
                    this.flamingo.setFilterFeeding(false);
                    if (this.stampTicks % 8 == 0) {
                        this.flamingo.playSound(SoundEvents.GENERIC_SWIM, 0.25F, 1.6F);
                    }
                } else {
                    // Phase 2: Dip inverted bill into water and sift
                    this.flamingo.setMudStamping(false);
                    this.flamingo.setFilterFeeding(true);
                    this.flamingo.setYRot(this.flamingo.getYRot() + (this.flamingo.tickCount % 20 == 0 ? 4.0F : 0.0F));
                }
            } else {
                this.flamingo.setFilterFeeding(false);
                this.flamingo.setMudStamping(false);
            }
        }

        @Override
        public void stop() {
            this.flamingo.setFilterFeeding(false);
            this.flamingo.setMudStamping(false);
            this.waterTarget = null;
            this.feedDuration = 0;
            this.stampTicks = 0;
        }
    }

    /**
     * Thermoregulatory One-Leg Resting Goal:
     * Minimizes heat loss by tucking one leg into breast/belly feathers
     * /
    static class FlamingoOneLegRestGoal extends Goal {
        private final FlamingoEntity flamingo;
        private int restDuration = 0;

        public FlamingoOneLegRestGoal(FlamingoEntity flamingo) {
            this.flamingo = flamingo;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.flamingo.isFlying() || this.flamingo.isFilterFeeding() || this.flamingo.isHeadFlagging()
                    || this.flamingo.isRunningTakeoff() || this.flamingo.isSentinelAlert()) {
                return false;
            }
            if (this.flamingo.getNavigation().isDone() && this.flamingo.getRandom().nextInt(35) == 0) {
                return this.flamingo.onGround() || this.flamingo.isInWater();
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.restDuration > 0 && !this.flamingo.isFlying() && !this.flamingo.isSentinelAlert();
        }

        @Override
        public void start() {
            this.restDuration = 220 + this.flamingo.getRandom().nextInt(240);
            this.flamingo.setStandingOnOneLeg(true);
            this.flamingo.setLeftLegRaised(this.flamingo.getRandom().nextBoolean());
            this.flamingo.getNavigation().stop();
            this.flamingo.setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void tick() {
            this.restDuration--;
            this.flamingo.setDeltaMovement(Vec3.ZERO);
            Player nearestPlayer = this.flamingo.level().getNearestPlayer(this.flamingo, 3.5);
            if (nearestPlayer != null && !nearestPlayer.isSpectator()) {
                this.stop();
            }
        }

        @Override
        public void stop() {
            this.flamingo.setStandingOnOneLeg(false);
            this.restDuration = 0;
        }
    }

    /**
     * Courtship Head-Flagging Display:
     * Rigid neck, rapid rhythmic left-right head turning, flashing black and coral flight feathers
     * /
    static class FlamingoHeadFlaggingGoal extends Goal {
        private final FlamingoEntity flamingo;
        private int displayDuration = 0;

        public FlamingoHeadFlaggingGoal(FlamingoEntity flamingo) {
            this.flamingo = flamingo;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.flamingo.isFlying() || this.flamingo.isFilterFeeding() || this.flamingo.isStandingOnOneLeg() || this.flamingo.isSentinelAlert()) {
                return false;
            }
            if (this.flamingo.getRandom().nextInt(50) == 0) {
                List<FlamingoEntity> nearby = this.flamingo.level().getEntitiesOfClass(
                        FlamingoEntity.class,
                        this.flamingo.getBoundingBox().inflate(10.0),
                        f -> f != this.flamingo && f.isAlive()
                );
                return !nearby.isEmpty();
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.displayDuration > 0 && !this.flamingo.isFlying() && !this.flamingo.isSentinelAlert();
        }

        @Override
        public void start() {
            this.displayDuration = 70 + this.flamingo.getRandom().nextInt(50);
            this.flamingo.setHeadFlagging(true);
            this.flamingo.getNavigation().stop();
            this.flamingo.playSound(this.flamingo.getAmbientSound(), 0.65F, 0.72F);
        }

        @Override
        public void tick() {
            this.displayDuration--;
            this.flamingo.setYRot(this.flamingo.getYRot() + 1.2F);
        }

        @Override
        public void stop() {
            this.flamingo.setHeadFlagging(false);
            this.displayDuration = 0;
        }
    }

    /**
     * Synchronized colony march through wetlands and beaches
     * /
    static class FlamingoFlockMarchGoal extends Goal {
        private final FlamingoEntity flamingo;
        private int marchTicks = 0;

        public FlamingoFlockMarchGoal(FlamingoEntity flamingo) {
            this.flamingo = flamingo;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.flamingo.isFlying() || this.flamingo.isStandingOnOneLeg() || this.flamingo.isFilterFeeding() || this.flamingo.isSentinelAlert()) {
                return false;
            }
            if (this.flamingo.hasFlockLeader()) {
                FlamingoEntity leader = this.flamingo.getFlockLeader();
                return leader != null && !leader.isFlying() && !leader.isStandingOnOneLeg() && this.flamingo.distanceToSqr(leader) > 4.0;
            }
            if (this.flamingo.getRandom().nextInt(40) == 0) {
                return this.flamingo.getNavigation().isDone();
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.marchTicks > 0 && !this.flamingo.isFlying() && !this.flamingo.isStandingOnOneLeg() && !this.flamingo.isSentinelAlert();
        }

        @Override
        public void start() {
            this.marchTicks = 110 + this.flamingo.getRandom().nextInt(80);
            if (this.flamingo.hasFlockLeader()) {
                FlamingoEntity leader = this.flamingo.getFlockLeader();
                this.flamingo.getNavigation().moveTo(leader, 1.05);
            } else {
                double angle = this.flamingo.getRandom().nextDouble() * Math.PI * 2.0;
                double dist = 14.0 + this.flamingo.getRandom().nextDouble() * 12.0;
                double tx = this.flamingo.getX() + Math.cos(angle) * dist;
                double tz = this.flamingo.getZ() + Math.sin(angle) * dist;
                int ty = this.flamingo.level().getHeight(Heightmap.Types.WORLD_SURFACE, (int)tx, (int)tz);
                this.flamingo.getNavigation().moveTo(tx, ty, tz, 1.0);
            }
        }

        @Override
        public void tick() {
            this.marchTicks--;
            if (this.flamingo.hasFlockLeader()) {
                FlamingoEntity leader = this.flamingo.getFlockLeader();
                if (leader != null && this.flamingo.tickCount % 20 == 0) {
                    this.flamingo.getNavigation().moveTo(leader, 1.05);
                }
            }
        }

        @Override
        public void stop() {
            this.marchTicks = 0;
        }
    }

    /**
     * Running takeoff: 4-6 strides across water surface before lift-off
     * /
    static class FlamingoWaterRunningTakeoffGoal extends Goal {
        private final FlamingoEntity flamingo;
        private int runTicks = 0;
        private Vec3 takeoffDir = null;

        public FlamingoWaterRunningTakeoffGoal(FlamingoEntity flamingo) {
            this.flamingo = flamingo;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.flamingo.isFlying() || this.flamingo.isStandingOnOneLeg()) {
                return false;
            }
            // Flight impulse when in water, or immediately when startled in sentinel alert!
            if (this.flamingo.isSentinelAlert() && this.flamingo.getRandom().nextInt(15) == 0) {
                return true;
            }
            return this.flamingo.getRandom().nextInt(90) == 0 && this.flamingo.isInWater();
        }

        @Override
        public boolean canContinueToUse() {
            return this.runTicks < 45 && !this.flamingo.isFlying();
        }

        @Override
        public void start() {
            this.runTicks = 0;
            this.flamingo.setRunningTakeoff(true);
            double angle = this.flamingo.getRandom().nextDouble() * Math.PI * 2.0;
            this.takeoffDir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).normalize();
            this.flamingo.playSound(SoundEvents.GENERIC_SWIM, 0.75F, 1.2F);
        }

        @Override
        public void tick() {
            this.runTicks++;
            if (this.takeoffDir != null) {
                this.flamingo.setDeltaMovement(this.takeoffDir.scale(0.40).add(0.0, 0.08, 0.0));
                this.flamingo.setYRot((float)(Mth.atan2(this.takeoffDir.z, this.takeoffDir.x) * (180.0 / Math.PI)) - 90.0F);

                if (this.runTicks > 24) {
                    this.flamingo.setOnGround(false);
                    this.flamingo.setDeltaMovement(this.takeoffDir.scale(0.50).add(0.0, 0.48, 0.0));
                    this.flamingo.playSound(SoundEvents.PARROT_FLY, 0.6F, 0.85F);
                    this.stop();
                }
            }
        }

        @Override
        public void stop() {
            this.flamingo.setRunningTakeoff(false);
            this.runTicks = 0;
            this.takeoffDir = null;
        }
    }

    /**
     * Stately high flight over wetlands (altitude 14-26 blocks) in arrow-straight posture
     * /
    static class FlamingoHighFlightGoal extends Goal {
        private final FlamingoEntity flamingo;
        private Vec3 flightTarget = null;
        private int flightTicks = 0;

        public FlamingoHighFlightGoal(FlamingoEntity flamingo) {
            this.flamingo = flamingo;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.flamingo.isPerched() || this.flamingo.isStandingOnOneLeg() || this.flamingo.isRunningTakeoff()) {
                return false;
            }
            return this.flamingo.isFlying() && (this.flightTarget == null || this.flightTicks <= 0 || this.flamingo.getNavigation().isDone());
        }

        @Override
        public boolean canContinueToUse() {
            return this.flightTicks > 0 && !this.flamingo.onGround();
        }

        @Override
        public void start() {
            this.flightTicks = 160 + this.flamingo.getRandom().nextInt(100);
            var random = this.flamingo.getRandom();
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 28.0 + random.nextDouble() * 28.0;
            double tx = this.flamingo.getX() + Math.cos(angle) * dist;
            double tz = this.flamingo.getZ() + Math.sin(angle) * dist;
            int surfaceY = this.flamingo.level().getHeight(Heightmap.Types.WORLD_SURFACE, (int)tx, (int)tz);
            double ty = Math.max(this.flamingo.getY(), surfaceY + 14.0 + random.nextDouble() * 10.0);
            this.flightTarget = new Vec3(tx, ty, tz);
            this.flamingo.getNavigation().moveTo(tx, ty, tz, 1.30);
        }

        @Override
        public void tick() {
            this.flightTicks--;
        }

        @Override
        public void stop() {
            this.flightTicks = 0;
            this.flightTarget = null;
        }
    }
}

*/