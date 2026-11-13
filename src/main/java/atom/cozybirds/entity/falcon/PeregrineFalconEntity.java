/* V2_DISABLED
package atom.cozybirds.entity.falcon;

import atom.cozybirds.entity.BirdEntity;
import atom.cozybirds.entity.ai.BirdFleeExplosionGoal;
import atom.cozybirds.entity.ai.PerchOnBlockGoal;
import atom.cozybirds.registry.ModEntities;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class PeregrineFalconEntity extends BirdEntity {
    private static final EntityDataAccessor<Boolean> DATA_DIVING =
            SynchedEntityData.defineId(PeregrineFalconEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_AIMING =
            SynchedEntityData.defineId(PeregrineFalconEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_EATING =
            SynchedEntityData.defineId(PeregrineFalconEntity.class, EntityDataSerializers.BOOLEAN);

    private FalconHighSoaringGoal soaringGoal;
    private int eatingTicks = 0;

    public PeregrineFalconEntity(EntityType<? extends BirdEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createFalconAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DIVING, false);
        builder.define(DATA_AIMING, false);
        builder.define(DATA_EATING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BirdFleeExplosionGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.2, stack -> isFood(stack), false));
        this.goalSelector.addGoal(4, new PerchOnBlockGoal(this));
        this.goalSelector.addGoal(5, new FalconHuntFishGoal(this));
        this.soaringGoal = new FalconHighSoaringGoal(this);
        this.goalSelector.addGoal(6, this.soaringGoal);
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SALMON)
                || stack.is(Items.COD)
                || stack.is(Items.TROPICAL_FISH);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.FALCON.create(level, EntitySpawnReason.BREEDING);
    }

    public boolean isDiving() {
        return this.entityData.get(DATA_DIVING);
    }

    public void setDiving(boolean diving) {
        this.entityData.set(DATA_DIVING, diving);
    }

    public boolean isAiming() {
        return this.entityData.get(DATA_AIMING);
    }

    public void setAiming(boolean aiming) {
        this.entityData.set(DATA_AIMING, aiming);
    }

    @Override
    public float getFlapFrequency() {
        if (this.isDiving()) {
            return 0.0F; // 0 Hz in stoop dive (wings locked back in teardrop shape)
        }
        if (this.isAiming()) {
            return 0.85F; // Gentle hovering flutter while aiming at prey
        }
        return 1.32F; // 4.2 Hz (real-world falcon cruise: 4 to 4.5 Hz, deep powerful wing strokes)
    }

    public boolean isEating() {
        return this.entityData.get(DATA_EATING);
    }

    public void setEating(boolean eating) {
        this.entityData.set(DATA_EATING, eating);
    }

    public void startEating() {
        this.setEating(true);
        this.eatingTicks = 65;
    }

    public void resetSoarTarget() {
        if (this.soaringGoal != null) {
            this.soaringGoal.resetTarget();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            // Falcons never float on water! If in water, immediately rocket upward back to the sky
            if (this.isInWater()) {
                Vec3 current = this.getDeltaMovement();
                this.setDeltaMovement(new Vec3(current.x * 0.5, 0.85, current.z * 0.5));
                this.setDiving(false);
                this.playSound(SoundEvents.GENERIC_SPLASH, 0.6F, 1.3F);
            }

            // Eating process with sounds and food particles
            if (this.isEating()) {
                if (this.eatingTicks > 0) {
                    this.eatingTicks--;
                    if (this.eatingTicks % 7 == 0) {
                        this.playSound(SoundEvents.GENERIC_EAT.value(), 0.65F, 1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);
                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                    new ItemParticleOption(ParticleTypes.ITEM, Items.COD),
                                    this.getX(),
                                    this.getY() + 0.25,
                                    this.getZ(),
                                    4,
                                    0.15, 0.15, 0.15,
                                    0.05
                            );
                        }
                    }
                } else {
                    this.setEating(false);
                    this.heal(4.0F);
                    this.playSound(SoundEvents.PARROT_EAT, 0.6F, 1.2F);
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

    /**
     * Soar high in the sky (Y = terrain + 14 to 26 blocks) circling and scanning below
     * /
    static class FalconHighSoaringGoal extends Goal {
        private final PeregrineFalconEntity falcon;
        private Vec3 targetSoarPos = null;
        private int soarTicks = 0;

        public FalconHighSoaringGoal(PeregrineFalconEntity falcon) {
            this.falcon = falcon;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        public void resetTarget() {
            this.targetSoarPos = null;
            this.soarTicks = 0;
        }

        @Override
        public boolean canUse() {
            if (this.falcon.isPerched() || this.falcon.isDiving()) return false;
            return this.falcon.getNavigation().isDone() || this.targetSoarPos == null || this.soarTicks <= 0;
        }

        @Override
        public void start() {
            this.soarTicks = 140 + this.falcon.getRandom().nextInt(100);
            chooseHighAltitudeTarget();
        }

        private void chooseHighAltitudeTarget() {
            var random = this.falcon.getRandom();
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 18.0 + random.nextDouble() * 24.0;
            double targetX = this.falcon.getX() + Math.cos(angle) * dist;
            double targetZ = this.falcon.getZ() + Math.sin(angle) * dist;

            // Find surface height below target
            int surfaceY = this.falcon.level().getHeight(Heightmap.Types.WORLD_SURFACE, (int) targetX, (int) targetZ);
            // Fly HIGH: 14 to 26 blocks above surface
            double targetY = Math.max(this.falcon.getY(), surfaceY + 14.0 + random.nextDouble() * 10.0);

            this.targetSoarPos = new Vec3(targetX, targetY, targetZ);
            this.falcon.getNavigation().moveTo(targetX, targetY, targetZ, 1.15);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.falcon.isDiving() && !this.falcon.isPerched() && this.soarTicks > 0 && !this.falcon.getNavigation().isDone();
        }

        @Override
        public void tick() {
            this.soarTicks--;
        }
    }

    /**
     * Predatory 4-stage hunt:
     * 1. STAGE_POSITIONING: Spots a fish, navigates agilely to 10-15 blocks directly above the fish.
     * 2. STAGE_AIMING: Stabilizes / hovers 10-15 blocks above prey, locks head & body pitch/yaw downwards
     *    directly targeting the fish below for ~26 ticks (predatory visual lock-on).
     * 3. STAGE_DIVING: High speed stoop dive (speed 1.55) straight into water on target fish, strikes it,
     *    discards fish entity (NO FLOATING ITEMS!), splashes water, and starts eating animation.
     * 4. STAGE_CLIMBING: Pulls up out of water carrying forward momentum away into the sky to a new altitude.
     * /
    static class FalconHuntFishGoal extends Goal {
        private static final int STAGE_POSITIONING = 0;
        private static final int STAGE_AIMING = 1;
        private static final int STAGE_DIVING = 2;
        private static final int STAGE_CLIMBING = 3;

        private final PeregrineFalconEntity falcon;
        private AbstractFish targetFish;
        private int huntStage = STAGE_POSITIONING;
        private int stageTicks = 0;
        private int huntCooldown = 80;
        private double hoverX;
        private double hoverY;
        private double hoverZ;
        private double pullUpAltitude = 0.0;
        private Vec3 diveVelocity = Vec3.ZERO;

        public FalconHuntFishGoal(PeregrineFalconEntity falcon) {
            this.falcon = falcon;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.falcon.isPerched() || this.falcon.isBaby()) {
                return false;
            }

            if (this.huntCooldown > 0) {
                this.huntCooldown--;
                return false;
            }

            // Periodic hunt opportunity
            if (this.falcon.getRandom().nextInt(15) != 0) {
                return false;
            }

            // Must be soaring high in the air to initiate a hunt (at least 7 blocks above ground/water)
            int groundY = this.falcon.level().getHeight(Heightmap.Types.WORLD_SURFACE, this.falcon.getBlockX(), this.falcon.getBlockZ());
            if (this.falcon.getY() < groundY + 6.0) {
                return false;
            }

            // Scan for fish in water below
            List<AbstractFish> fishList = this.falcon.level().getEntitiesOfClass(
                    AbstractFish.class,
                    this.falcon.getBoundingBox().inflate(34.0, 28.0, 34.0),
                    f -> f.isAlive() && f.isInWater() && f.getY() < this.falcon.getY() - 6.0
            );

            if (fishList.isEmpty()) {
                this.huntCooldown = 120 + this.falcon.getRandom().nextInt(60);
                return false;
            }

            this.targetFish = fishList.get(this.falcon.getRandom().nextInt(fishList.size()));
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.huntStage == STAGE_CLIMBING) {
                return this.falcon.getY() < this.pullUpAltitude && this.stageTicks < 140;
            }
            return this.targetFish != null && this.targetFish.isAlive() && !this.targetFish.isRemoved() && this.stageTicks < 120;
        }

        @Override
        public void start() {
            this.huntStage = STAGE_POSITIONING;
            this.stageTicks = 0;
            this.falcon.setAiming(false);
            this.falcon.setDiving(false);

            // Calculate hover point: 11-14 blocks directly above fish
            updateHoverPos();
            this.falcon.getNavigation().moveTo(this.hoverX, this.hoverY, this.hoverZ, 1.30);
        }

        private void updateHoverPos() {
            if (this.targetFish != null) {
                this.hoverX = this.targetFish.getX();
                this.hoverZ = this.targetFish.getZ();
                this.hoverY = this.targetFish.getY() + 11.0 + (this.falcon.getRandom().nextDouble() * 3.0);
            }
        }

        @Override
        public void tick() {
            this.stageTicks++;

            if (this.targetFish == null || !this.targetFish.isAlive() || this.targetFish.isRemoved()) {
                if (this.huntStage != STAGE_CLIMBING) {
                    this.stop();
                    return;
                }
            }

            switch (this.huntStage) {
                case STAGE_POSITIONING -> {
                    // Update hover point with fish drift
                    updateHoverPos();
                    this.falcon.getNavigation().moveTo(this.hoverX, this.hoverY, this.hoverZ, 1.35);
                    this.falcon.getLookControl().setLookAt(this.targetFish, 30.0F, 30.0F);

                    double horizDistSq = this.falcon.distanceToSqr(this.hoverX, this.falcon.getY(), this.hoverZ);
                    double vertDiff = Math.abs(this.falcon.getY() - this.hoverY);

                    // Reached 10-15 blocks above fish: switch to visual lock-on aiming stage!
                    if ((horizDistSq < 6.25 && vertDiff < 3.5) || this.stageTicks > 65) {
                        this.huntStage = STAGE_AIMING;
                        this.stageTicks = 0;
                        this.falcon.getNavigation().stop();
                        this.falcon.setAiming(true);
                        this.falcon.playSound(SoundEvents.PARROT_FLY, 0.4F, 1.4F);
                    }
                }
                case STAGE_AIMING -> {
                    // Hover in place directly above prey
                    this.falcon.setDeltaMovement(this.falcon.getDeltaMovement().scale(0.25).add(0.0, 0.02, 0.0));

                    // Aim head and body precisely down at target fish
                    double dx = this.targetFish.getX() - this.falcon.getX();
                    double dy = this.targetFish.getY() - this.falcon.getY();
                    double dz = this.targetFish.getZ() - this.falcon.getZ();
                    double horizDist = Math.sqrt(dx * dx + dz * dz);

                    float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                    float pitch = (float) -(Mth.atan2(dy, Math.max(horizDist, 0.001)) * (180.0 / Math.PI));

                    this.falcon.setYRot(yaw);
                    this.falcon.yBodyRot = yaw;
                    this.falcon.yHeadRot = yaw;
                    this.falcon.setXRot(pitch);
                    this.falcon.getLookControl().setLookAt(this.targetFish, 80.0F, 80.0F);

                    // After ~26 ticks (~1.3s) of intense aiming lock-on: STOOP DIVE!
                    if (this.stageTicks >= 26) {
                        this.huntStage = STAGE_DIVING;
                        this.stageTicks = 0;
                        this.falcon.setAiming(false);
                        this.falcon.setDiving(true);
                        this.falcon.playSound(SoundEvents.PARROT_FLY, 0.6F, 0.8F);
                    }
                }
                case STAGE_DIVING -> {
                    // Terminal velocity stoop dive towards fish!
                    Vec3 toFish = this.targetFish.position().subtract(this.falcon.position());
                    this.diveVelocity = toFish.normalize().scale(1.55);
                    this.falcon.setDeltaMovement(this.diveVelocity);
                    this.falcon.getLookControl().setLookAt(this.targetFish, 40.0F, 40.0F);
                    this.falcon.setXRot(75.0F);

                    double distSq = this.falcon.distanceToSqr(this.targetFish);
                    if (distSq < 3.8 || this.falcon.isInWater()) {
                        // Strike! Discard the fish entity immediately so NO ITEM FLOATS on the water!
                        this.targetFish.discard();

                        if (this.falcon.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                    ParticleTypes.SPLASH,
                                    this.falcon.getX(),
                                    this.falcon.getY() + 0.2,
                                    this.falcon.getZ(),
                                    30,
                                    0.35, 0.35, 0.35,
                                    0.25
                            );
                        }

                        this.falcon.playSound(SoundEvents.GENERIC_SPLASH, 0.8F, 1.2F);
                        this.falcon.playSound(SoundEvents.PARROT_FLY, 0.6F, 0.8F);

                        // Start eating the caught fish with crunching animation and cod particles!
                        this.falcon.startEating();

                        // Compute forward escape vector from dive velocity
                        Vec3 forwardDir = new Vec3(this.diveVelocity.x, 0, this.diveVelocity.z).normalize();
                        if (forwardDir.lengthSqr() < 0.001) {
                            forwardDir = this.falcon.getLookAngle();
                        }

                        // Swoop upwards and carry forward momentum away to a new altitude
                        this.huntStage = STAGE_CLIMBING;
                        this.stageTicks = 0;
                        this.falcon.setDiving(false);
                        this.falcon.setDeltaMovement(new Vec3(forwardDir.x * 0.95, 0.85, forwardDir.z * 0.95));
                        this.pullUpAltitude = this.falcon.getY() + 18.0;
                        this.targetFish = null;
                        this.huntCooldown = 280 + this.falcon.getRandom().nextInt(160);

                        // Move navigation forward in the escape trajectory
                        double escapeX = this.falcon.getX() + forwardDir.x * 35.0;
                        double escapeZ = this.falcon.getZ() + forwardDir.z * 35.0;
                        double escapeY = this.pullUpAltitude;
                        this.falcon.getNavigation().moveTo(escapeX, escapeY, escapeZ, 1.35);
                    }
                }
                case STAGE_CLIMBING -> {
                    // Powerful climb carrying forward momentum away from the dive!
                    Vec3 current = this.falcon.getDeltaMovement();
                    this.falcon.setDeltaMovement(new Vec3(current.x * 0.98, 0.75, current.z * 0.98));
                    this.falcon.setXRot(-25.0F); // nose up
                    if (this.falcon.getY() >= this.pullUpAltitude || this.stageTicks > 120) {
                        this.stop();
                    }
                }
            }
        }

        @Override
        public void stop() {
            this.falcon.setAiming(false);
            this.falcon.setDiving(false);
            this.targetFish = null;
            this.huntStage = STAGE_POSITIONING;
            if (this.huntCooldown <= 0) {
                this.huntCooldown = 150;
            }
            // Clear navigation so it does NOT return to pre-dive position!
            this.falcon.getNavigation().stop();
            // Reset soaring target so soaring starts fresh from where the falcon climbed to!
            this.falcon.resetSoarTarget();
        }
    }
}

*/