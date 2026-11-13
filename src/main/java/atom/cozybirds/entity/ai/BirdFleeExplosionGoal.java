package atom.cozybirds.entity.ai;

import atom.cozybirds.entity.BirdEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class BirdFleeExplosionGoal extends Goal {
    private final BirdEntity bird;
    private Entity dangerEntity;
    private Vec3 fleeTarget;
    private int panicTicks;

    private int checkCooldown = 0;

    public BirdFleeExplosionGoal(BirdEntity bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.panicTicks > 0) {
            return true;
        }

        if (--this.checkCooldown > 0) {
            return false;
        }
        // Stagger checks every 8-11 ticks (~0.5s) to save massive CPU without losing responsiveness
        this.checkCooldown = 8 + (this.bird.getId() & 3);

        // Scan for Primed TNT within 16 blocks
        List<PrimedTnt> tnts = this.bird.level().getEntitiesOfClass(
                PrimedTnt.class,
                this.bird.getBoundingBox().inflate(16.0),
                Entity::isAlive
        );
        if (!tnts.isEmpty()) {
            this.dangerEntity = tnts.getFirst();
            return true;
        }

        // Scan for Creeper about to explode (swelling or ignited) within 12 blocks
        List<Creeper> creepers = this.bird.level().getEntitiesOfClass(
                Creeper.class,
                this.bird.getBoundingBox().inflate(12.0),
                c -> c.isAlive() && (c.isIgnited() || c.getSwellDir() > 0)
        );
        if (!creepers.isEmpty()) {
            this.dangerEntity = creepers.getFirst();
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.panicTicks > 0 && !this.bird.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.panicTicks = 70 + this.bird.getRandom().nextInt(30);

        if (this.bird.isPerched()) {
            this.bird.stopPerching();
        }

        // Take off into flight immediately
        this.bird.setDeltaMovement(new Vec3(
                (this.bird.getRandom().nextDouble() - 0.5) * 0.4,
                0.55,
                (this.bird.getRandom().nextDouble() - 0.5) * 0.4
        ));
        this.bird.playSound(SoundEvents.PARROT_FLY, 0.4F, 1.2F);

        findFleePosition();
    }

    private void findFleePosition() {
        Vec3 dangerPos = this.dangerEntity != null ? this.dangerEntity.position() : this.bird.position();
        Vec3 awayDir = this.bird.position().subtract(dangerPos).normalize();
        if (awayDir.lengthSqr() < 0.01) {
            awayDir = new Vec3(this.bird.getRandom().nextDouble() - 0.5, 0.3, this.bird.getRandom().nextDouble() - 0.5).normalize();
        }

        // Target high in the air away from danger
        Vec3 target = this.bird.position().add(awayDir.x * 18.0, 6.0 + this.bird.getRandom().nextDouble() * 4.0, awayDir.z * 18.0);
        this.fleeTarget = target;
        this.bird.getNavigation().moveTo(target.x, target.y, target.z, 1.6);
    }

    @Override
    public void tick() {
        this.panicTicks--;
        if (this.fleeTarget != null) {
            this.bird.getLookControl().setLookAt(this.fleeTarget.x, this.fleeTarget.y, this.fleeTarget.z, 30.0F, 30.0F);
        }

        if (this.panicTicks % 20 == 0 && this.dangerEntity != null && this.dangerEntity.isAlive()) {
            findFleePosition();
        }
    }

    @Override
    public void stop() {
        this.dangerEntity = null;
        this.fleeTarget = null;
        this.panicTicks = 0;
    }
}
