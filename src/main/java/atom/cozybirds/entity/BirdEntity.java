package atom.cozybirds.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class BirdEntity extends Animal {
    private static final EntityDataAccessor<Boolean> DATA_PERCHED =
            SynchedEntityData.defineId(BirdEntity.class, EntityDataSerializers.BOOLEAN);

    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    public float flapping = 1.0F;
    public float nextFlap = 1.0F;

    private BlockPos perchPos = null;
    private int perchTicks = 0;

    protected float oFlightProgress = 0.0F;
    protected float flightProgress = 0.0F;

    protected float oBank = 0.0F;
    protected float bank = 0.0F;
    protected float oPitchTilt = 0.0F;
    protected float pitchTilt = 0.0F;

    protected BirdEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, -1.0F);
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.setPathfindingMalus(PathType.COCOA, -1.0F);
    }

    public static AttributeSupplier.Builder createBirdAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PERCHED, false);
    }

    public boolean isPerched() {
        return this.entityData.get(DATA_PERCHED);
    }

    public void setPerched(boolean perched) {
        this.entityData.set(DATA_PERCHED, perched);
        if (perched) {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    public void startPerching(BlockPos pos, int durationTicks) {
        this.perchPos = pos;
        this.perchTicks = durationTicks;
        Vec3 perchVec = getPerchPosition(this.level(), pos);
        this.teleportTo(perchVec.x, perchVec.y, perchVec.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setPerched(true);

        // If perching on an edge, orient facing outward towards the drop
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (this.level().getBlockState(neighbor).isAir()) {
                float targetYaw = dir.toYRot();
                this.setYRot(targetYaw);
                this.yBodyRot = targetYaw;
                this.yHeadRot = targetYaw;
                break;
            }
        }
    }

    public void stopPerching() {
        if (isPerched()) {
            this.setPerched(false);
            this.perchPos = null;
            this.perchTicks = 0;
            // Launch up into flight
            this.setDeltaMovement(new Vec3(
                    (this.random.nextDouble() - 0.5) * 0.2,
                    0.35,
                    (this.random.nextDouble() - 0.5) * 0.2
            ));
            this.playSound(getFlapSound(), 0.15F, 1.0F);
        }
    }

    public static boolean isBlockEdge(LevelReader level, BlockPos pos, BlockState state) {
        if (!state.isSolid()) {
            return false;
        }
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            mut.set(pos.getX() + dir.getStepX(), pos.getY(), pos.getZ() + dir.getStepZ());
            BlockState neighborState = level.getBlockState(mut);
            if (neighborState.isAir() || !neighborState.isSolid()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlockEdge(LevelReader level, BlockPos pos) {
        return isBlockEdge(level, pos, level.getBlockState(pos));
    }

    public static boolean isValidPerch(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (!aboveState.isAir() && aboveState.isSolid()) {
            return false;
        }

        // Horizontal chain
        if (state.getBlock() instanceof ChainBlock
                && state.hasProperty(RotatedPillarBlock.AXIS)
                && state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) {
            return true;
        }

        // Fences and walls
        if (state.is(BlockTags.FENCES) || state.getBlock() instanceof FenceBlock) return true;
        if (state.is(BlockTags.WALLS) || state.getBlock() instanceof WallBlock) return true;

        // Tree leaves
        if (state.is(BlockTags.LEAVES)) return true;

        // Slabs and stairs
        if (state.getBlock() instanceof SlabBlock) return true;
        if (state.getBlock() instanceof StairBlock) return true;

        // Lanterns
        if (state.getBlock() instanceof LanternBlock) return true;

        // Roof edges, parapets, building borders
        return isBlockEdge(level, pos, state);
    }

    public static Vec3 getPerchPosition(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChainBlock) {
            return new Vec3(pos.getX() + 0.5, pos.getY() + 0.52, pos.getZ() + 0.5);
        } else if (state.getBlock() instanceof SlabBlock) {
            if (state.hasProperty(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) {
                return new Vec3(pos.getX() + 0.5, pos.getY() + 0.50, pos.getZ() + 0.5);
            }
            return new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        } else if (state.getBlock() instanceof LanternBlock) {
            return new Vec3(pos.getX() + 0.5, pos.getY() + 0.56, pos.getZ() + 0.5);
        } else {
            return new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        }
    }

    public boolean isFlying() {
        return !this.onGround() && !this.isPerched();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        calculateFlapping();

        this.oFlightProgress = this.flightProgress;
        if (this.isFlying()) {
            this.flightProgress = Math.min(1.0F, this.flightProgress + 0.1F);
        } else {
            this.flightProgress = Math.max(0.0F, this.flightProgress - 0.1F);
        }

        this.oBank = this.bank;
        this.oPitchTilt = this.pitchTilt;
        if (this.isFlying()) {
            float yawDelta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            float targetBank = Mth.clamp(-yawDelta * 0.055F, -0.70F, 0.70F);
            this.bank = Mth.lerp(0.20F, this.bank, targetBank);

            Vec3 vel = this.getDeltaMovement();
            float targetPitch = (float) Mth.clamp(-vel.y * 1.5, -0.50, 0.50);
            this.pitchTilt = Mth.lerp(0.20F, this.pitchTilt, targetPitch);
        } else {
            this.bank = Mth.lerp(0.30F, this.bank, 0.0F);
            this.pitchTilt = Mth.lerp(0.30F, this.pitchTilt, 0.0F);
        }

        if (this.isPerched()) {
            this.setDeltaMovement(Vec3.ZERO);

            if (!this.level().isClientSide()) {
                if (this.perchPos == null || !isValidPerch(this.level(), this.perchPos)) {
                    stopPerching();
                    return;
                }

                this.perchTicks--;
                if (this.perchTicks <= 0) {
                    stopPerching();
                    return;
                }

                // Check for nearby scary players
                Player closestPlayer = this.level().getNearestPlayer(this, 3.5);
                if (closestPlayer != null && !closestPlayer.isDiscrete()) {
                    stopPerching();
                }
            }
        }
    }

    private void calculateFlapping() {
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        boolean airborne = !this.onGround() && !this.isPerched() && !this.isPassenger();
        this.flapSpeed += (airborne ? 4 : -1) * 0.3F;
        this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);

        if (airborne && this.flapping < 1.0F) {
            this.flapping = 1.0F;
        }
        this.flapping *= 0.9F;

        Vec3 delta = this.getDeltaMovement();
        if (airborne && delta.y < 0.0) {
            this.setDeltaMovement(delta.multiply(1.0, 0.6, 1.0));
        }

        float freq = getFlapFrequency();
        this.flap += airborne ? (freq * this.flapSpeed) : (this.flapping * 1.2F);
    }

    public float getFlapFrequency() {
        return 1.73F; // Default ~5.5 Hz
    }

    public float getFlapAngle(float partialTick) {
        return Mth.lerp(partialTick, this.oFlap, this.flap);
    }

    public float getFlightProgress(float partialTick) {
        return Mth.lerp(partialTick, this.oFlightProgress, this.flightProgress);
    }

    public float getBank(float partialTick) {
        return Mth.lerp(partialTick, this.oBank, this.bank);
    }

    public float getPitchTilt(float partialTick) {
        return Mth.lerp(partialTick, this.oPitchTilt, this.pitchTilt);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isPerched()) {
            stopPerching();
        }
        // Panicked launch into air when hurt
        this.setDeltaMovement(new Vec3(
                (this.random.nextDouble() - 0.5) * 0.4,
                0.5,
                (this.random.nextDouble() - 0.5) * 0.4
        ));
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return !this.isFlying() && !this.isPerched();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // Birds take no fall damage while flapping
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        double maxDist = 96.0 * getViewScale();
        return distanceSqr < maxDist * maxDist;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isPersistenceRequired() && !this.hasCustomName();
    }

    protected SoundEvent getFlapSound() {
        return SoundEvents.PARROT_FLY;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Perched", this.isPerched());
        if (this.perchPos != null) {
            output.putInt("PerchX", this.perchPos.getX());
            output.putInt("PerchY", this.perchPos.getY());
            output.putInt("PerchZ", this.perchPos.getZ());
            output.putInt("PerchTicks", this.perchTicks);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        boolean perched = input.getBooleanOr("Perched", false);
        this.setPerched(perched);
        if (perched && input.getInt("PerchX").isPresent()) {
            int x = input.getIntOr("PerchX", 0);
            int y = input.getIntOr("PerchY", 0);
            int z = input.getIntOr("PerchZ", 0);
            this.perchPos = new BlockPos(x, y, z);
            this.perchTicks = input.getIntOr("PerchTicks", 200);
        }
    }
}
