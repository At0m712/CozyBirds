/* V2_DISABLED
package atom.cozybirds.client.renderer;

import atom.cozybirds.CozyBirds;
import atom.cozybirds.client.model.FalconModel;
import atom.cozybirds.client.renderer.state.FalconRenderState;
import atom.cozybirds.entity.falcon.PeregrineFalconEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class FalconRenderer extends MobRenderer<PeregrineFalconEntity, FalconRenderState, FalconModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(CozyBirds.MOD_ID, "textures/entity/falcon/falcon.png");

    public FalconRenderer(EntityRendererProvider.Context context) {
        super(context, new FalconModel(context.bakeLayer(FalconModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public FalconRenderState createRenderState() {
        return new FalconRenderState();
    }

    @Override
    public void extractRenderState(PeregrineFalconEntity entity, FalconRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isFlying = entity.isFlying();
        state.isPerched = entity.isPerched();
        state.isDiving = entity.isDiving();
        state.isAiming = entity.isAiming();
        state.isEating = entity.isEating();
        state.flapAngle = entity.getFlapAngle(partialTick);
        state.flightProgress = entity.getFlightProgress(partialTick);
        state.bank = entity.getBank(partialTick) * state.flightProgress;
        state.pitchTilt = entity.getPitchTilt(partialTick) * state.flightProgress;

        var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.mainCamera();
        if (cam != null && cam.isInitialized()) {
            net.minecraft.world.phys.Vec3 camPos = cam.position();
            state.distanceToCamera = (float) Math.sqrt(entity.distanceToSqr(camPos.x, camPos.y, camPos.z));
        }
    }

    @Override
    public void submit(FalconRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        poseStack.pushPose();
        float dist = state.distanceToCamera;
        if (dist > 18.0F) {
            float distScale = Math.min(1.40F, 1.0F + (dist - 18.0F) / 45.0F * 0.40F);
            poseStack.scale(distScale, distScale, distScale);
        }
        if (state.flightProgress > 0.01F) {
            poseStack.translate(0.0F, 1.15F, 0.0F);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(state.bank));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(state.pitchTilt));
            poseStack.translate(0.0F, -1.15F, 0.0F);
        }
        super.submit(state, poseStack, collector, camera);
        poseStack.popPose();
    }

    @Override
    public Identifier getTextureLocation(FalconRenderState state) {
        return TEXTURE;
    }
}

*/