package atom.cozybirds.entity.magpie;

import atom.cozybirds.entity.BirdEntity;
import atom.cozybirds.entity.ai.BirdFleeExplosionGoal;
import atom.cozybirds.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class MagpieEntity extends BirdEntity {
    private MagpieEntity flockLeader = null;
    private UUID flockId = null;
    private int leaderCheckCooldown = 0;
    private int groundTicks = 0;

    public MagpieEntity(EntityType<? extends BirdEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new MagpieMoveControl(this);
        // Desynchronize initial wingbeat phase completely across all birds upon spawn
        this.flap = this.random.nextFloat() * (float)Math.PI * 2.0F;
        this.oFlap = this.flap;
    }

    public static AttributeSupplier.Builder createMagpieAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.FLYING_SPEED, 0.65)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public float getFlapFrequency() {
        int id = Math.abs(this.getId());
        // Distinct frequency per bird (1.75 to 2.35) so wing strokes drift organically
        return 1.75F + ((id * 31) % 15) * 0.04F;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BirdFleeExplosionGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.15, stack -> isFood(stack), false));
        this.goalSelector.addGoal(4, new MagpiePerchInTreeGoal(this));
        this.goalSelector.addGoal(5, new MagpieFlockFlightGoal(this));
        this.goalSelector.addGoal(6, new MagpieHighFlightGoal(this));
        this.goalSelector.addGoal(7, new MagpieLookAtPlayerGoal(this));
        this.goalSelector.addGoal(8, new MagpieRandomLookAroundGoal(this));
    }

    @Override
    public boolean isPushable() {
        return !this.isFlying();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS)
                || stack.is(Items.GOLD_NUGGET)
                || stack.is(Items.IRON_NUGGET)
                || stack.is(Items.SWEET_BERRIES);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        MagpieEntity baby = ModEntities.MAGPIE.create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setFlockId(this.getFlockId());
            if (this.hasFlockLeader()) {
                baby.setFlockLeader(this.getFlockLeader());
            } else {
                baby.setFlockLeader(this);
            }
        }
        return baby;
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

    public MagpieEntity getFlockLeader() {
        return this.flockLeader;
    }

    public void setFlockLeader(MagpieEntity leader) {
        this.flockLeader = leader;
        if (leader != null) {
            this.flockId = leader.getFlockId();
        }
    }

    public boolean hasFlockLeader() {
        return this.flockLeader != null && this.flockLeader.isAlive() && !this.flockLeader.isRemoved() && this.flockLeader != this;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            // Magpies hate staying on the ground! Fly 70% of the time, rest in trees
            if (this.onGround() && !this.isPerched()) {
                this.groundTicks++;
                if (this.groundTicks > 15 || (this.hasFlockLeader() && this.flockLeader.isFlying())) {
                    this.groundTicks = 0;
                    this.setOnGround(false);
                    this.setDeltaMovement(new Vec3(
                            (this.random.nextDouble() - 0.5) * 0.3,
                            0.45,
                            (this.random.nextDouble() - 0.5) * 0.3
                    ));
                    this.playSound(getFlapSound(), 0.15F, 1.0F);
                }
            } else {
                this.groundTicks = 0;
            }

            if (this.hasFlockLeader()) {
                if (leaderCheckCooldown-- <= 0) {
                    leaderCheckCooldown = 60 + (this.getId() % 30);
                    updateFlockMembership();
                }
            } else {
                if (leaderCheckCooldown-- <= 0) {
                    leaderCheckCooldown = 20 + (this.getId() % 15);
                    updateFlockMembership();
                }
            }

            // If leader takes flight, entire swarm launches into the sky in unison!
            if (this.hasFlockLeader() && this.flockLeader.isFlying() && !this.isFlying() && !this.isPerched()) {
                this.setOnGround(false);
                this.setDeltaMovement(new Vec3(
                        (this.random.nextDouble() - 0.5) * 0.2,
                        0.45,
                        (this.random.nextDouble() - 0.5) * 0.2
                ));
            }
        }
    }

    private void updateFlockMembership() {
        if (this.hasFlockLeader()) {
            if (this.distanceToSqr(this.flockLeader) > 65.0 * 65.0 || !this.flockLeader.isAlive() || this.flockLeader.isRemoved()) {
                this.flockLeader = null;
            } else {
                return;
            }
        }

        UUID myFlock = this.getFlockId();

        // STRICT FLOCK ISOLATION: Only interact with birds of the EXACT SAME flock!
        // This permanently prevents two swarms from ever merging when they cross paths!
        List<MagpieEntity> nearby = this.level().getEntitiesOfClass(
                MagpieEntity.class,
                this.getBoundingBox().inflate(55.0),
                m -> m != this && m.isAlive() && !m.isRemoved() && myFlock.equals(m.getFlockId())
        );

        if (!nearby.isEmpty()) {
            // Elect oldest / lowest-ID magpie belonging strictly to THIS flock
            MagpieEntity bestCandidate = null;
            for (MagpieEntity m : nearby) {
                if (m.getId() < this.getId()) {
                    if (bestCandidate == null || m.getId() < bestCandidate.getId()) {
                        bestCandidate = m;
                    }
                }
            }
            this.flockLeader = bestCandidate;
        } else {
            this.flockLeader = null;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.flockId != null) {
            output.putString("FlockId", this.flockId.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.getString("FlockId").isPresent()) {
            try {
                this.flockId = UUID.fromString(input.getStringOr("FlockId", ""));
            } catch (Exception ignored) {}
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
     * Organic murmuration flight for flock members:
     * Natural 3D undulating harmonics, neighbor separation, real-life magpie flight speed (38-48 km/h),
     * and strictly forward-oriented aerodynamic velocity (guaranteed zero backward flight!).
     */
    static class MagpieFlockFlightGoal extends Goal {
        private final MagpieEntity magpie;

        public MagpieFlockFlightGoal(MagpieEntity magpie) {
            this.magpie = magpie;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.magpie.hasFlockLeader() && !this.magpie.isPerched();
        }

        @Override
        public boolean canContinueToUse() {
            return this.magpie.hasFlockLeader() && !this.magpie.isPerched();
        }

        @Override
        public void tick() {
            MagpieEntity leader = this.magpie.getFlockLeader();
            if (leader == null || !leader.isAlive() || leader.isRemoved()) return;

            int id = Math.abs(this.magpie.getId());
            int slot = (id % 39) + 1; // 1 to 39

            // --- 1. NON-PERIODIC MULTI-OCTAVE HARMONICS & INDIVIDUAL BIRD VARIATIONS ---
            // Each bird has its own unique pseudo-random seed, rates, and phases
            long seed = ((long) id * 31237L) ^ 0x5DEECE66DL;
            double freq1 = 0.038 + ((seed % 19) * 0.0018); // 0.038 to 0.072 (unique tempo per bird)
            double freq2 = 0.021 + (((seed >> 4) % 15) * 0.0015);
            double freq3 = 0.011 + (((seed >> 8) % 11) * 0.0012);
            double phase1 = (seed % 100) * 0.0628;
            double phase2 = ((seed >> 6) % 100) * 0.0628;
            double phase3 = ((seed >> 12) % 100) * 0.0628;

            double t = this.magpie.tickCount;

            // Lateral crossing weave:
            // Superposition of golden-ratio incommensurate frequencies guarantees the weave NEVER repeats!
            double crossSweep = Math.sin(t * freq1 + phase1) * 0.65
                              + Math.sin(t * freq1 * 1.618 + phase2) * 0.40
                              + Math.cos(t * freq2 + phase3) * 0.35;

            // Vertical undulation (3D crossing over and under):
            double vertSweep = Math.sin(t * freq2 * 1.3 + phase2) * 0.65
                             + Math.cos(t * freq1 * 1.414 + phase1) * 0.45
                             + Math.sin(t * 0.015) * 0.40; // atmospheric wave

            // Flock breathing cycle: expands into a wide veil (~7 blocks) and tightens into a core (~3 blocks)
            double breath = 1.0 + Math.sin(t * 0.012 + phase3) * 0.35;
            double ribbonWidth = (2.2 + (slot % 5) * 0.80) * breath;

            // Distinct base 3D distribution grid per bird: guarantees flock members NEVER collapse into a single point/column!
            double baseLaneX = (((slot * 7) % 9) - 4.0) * 0.95; // Staggered lateral lanes (-3.8 to +3.8 blocks)
            double baseLaneY = (((slot * 3) % 7) - 3.0) * 0.45; // Staggered vertical tiers (-1.35 to +1.35 blocks)
            double baseLaneZ = -(2.5 + (slot * 0.95) + Math.cos(t * freq3 + phase1) * 1.2); // Aerodynamic depth trailing

            double localRight = baseLaneX + crossSweep * ribbonWidth;
            double localUp = baseLaneY + vertSweep * 1.5;
            double localBack = baseLaneZ;

            // Orient offset along leader's horizontal facing direction
            float leaderYaw = leader.getYRot();
            double leaderRad = Math.toRadians(leaderYaw);
            double fwdX = -Math.sin(leaderRad);
            double fwdZ = Math.cos(leaderRad);
            double rightX = fwdZ;
            double rightZ = -fwdX;

            Vec3 leaderPos = leader.position();
            double targetX = leaderPos.x + localRight * rightX + localBack * fwdX;
            double targetY = leaderPos.y + localUp;
            double targetZ = leaderPos.z + localRight * rightZ + localBack * fwdZ;

            Vec3 myPos = this.magpie.position();
            Vec3 toTarget = new Vec3(targetX - myPos.x, targetY - myPos.y, targetZ - myPos.z);
            double distSq = toTarget.lengthSqr();
            double dist = Math.sqrt(distSq);

            // --- 2. LOCAL NEIGHBOR SEPARATION (Aerodynamic personal bubble, prevents clipping and clumping) ---
            Vec3 separation = Vec3.ZERO;
            List<MagpieEntity> neighbors = this.magpie.level().getEntitiesOfClass(
                    MagpieEntity.class,
                    this.magpie.getBoundingBox().inflate(2.6),
                    m -> m != this.magpie && m.isAlive() && m.hasFlockLeader()
            );
            for (MagpieEntity other : neighbors) {
                Vec3 diff = myPos.subtract(other.position());
                double dSq = diff.lengthSqr();
                if (dSq > 0.001 && dSq < 2.6 * 2.6) {
                    double d = Math.sqrt(dSq);
                    double push = (2.6 - d) / 2.6;
                    separation = separation.add(diff.normalize().scale(push * push * 0.28));
                }
            }

            // --- 3. REAL-LIFE AIRSPEED REGULATION WITH ORGANIC MICRO-SURGES ---
            double speed = 0.54;
            double microSurge = Math.sin(t * freq2 * 2.0 + phase1) * 0.05; // gentle natural speed bursts
            if (dist > 6.0) {
                speed = 0.66; // fast sprint to catch up (48 km/h)
            } else if (dist > 3.0) {
                speed = 0.59;
            } else if (dist < 1.2) {
                speed = 0.49; // gentle glide (35 km/h)
            }
            speed = Mth.clamp(speed + microSurge, 0.46, 0.68);

            // --- 4. STEERING: STRICTLY FORWARD FACING (ZERO BACKWARD FLIGHT) ---
            Vec3 desiredDir;
            if (dist > 0.4) {
                Vec3 pullDir = toTarget.normalize();
                desiredDir = new Vec3(fwdX, 0, fwdZ).scale(0.52).add(pullDir.scale(0.48));
            } else {
                desiredDir = new Vec3(fwdX, 0, fwdZ);
            }

            float targetYaw = (float)(Mth.atan2(desiredDir.z, desiredDir.x) * (180.0 / Math.PI)) - 90.0F;
            float currentYaw = this.magpie.getYRot();
            float turnResponsiveness = 0.16F + (float)(Math.sin(t * 0.03 + phase2) * 0.04);
            float smoothedYaw = Mth.rotLerp(turnResponsiveness, currentYaw, targetYaw);

            this.magpie.setYRot(smoothedYaw);
            this.magpie.yBodyRot = smoothedYaw;
            this.magpie.yHeadRot = smoothedYaw;

            // Velocity is strictly forward along smoothedYaw + separation
            double rad = Math.toRadians(smoothedYaw);
            double finalFwdX = -Math.sin(rad);
            double finalFwdZ = Math.cos(rad);

            double finalVy = Mth.clamp(toTarget.y * 0.12, -0.25, 0.25) + separation.y;
            double finalVx = finalFwdX * speed + separation.x;
            double finalVz = finalFwdZ * speed + separation.z;

            this.magpie.setDeltaMovement(new Vec3(finalVx, finalVy, finalVz));
            this.magpie.setOnGround(false);

            // Pitch smoothly matches vertical velocity
            float targetPitch = (float)(-Math.atan2(finalVy, speed) * (180.0 / Math.PI));
            this.magpie.setXRot(Mth.rotLerp(0.20F, this.magpie.getXRot(), targetPitch));
        }
    }

    /**
     * Swarm Leader high-flight goal: leads the murmuration in sweeping panoramic curves,
     * non-periodic multi-harmonic meandering S-curves, and organic vertical swoops
     * at real-life magpie cruising speed (40 km/h = 0.54 blocks/tick).
     */
    static class MagpieHighFlightGoal extends Goal {
        private final MagpieEntity magpie;
        private Vec3 targetFlightPos = null;
        private int waypointTicks = 0;
        private double baseFlightAngle = 0.0;
        private double wanderSeed = 0.0;

        public MagpieHighFlightGoal(MagpieEntity magpie) {
            this.magpie = magpie;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.magpie.isPerched() || this.magpie.hasFlockLeader()) return false;
            return this.targetFlightPos == null || this.waypointTicks <= 0 || this.magpie.position().distanceToSqr(this.targetFlightPos) < 144.0;
        }

        @Override
        public void start() {
            this.baseFlightAngle = Math.toRadians(this.magpie.getYRot());
            this.wanderSeed = this.magpie.getRandom().nextDouble() * 1000.0;
            chooseHighFlightTarget();
        }

        private void chooseHighFlightTarget() {
            var random = this.magpie.getRandom();

            // Broad panoramic exploration angle changes:
            // Randomized turns: 30% sharp turns (±60 to ±90°), 70% sweeping bends (±25 to ±45°)
            double turnDelta;
            if (random.nextInt(3) == 0) {
                turnDelta = (random.nextBoolean() ? 1.0 : -1.0) * (0.9 + random.nextDouble() * 0.6); // sharp wheeling sweep
            } else {
                turnDelta = (random.nextDouble() - 0.5) * 1.2; // organic meander
            }
            this.baseFlightAngle += turnDelta;

            double fwdX = -Math.sin(this.baseFlightAngle);
            double fwdZ = Math.cos(this.baseFlightAngle);

            double dist = 42.0 + random.nextDouble() * 38.0;
            double targetX = this.magpie.getX() + fwdX * dist;
            double targetZ = this.magpie.getZ() + fwdZ * dist;

            int surfaceY = this.magpie.level().getHeight(Heightmap.Types.WORLD_SURFACE, (int) targetX, (int) targetZ);
            // Dynamic altitude swooping (15 to 28 blocks above terrain with organic variance)
            double targetY = Math.max(this.magpie.getY(), surfaceY + 15.0 + random.nextDouble() * 13.0);

            this.targetFlightPos = new Vec3(targetX, targetY, targetZ);
            this.waypointTicks = 120 + random.nextInt(100);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.magpie.isPerched() && !this.magpie.hasFlockLeader() && this.waypointTicks > 0;
        }

        @Override
        public void tick() {
            this.waypointTicks--;
            if (this.targetFlightPos == null || this.magpie.position().distanceToSqr(this.targetFlightPos) < 144.0) {
                chooseHighFlightTarget();
            }

            // Real-life Eurasian Magpie cruising speed: ~40 km/h = 0.54 blocks/tick
            // Slight natural speed modulation (0.50 to 0.58)
            double t = (this.magpie.tickCount + this.wanderSeed) * 0.02;
            double speedMod = Math.sin(t * 1.3) * 0.04;
            double cruiseSpeed = 0.54 + speedMod;

            Vec3 pos = this.magpie.position();
            Vec3 toTarget = this.targetFlightPos.subtract(pos);

            // Dynamic multi-harmonic S-curve meandering for the leader:
            // Combines 3 distinct frequencies so flight curves never repeat in a simple loop!
            double sCurveMeander = Math.sin(t * 1.0) * 0.38
                                 + Math.sin(t * 2.41) * 0.22
                                 + Math.cos(t * 0.53) * 0.28;

            float targetYaw = (float)(Mth.atan2(toTarget.z, toTarget.x) * (180.0 / Math.PI)) - 90.0F + (float)Math.toDegrees(sCurveMeander);

            float currentYaw = this.magpie.getYRot();
            float newYaw = Mth.rotLerp(0.08F, currentYaw, targetYaw);
            this.magpie.setYRot(newYaw);
            this.magpie.yBodyRot = newYaw;
            this.magpie.yHeadRot = newYaw;

            // Forward flight vector strictly derived from current yaw
            double rad = Math.toRadians(newYaw);
            double fwdX = -Math.sin(rad);
            double fwdZ = Math.cos(rad);

            // Dynamic rollercoaster vertical swooping:
            double altitudeWave = Math.sin(t * 1.5) * 0.08;
            double dy = (this.targetFlightPos.y - pos.y);
            double climbSpeed = Mth.clamp(dy * 0.05 + altitudeWave, -0.22, 0.25);

            Vec3 avoidance = getLeaderAvoidance();

            double finalVx = fwdX * cruiseSpeed + avoidance.x;
            double finalVz = fwdZ * cruiseSpeed + avoidance.z;
            double finalVy = climbSpeed + avoidance.y;

            this.magpie.setDeltaMovement(new Vec3(finalVx, finalVy, finalVz));
            this.magpie.setOnGround(false);

            float targetPitch = (float)(-Math.atan2(finalVy, cruiseSpeed) * (180.0 / Math.PI));
            this.magpie.setXRot(Mth.rotLerp(0.15F, this.magpie.getXRot(), targetPitch));
        }

        private Vec3 getLeaderAvoidance() {
            if (this.magpie.tickCount % 4 != 0) return Vec3.ZERO;
            UUID myFlock = this.magpie.getFlockId();
            List<MagpieEntity> otherLeaders = this.magpie.level().getEntitiesOfClass(
                    MagpieEntity.class,
                    this.magpie.getBoundingBox().inflate(24.0),
                    other -> other != this.magpie && other.isAlive() && !other.hasFlockLeader() && !myFlock.equals(other.getFlockId())
            );

            if (otherLeaders.isEmpty()) return Vec3.ZERO;

            Vec3 repel = Vec3.ZERO;
            for (MagpieEntity other : otherLeaders) {
                Vec3 diff = this.magpie.position().subtract(other.position());
                double distSq = diff.lengthSqr();
                if (distSq > 0.01 && distSq < 24.0 * 24.0) {
                    double strength = (24.0 - Math.sqrt(distSq)) / 24.0;
                    repel = repel.add(diff.normalize().scale(strength * 0.06));
                }
            }
            return repel;
        }
    }

    static class MagpiePerchInTreeGoal extends Goal {
        private final MagpieEntity magpie;
        private BlockPos targetLeafPos = null;
        private int cooldown = 0;

        public MagpiePerchInTreeGoal(MagpieEntity magpie) {
            this.magpie = magpie;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.magpie.isPerched() || this.magpie.isBaby() || this.magpie.hasFlockLeader() || this.magpie.isFlying()) return false;
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            if (this.magpie.getRandom().nextInt(40) != 0) return false;

            BlockPos current = this.magpie.blockPosition();
            BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
            var level = this.magpie.level();
            var random = this.magpie.getRandom();

            for (int i = 0; i < 12; i++) {
                int dx = random.nextInt(25) - 12;
                int dy = random.nextInt(9) - 3;
                int dz = random.nextInt(25) - 12;
                mut.set(current.getX() + dx, current.getY() + dy, current.getZ() + dz);

                if (level.getBlockState(mut).is(BlockTags.LEAVES)) {
                    BlockPos above = mut.above();
                    if (level.getBlockState(above).isAir()) {
                        this.targetLeafPos = mut.immutable();
                        return true;
                    }
                }
            }
            this.cooldown = 80 + random.nextInt(40);
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetLeafPos != null && !this.magpie.isPerched();
        }

        @Override
        public void start() {
            if (this.targetLeafPos != null) {
                this.magpie.getNavigation().moveTo(
                        this.targetLeafPos.getX() + 0.5,
                        this.targetLeafPos.getY() + 1.0,
                        this.targetLeafPos.getZ() + 0.5,
                        1.3
                );
            }
        }

        @Override
        public void tick() {
            if (this.targetLeafPos == null) return;
            double distSq = this.magpie.distanceToSqr(
                    this.targetLeafPos.getX() + 0.5,
                    this.targetLeafPos.getY() + 1.0,
                    this.targetLeafPos.getZ() + 0.5
            );
            if (distSq < 2.0) {
                this.magpie.startPerching(this.targetLeafPos, 100 + this.magpie.getRandom().nextInt(120));
                this.targetLeafPos = null;
                this.cooldown = 200;
            }
        }

        @Override
        public void stop() {
            this.targetLeafPos = null;
            if (this.cooldown <= 0) this.cooldown = 120;
        }
    }

    public static class MagpieMoveControl extends net.minecraft.world.entity.ai.control.FlyingMoveControl {
        private final MagpieEntity magpie;

        public MagpieMoveControl(MagpieEntity magpie) {
            super(magpie, 10, false);
            this.magpie = magpie;
        }

        @Override
        public void tick() {
            if (this.magpie.isFlying()) {
                // When in flight (leader and followers), physics, aerodynamics, and heading
                // are driven smoothly by our dedicated aerodynamic flight AI at real-life magpie speeds
                return;
            }
            super.tick();
        }
    }

    static class MagpieLookAtPlayerGoal extends LookAtPlayerGoal {
        private final MagpieEntity magpie;

        public MagpieLookAtPlayerGoal(MagpieEntity magpie) {
            super(magpie, Player.class, 6.0F);
            this.magpie = magpie;
        }

        @Override
        public boolean canUse() {
            if (this.magpie.isFlying()) return false;
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            if (this.magpie.isFlying()) return false;
            return super.canContinueToUse();
        }
    }

    static class MagpieRandomLookAroundGoal extends RandomLookAroundGoal {
        private final MagpieEntity magpie;

        public MagpieRandomLookAroundGoal(MagpieEntity magpie) {
            super(magpie);
            this.magpie = magpie;
        }

        @Override
        public boolean canUse() {
            if (this.magpie.isFlying()) return false;
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            if (this.magpie.isFlying()) return false;
            return super.canContinueToUse();
        }
    }
}
