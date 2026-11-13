package atom.cozybirds.client.renderer;

import atom.cozybirds.CozyBirds;
import atom.cozybirds.client.model.MagpieModel;
import atom.cozybirds.client.renderer.state.MagpieRenderState;
import atom.cozybirds.entity.magpie.MagpieEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public class MagpieRenderer extends MobRenderer<MagpieEntity, MagpieRenderState, MagpieModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(CozyBirds.MOD_ID, "textures/entity/magpie/magpie.png");

    public MagpieRenderer(EntityRendererProvider.Context context) {
        super(context, new MagpieModel(context.bakeLayer(MagpieModel.LAYER_LOCATION)), 0.20F);
    }

    @Override
    public MagpieRenderState createRenderState() {
        return new MagpieRenderState();
    }

    @Override
    public void extractRenderState(MagpieEntity entity, MagpieRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
        state.isFlying = entity.isFlying();
        state.isPerched = entity.isPerched();
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
    public void submit(MagpieRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();

        // 1. Scale down high-definition 256x256 model to real-world IRL Magpie dimensions (~45 cm length, ~55 cm wingspan)
        float irlScale = 0.32F;
        poseStack.scale(irlScale, irlScale, irlScale);

        // 2. Distance scaling to keep fast murmurations clearly readable at far distance
        float dist = state.distanceToCamera;
        if (dist > 18.0F) {
            float distScale = Math.min(1.30F, 1.0F + (dist - 18.0F) / 45.0F * 0.30F);
            poseStack.scale(distScale, distScale, distScale);
        }

        // 3. Flight banking and pitch orientation in murmuration swoops
        if (state.flightProgress > 0.01F) {
            poseStack.translate(0.0F, 0.85F, 0.0F);
            poseStack.rotate(com.mojang.math.Axis.ZP, state.bank);
            poseStack.rotate(com.mojang.math.Axis.XP, state.pitchTilt);
            poseStack.translate(0.0F, -0.85F, 0.0F);
        }

        super.submit(state, poseStack, collector, camera);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(MagpieEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ, float partialTick) {
        if (!entity.shouldRender(camX, camY, camZ)) {
            return false;
        }
        return frustum.isVisible(entity.getBoundingBox().inflate(1.5, 1.0, 1.5));
    }

    @Override
    public Identifier getTextureLocation(MagpieRenderState state) {
        return TEXTURE;
    }
}
