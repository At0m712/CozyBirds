package atom.cozybirds.entity.ai;

import atom.cozybirds.entity.BirdEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class PerchOnBlockGoal extends Goal {
    private final BirdEntity bird;
    private BlockPos targetPerch;
    private int searchCooldown;

    public PerchOnBlockGoal(BirdEntity bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.bird.isPerched() || this.bird.isPassenger()) {
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        // Only search occasionally to keep performance top notch
        if (this.bird.getRandom().nextInt(60) != 0) {
            return false;
        }

        this.targetPerch = findNearbyPerch();
        if (this.targetPerch == null) {
            // Set cooldown on miss to prevent burning CPU every tick
            this.searchCooldown = 60 + this.bird.getRandom().nextInt(40);
            return false;
        }
        return true;
    }

    private BlockPos findNearbyPerch() {
        BlockPos origin = this.bird.blockPosition();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        var level = this.bird.level();
        var random = this.bird.getRandom();

        // Sample 12 random nearby positions, checking chunk boundary safely
        for (int i = 0; i < 12; i++) {
            int dx = random.nextInt(25) - 12;
            int dy = random.nextInt(9) - 4;
            int dz = random.nextInt(25) - 12;
            int targetX = origin.getX() + dx;
            int targetZ = origin.getZ() + dz;
            if (!level.hasChunk(targetX >> 4, targetZ >> 4)) continue;

            mutable.set(targetX, origin.getY() + dy, targetZ);
            if (BirdEntity.isValidPerch(level, mutable)) {
                return mutable.immutable();
            }
        }
        return null;
    }

    @Override
    public void start() {
        if (this.targetPerch != null) {
            Vec3 target = BirdEntity.getPerchPosition(this.bird.level(), this.targetPerch);
            this.bird.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.bird.isPerched() && this.targetPerch != null
                && BirdEntity.isValidPerch(this.bird.level(), this.targetPerch)
                && !this.bird.getNavigation().isDone();
    }

    @Override
    public void tick() {
        if (this.targetPerch == null) {
            return;
        }

        Vec3 target = BirdEntity.getPerchPosition(this.bird.level(), this.targetPerch);
        double distSq = this.bird.distanceToSqr(target);

        this.bird.getLookControl().setLookAt(target.x, target.y, target.z, 20.0F, 20.0F);

        if (distSq < 0.7) {
            int duration = 250 + this.bird.getRandom().nextInt(350);
            this.bird.startPerching(this.targetPerch, duration);
            this.targetPerch = null;
            this.searchCooldown = 300;
        }
    }

    @Override
    public void stop() {
        this.targetPerch = null;
        if (!this.bird.isPerched()) {
            this.searchCooldown = 100;
        }
    }
}
