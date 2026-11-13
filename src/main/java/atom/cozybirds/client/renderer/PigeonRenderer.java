package atom.cozybirds.client.renderer;

import atom.cozybirds.client.model.PigeonModel;
import atom.cozybirds.client.renderer.state.PigeonRenderState;
import atom.cozybirds.entity.pigeon.PigeonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public class PigeonRenderer extends MobRenderer<PigeonEntity, PigeonRenderState, PigeonModel> {

    public PigeonRenderer(EntityRendererProvider.Context context) {
        super(context, new PigeonModel(context.bakeLayer(PigeonModel.LAYER_LOCATION)), 0.22F);
    }

    @Override
    public PigeonRenderState createRenderState() {
        return new PigeonRenderState();
    }

    @Override
    public void extractRenderState(PigeonEntity entity, PigeonRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.variant = entity.getVariant();
        state.isFlying = entity.isFlying();
        state.isPerched = entity.isPerched();
        state.flapAngle = entity.getFlapAngle(partialTick);
        state.flightProgress = entity.getFlightProgress(partialTick);
        state.bank = entity.getBank(partialTick) * state.flightProgress;
        state.pitchTilt = entity.getPitchTilt(partialTick) * state.flightProgress;
        state.action = entity.getAction();
        state.isPecking = entity.isPecking();
        state.isCooing = entity.isCooing();
        state.isPreening = entity.isPreening();

        var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        if (cam != null && cam.isInitialized()) {
            net.minecraft.world.phys.Vec3 camPos = cam.position();
            state.distanceToCamera = (float) Math.sqrt(entity.distanceToSqr(camPos.x, camPos.y, camPos.z));
        }
    }

    @Override
    public void submit(PigeonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();

        // 1. Scale down high-definition 256x256 model to real-world IRL Pigeon dimensions (~34 cm length, ~66 cm wingspan)
        float irlScale = 0.18F;
        poseStack.scale(irlScale, irlScale, irlScale);

        // 2. Distance scaling to keep pigeon silhouettes crisp at far distance
        float dist = state.distanceToCamera;
        if (dist > 18.0F) {
            float distScale = Math.min(1.30F, 1.0F + (dist - 18.0F) / 45.0F * 0.30F);
            poseStack.scale(distScale, distScale, distScale);
        }

        // 3. Flight banking and pitch orientation
        if (state.flightProgress > 0.01F) {
            float fp = state.flightProgress;
            poseStack.translate(0.0F, 1.20F, 0.0F);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(state.bank * fp));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(state.pitchTilt * fp));
            poseStack.translate(0.0F, -1.20F, 0.0F);
        }

        super.submit(state, poseStack, collector, camera);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(PigeonEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ) {
        if (!entity.shouldRender(camX, camY, camZ)) {
            return false;
        }
        return frustum.isVisible(entity.getBoundingBox().inflate(1.5, 1.0, 1.5));
    }

    @Override
    public Identifier getTextureLocation(PigeonRenderState state) {
        return state.variant.getTexture();
    }
}
