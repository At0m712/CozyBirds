/* V2_DISABLED
package atom.cozybirds.entity.eagle;

import atom.cozybirds.entity.BirdEntity;
import atom.cozybirds.entity.ai.BirdFleeExplosionGoal;
import atom.cozybirds.entity.ai.PerchOnBlockGoal;
import atom.cozybirds.registry.ModEntities;
import atom.cozybirds.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Magnificent Golden Eagle (Aquila chrysaetos).
 * Ornithological realism:
 * - King of raptors: soaring on thermal updrafts above peaks and cliffs.
 * - Hunting stoop: high-speed dive attack on rabbits, chickens, and rodents.
 * - Territory screeching and mountain perch vigilance.
 * - Feathered tarsi down to powerful golden talons.
 * /
public class GoldenEagleEntity extends BirdEntity {
    private static final EntityDataAccessor<Boolean> DATA_SOARING =
            SynchedEntityData.defineId(GoldenEagleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DIVING =
            SynchedEntityData.defineId(GoldenEagleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SCREECHING =
            SynchedEntityData.defineId(GoldenEagleEntity.class, EntityDataSerializers.BOOLEAN);

    private float prevSoarProgress = 0.0F;
    private float soarProgress = 0.0F;
    private float prevDiveProgress = 0.0F;
    private float diveProgress = 0.0F;
    private float prevScreechProgress = 0.0F;
    private float screechProgress = 0.0F;

    private int screechTicks = 0;
    private int huntCooldown = 0;

    public GoldenEagleEntity(EntityType<? extends BirdEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createEagleAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.FLYING_SPEED, 0.58)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.ATTACK_DAMAGE, 8.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SOARING, false);
        builder.define(DATA_DIVING, false);
        builder.define(DATA_SCREECHING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BirdFleeExplosionGoal(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.15, this::isFood, false));
        this.goalSelector.addGoal(4, new EagleHuntingStoopGoal(this));
        this.goalSelector.addGoal(5, new EaglePerchOnPeaksGoal(this));
        this.goalSelector.addGoal(6, new EagleThermalSoaringGoal(this));
        this.goalSelector.addGoal(7, new PanicGoal(this, 1.35));

        // Hunting prey: Rabbits and Chickens
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Rabbit.class, 10, true, false, null));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Chicken.class, 15, true, false, null));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.RABBIT) || stack.is(Items.CHICKEN) || stack.is(Items.MUTTON)
                || stack.is(Items.BEEF) || stack.is(Items.SALMON);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return ModEntities.GOLDEN_EAGLE.create(serverLevel, EntitySpawnReason.BREEDING);
    }

    public boolean isSoaring() {
        return this.entityData.get(DATA_SOARING);
    }

    public void setSoaring(boolean soaring) {
        this.entityData.set(DATA_SOARING, soaring);
    }

    public boolean isDiving() {
        return this.entityData.get(DATA_DIVING);
    }

    public void setDiving(boolean diving) {
        this.entityData.set(DATA_DIVING, diving);
    }

    public boolean isScreeching() {
        return this.entityData.get(DATA_SCREECHING);
    }

    public void setScreeching(boolean screeching) {
        this.entityData.set(DATA_SCREECHING, screeching);
    }

    public float getSoarProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevSoarProgress, this.soarProgress);
    }

    public float getDiveProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevDiveProgress, this.diveProgress);
    }

    public float getScreechProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevScreechProgress, this.screechProgress);
    }

    @Override
    public float getFlapFrequency() {
        if (isSoaring()) return 0.08F;
        if (isDiving()) return 0.05F;
        return 0.18F;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.prevSoarProgress = this.soarProgress;
        this.prevDiveProgress = this.diveProgress;
        this.prevScreechProgress = this.screechProgress;

        float targetSoar = isSoaring() ? 1.0F : 0.0F;
        float targetDive = isDiving() ? 1.0F : 0.0F;
        float targetScreech = isScreeching() ? 1.0F : 0.0F;

        this.soarProgress = Mth.approach(this.soarProgress, targetSoar, 0.06F);
        this.diveProgress = Mth.approach(this.diveProgress, targetDive, 0.12F);
        this.screechProgress = Mth.approach(this.screechProgress, targetScreech, 0.15F);

        if (!this.level().isClientSide()) {
            if (this.huntCooldown > 0) {
                this.huntCooldown--;
            }

            if (this.screechTicks > 0) {
                this.screechTicks--;
                if (this.screechTicks == 0) {
                    setScreeching(false);
                }
            } else if (this.random.nextInt(600) == 0 && this.isFlying()) {
                triggerScreech();
            }
        }
    }

    public void triggerScreech() {
        this.screechTicks = 35;
        this.setScreeching(true);
        this.playSound(SoundEvents.PHANTOM_BITE, 1.4F, 1.35F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
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
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        int feathers = 1 + this.random.nextInt(3);
        for (int i = 0; i < feathers; i++) {
            this.spawnAtLocation(level, ModItems.GOLDEN_EAGLE_FEATHER);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Soaring", this.isSoaring());
        output.putBoolean("Diving", this.isDiving());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setSoaring(input.getBooleanOr("Soaring", false));
        this.setDiving(input.getBooleanOr("Diving", false));
    }

    // --- AI GOAL: THERMAL SOARING (CERCLAGE EN ALTITUDE) ---
    static class EagleThermalSoaringGoal extends Goal {
        private final GoldenEagleEntity eagle;
        private double centerX;
        private double centerZ;
        private double circleAngle = 0.0;
        private double targetY;
        private int duration = 0;

        EagleThermalSoaringGoal(GoldenEagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.eagle.getTarget() == null && !this.eagle.isPerched()
                    && this.eagle.random.nextInt(40) == 0;
        }

        @Override
        public void start() {
            this.centerX = this.eagle.getX();
            this.centerZ = this.eagle.getZ();
            this.circleAngle = this.eagle.random.nextDouble() * Math.PI * 2.0;
            int surfaceY = this.eagle.level().getHeight(Heightmap.Types.WORLD_SURFACE, (int) centerX, (int) centerZ);
            this.targetY = surfaceY + 22.0 + this.eagle.random.nextDouble() * 14.0;
            this.duration = 200 + this.eagle.random.nextInt(300);
            this.eagle.setOnGround(false);
            this.eagle.setPerched(false);
            this.eagle.setSoaring(true);
        }

        @Override
        public boolean canContinueToUse() {
            return this.duration > 0 && this.eagle.getTarget() == null && !this.eagle.isPerched();
        }

        @Override
        public void tick() {
            this.duration--;
            this.circleAngle += 0.035;
            double radius = 18.0;
            double targetX = this.centerX + Math.cos(this.circleAngle) * radius;
            double targetZ = this.centerZ + Math.sin(this.circleAngle) * radius;

            Vec3 moveVec = new Vec3(targetX - this.eagle.getX(), this.targetY - this.eagle.getY(), targetZ - this.eagle.getZ());
            double dist = moveVec.length();
            if (dist > 0.1) {
                Vec3 norm = moveVec.normalize();
                this.eagle.setDeltaMovement(norm.x * 0.42, norm.y * 0.18, norm.z * 0.42);
                float desiredYaw = (float) (Mth.atan2(norm.z, norm.x) * (180.0 / Math.PI)) - 90.0F;
                this.eagle.setYRot(Mth.rotLerp(0.1F, this.eagle.getYRot(), desiredYaw));
            }
        }

        @Override
        public void stop() {
            this.eagle.setSoaring(false);
        }
    }

    // --- AI GOAL: HUNTING STOOP (PIQUÉ DE CHASSE FULGURANT) ---
    static class EagleHuntingStoopGoal extends Goal {
        private final GoldenEagleEntity eagle;
        private int stoopTicks = 0;

        EagleHuntingStoopGoal(GoldenEagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.eagle.getTarget();
            return target != null && target.isAlive() && this.eagle.huntCooldown == 0;
        }

        @Override
        public void start() {
            this.stoopTicks = 0;
            this.eagle.setOnGround(false);
            this.eagle.setPerched(false);
            this.eagle.setSoaring(false);
            this.eagle.setDiving(true);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.eagle.getTarget();
            return target != null && target.isAlive() && this.stoopTicks < 140;
        }

        @Override
        public void tick() {
            LivingEntity target = this.eagle.getTarget();
            if (target == null) return;

            this.stoopTicks++;
            this.eagle.getLookControl().setLookAt(target, 30.0F, 30.0F);

            Vec3 toTarget = target.position().add(0, 0.4, 0).subtract(this.eagle.position());
            double dist = toTarget.length();

            if (dist > 2.2) {
                Vec3 norm = toTarget.normalize();
                double speed = (this.eagle.getY() > target.getY() + 3.0) ? 0.72 : 0.48;
                this.eagle.setDeltaMovement(norm.scale(speed));
            } else {
                this.eagle.doHurtTarget((ServerLevel) this.eagle.level(), target);
                this.eagle.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.2F, 0.8F);
                this.eagle.setDiving(false);
                this.eagle.huntCooldown = 200;
                this.eagle.setDeltaMovement(0, 0.55, 0);
            }
        }

        @Override
        public void stop() {
            this.eagle.setDiving(false);
        }
    }

    // --- AI GOAL: PERCH ON PEAKS (PERCHOIR SUR CRÊTES ET ROCHERS) ---
    static class EaglePerchOnPeaksGoal extends Goal {
        private final GoldenEagleEntity eagle;
        private BlockPos perchTarget = null;
        private int searchCooldown = 0;

        EaglePerchOnPeaksGoal(GoldenEagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.eagle.getTarget() != null || this.eagle.isPerched()) return false;
            if (this.searchCooldown-- > 0) return false;
            this.searchCooldown = 60 + this.eagle.random.nextInt(60);
            return this.eagle.random.nextInt(5) == 0;
        }

        @Override
        public void start() {
            BlockPos pos = this.eagle.blockPosition();
            BlockPos bestPos = null;
            int highestY = -100;

            for (int dx = -16; dx <= 16; dx += 4) {
                for (int dz = -16; dz <= 16; dz += 4) {
                    int y = this.eagle.level().getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX() + dx, pos.getZ() + dz);
                    BlockPos candidate = new BlockPos(pos.getX() + dx, y, pos.getZ() + dz);
                    BlockState below = this.eagle.level().getBlockState(candidate.below());

                    if (below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(BlockTags.LOGS) || below.is(BlockTags.LEAVES)) {
                        if (candidate.getY() > highestY) {
                            highestY = candidate.getY();
                            bestPos = candidate;
                        }
                    }
                }
            }

            if (bestPos != null) {
                this.perchTarget = bestPos;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.perchTarget != null && !this.eagle.isPerched();
        }

        @Override
        public void tick() {
            if (this.perchTarget == null) return;
            double distSqr = this.eagle.distanceToSqr(Vec3.atBottomCenterOf(this.perchTarget));
            if (distSqr < 2.0) {
                this.eagle.setPos(this.perchTarget.getX() + 0.5, this.perchTarget.getY(), this.perchTarget.getZ() + 0.5);
                this.eagle.setOnGround(true);
                this.eagle.setPerched(true);
                this.perchTarget = null;
            } else {
                Vec3 dir = Vec3.atBottomCenterOf(this.perchTarget).subtract(this.eagle.position()).normalize();
                this.eagle.setDeltaMovement(dir.scale(0.38));
            }
        }

        @Override
        public void stop() {
            this.perchTarget = null;
        }
    }
}

*/