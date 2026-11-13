/* V2_DISABLED
package atom.cozybirds.client.renderer;

import atom.cozybirds.CozyBirds;
import atom.cozybirds.client.model.GoldenEagleModel;
import atom.cozybirds.client.renderer.state.GoldenEagleRenderState;
import atom.cozybirds.entity.eagle.GoldenEagleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class GoldenEagleRenderer extends MobRenderer<GoldenEagleEntity, GoldenEagleRenderState, GoldenEagleModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(CozyBirds.MOD_ID, "textures/entity/eagle/golden_eagle.png");

    public GoldenEagleRenderer(EntityRendererProvider.Context context) {
        super(context, new GoldenEagleModel(context.bakeLayer(GoldenEagleModel.LAYER_LOCATION)), 0.55F);
    }

    @Override
    public GoldenEagleRenderState createRenderState() {
        return new GoldenEagleRenderState();
    }

    @Override
    public void extractRenderState(GoldenEagleEntity entity, GoldenEagleRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isFlying = entity.isFlying();
        state.isPerched = entity.isPerched();
        state.isSoaring = entity.isSoaring();
        state.isDiving = entity.isDiving();
        state.isScreeching = entity.isScreeching();

        state.soarProgress = entity.getSoarProgress(partialTick);
        state.diveProgress = entity.getDiveProgress(partialTick);
        state.screechProgress = entity.getScreechProgress(partialTick);

        state.flapAngle = entity.getFlapAngle(partialTick);
        state.flightProgress = entity.getFlightProgress(partialTick);
        state.bank = entity.getBank(partialTick) * state.flightProgress;
        state.pitchTilt = entity.getPitchTilt(partialTick) * state.flightProgress;

        var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        if (cam != null && cam.isInitialized()) {
            net.minecraft.world.phys.Vec3 camPos = cam.position();
            state.distanceToCamera = (float) Math.sqrt(entity.distanceToSqr(camPos.x, camPos.y, camPos.z));
        }
    }

    @Override
    public void submit(GoldenEagleRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        poseStack.pushPose();

        // High altitude silhouette scaling so the soaring raptor is clearly visible from far away!
        float dist = state.distanceToCamera;
        if (dist > 18.0F) {
            float distScale = Math.min(1.40F, 1.0F + (dist - 18.0F) / 45.0F * 0.40F);
            poseStack.scale(distScale, distScale, distScale);
        }

        if (state.flightProgress > 0.01F) {
            poseStack.translate(0.0F, 0.75F, 0.0F);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(state.bank));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(state.pitchTilt));
            poseStack.translate(0.0F, -0.75F, 0.0F);
        }

        super.submit(state, poseStack, collector, camera);
        poseStack.popPose();
    }

    @Override
    public Identifier getTextureLocation(GoldenEagleRenderState state) {
        return TEXTURE;
    }
}

*/