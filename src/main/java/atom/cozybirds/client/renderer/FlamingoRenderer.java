/* V2_DISABLED
package atom.cozybirds.client.renderer;

import atom.cozybirds.CozyBirds;
import atom.cozybirds.client.model.FlamingoModel;
import atom.cozybirds.client.renderer.state.FlamingoRenderState;
import atom.cozybirds.entity.flamingo.FlamingoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class FlamingoRenderer extends MobRenderer<FlamingoEntity, FlamingoRenderState, FlamingoModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(CozyBirds.MOD_ID, "textures/entity/flamingo/flamingo.png");

    public FlamingoRenderer(EntityRendererProvider.Context context) {
        super(context, new FlamingoModel(context.bakeLayer(FlamingoModel.LAYER_LOCATION)), 0.45F);
    }

    @Override
    public FlamingoRenderState createRenderState() {
        return new FlamingoRenderState();
    }

    @Override
    public void extractRenderState(FlamingoEntity entity, FlamingoRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isFlying = entity.isFlying();
        state.isPerched = entity.isPerched();
        state.isStandingOneLeg = entity.isStandingOnOneLeg();
        state.isLeftLegRaised = entity.isLeftLegRaised();
        state.isFilterFeeding = entity.isFilterFeeding();
        state.isMudStamping = entity.isMudStamping();
        state.isHeadFlagging = entity.isHeadFlagging();
        state.isSentinelAlert = entity.isSentinelAlert();
        state.isRunningTakeoff = entity.isRunningTakeoff();
        state.oneLegProgress = entity.getOneLegProgress(partialTick);
        state.filterFeedProgress = entity.getFilterFeedProgress(partialTick);
        state.mudStampProgress = entity.getMudStampProgress(partialTick);
        state.sentinelProgress = entity.getSentinelProgress(partialTick);
        state.headFlagProgress = entity.getHeadFlagProgress(partialTick);
        state.takeoffProgress = entity.getTakeoffProgress(partialTick);
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
    public void submit(FlamingoRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        poseStack.pushPose();
        float dist = state.distanceToCamera;
        if (dist > 18.0F) {
            float distScale = Math.min(1.35F, 1.0F + (dist - 18.0F) / 45.0F * 0.35F);
            poseStack.scale(distScale, distScale, distScale);
        }
        if (state.flightProgress > 0.01F) {
            poseStack.translate(0.0F, 1.10F, 0.0F);
            poseStack.rotate(com.mojang.math.Axis.ZP, state.bank);
            poseStack.rotate(com.mojang.math.Axis.XP, state.pitchTilt);
            poseStack.translate(0.0F, -1.10F, 0.0F);
        }
        super.submit(state, poseStack, collector, camera);
        poseStack.popPose();
    }

    @Override
    public Identifier getTextureLocation(FlamingoRenderState state) {
        return TEXTURE;
    }
}

*/