/* V2_DISABLED
package atom.cozybirds.client.model;

import atom.cozybirds.CozyBirds;
import atom.cozybirds.client.renderer.state.FalconRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class FalconModel extends EntityModel<FalconRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(CozyBirds.MOD_ID, "falcon"), "main");

    private final ModelPart body;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart head;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart tail;

    public FalconModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.rightWing = root.getChild("rightWing");
        this.leftWing = root.getChild("leftWing");
        this.head = root.getChild("head");
        this.rightLeg = root.getChild("rightLeg");
        this.leftLeg = root.getChild("leftLeg");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Simple, clean raptor cuboids with 64x32 texture layout
        partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.5F, -3.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 19.0F, 0.0F));

        partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(22, 0)
                        .addBox(-1.5F, -3.0F, -1.5F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
                        // Curved raptor beak
                        .texOffs(36, 0)
                        .addBox(-1.0F, -1.5F, -3.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 16.5F, -2.5F));

        partdefinition.addOrReplaceChild("rightWing",
                CubeListBuilder.create().texOffs(0, 12)
                        .addBox(-1.0F, 0.0F, -2.0F, 1.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.5F, 16.5F, 0.0F));

        partdefinition.addOrReplaceChild("leftWing",
                CubeListBuilder.create().texOffs(16, 12)
                        .addBox(0.0F, 0.0F, -2.0F, 1.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.5F, 16.5F, 0.0F));

        partdefinition.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(32, 12)
                        .addBox(-2.0F, 0.0F, 0.0F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 19.5F, 2.5F, 0.3F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("rightLeg",
                CubeListBuilder.create().texOffs(46, 0)
                        .addBox(-0.5F, 0.0F, 0.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(52, 0)
                        .addBox(-1.0F, 3.0F, -1.5F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.5F, 21.0F, 0.0F));

        partdefinition.addOrReplaceChild("leftLeg",
                CubeListBuilder.create().texOffs(46, 5)
                        .addBox(-0.5F, 0.0F, 0.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(52, 5)
                        .addBox(-1.0F, 3.0F, -1.5F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.5F, 21.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @Override
    public void setupAnim(FalconRenderState state) {
        this.resetPose();

        float fp = state.flightProgress;
        float headYaw = state.yRot * ((float)Math.PI / 180F);
        float headPitch = state.xRot * ((float)Math.PI / 180F);

        if (state.isAiming) {
            // Predatory lock-on: falcon hovers stationary above fish, looking down intently
            this.body.xRot = 0.65F;
            this.body.y = 17.0F;
            this.head.xRot = 1.10F + headPitch * 0.3F;
            this.head.y = 16.0F;
            this.head.z = -3.5F;
            this.head.yRot = headYaw * 0.5F;

            // Wings outstretched hovering with slight tremor
            float hoverFlutter = Mth.sin(state.ageInTicks * 0.5F) * 0.12F;
            this.rightWing.zRot = 1.45F + hoverFlutter;
            this.leftWing.zRot = -1.45F - hoverFlutter;
            this.rightWing.xRot = 0.1F;
            this.leftWing.xRot = 0.1F;
            this.rightWing.y = 16.0F;
            this.leftWing.y = 16.0F;

            this.rightLeg.xRot = 0.9F;
            this.leftLeg.xRot = 0.9F;
            this.tail.xRot = 0.5F;
            return;
        }

        if (state.isDiving) {
            // High-speed stoop dive towards water: wings pinned back, aerodynamic nose-down
            this.body.xRot = 1.15F;
            this.body.y = 17.5F;
            this.head.xRot = headPitch * 0.2F - 0.9F;
            this.head.y = 16.0F;
            this.head.z = -4.0F;
            this.head.yRot = headYaw * 0.3F;

            // Wings pinned back tight against flanks
            this.rightWing.xRot = 1.15F;
            this.leftWing.xRot = 1.15F;
            this.rightWing.zRot = -0.2F;
            this.leftWing.zRot = 0.2F;
            this.rightWing.y = 16.5F;
            this.leftWing.y = 16.5F;

            this.rightLeg.xRot = 1.35F;
            this.leftLeg.xRot = 1.35F;
            this.rightLeg.y = 19.5F;
            this.leftLeg.y = 19.5F;
            this.tail.xRot = 1.0F;

            // Bank during high speed dive steering
            float bank = state.bank * 1.5F;
            this.body.zRot = bank;
            this.tail.zRot = -bank;
            return;
        }

        // Ground defaults
        float bodyXRot = 0.0F;
        float bodyY = 19.0F;
        float headXRot = headPitch;
        float headY = 16.5F;
        float headZ = -2.5F;
        float tailXRot = 0.3F;
        float tailY = 19.5F;
        float rightLegXRot = 0.0F;
        float leftLegXRot = 0.0F;
        float rightLegY = 21.0F;
        float leftLegY = 21.0F;

        if (state.isPerched) {
            rightLegXRot = 0.2F;
            leftLegXRot = 0.2F;
            tailXRot = 0.38F + 0.05F * Mth.sin(state.ageInTicks * 0.1F);
        } else if (state.walkAnimationSpeed > 0.02F && fp < 0.5F) {
            float walkPos = state.walkAnimationPos;
            float walkSpeed = Math.min(state.walkAnimationSpeed, 1.0F);

            rightLegXRot = Mth.cos(walkPos * 1.5F) * 0.7F * walkSpeed;
            leftLegXRot = Mth.cos(walkPos * 1.5F + (float)Math.PI) * 0.7F * walkSpeed;
            tailXRot = 0.3F + Mth.sin(walkPos * 1.5F) * 0.1F * walkSpeed;
        }

        if (fp > 0.001F) {
            float flightBodyXRot = 0.52F;
            float flightBodyY = 17.5F;
            float flightHeadXRot = headPitch * 0.4F - 0.2F;
            float flightHeadY = 16.5F;
            float flightHeadZ = -4.0F;
            float flightTailXRot = 0.25F;
            float flightTailY = 18.0F;
            float flightLegXRot = 1.25F;
            float flightLegY = 19.5F;

            this.body.xRot = Mth.lerp(fp, bodyXRot, flightBodyXRot);
            this.body.y = Mth.lerp(fp, bodyY, flightBodyY);

            this.tail.xRot = Mth.lerp(fp, tailXRot, flightTailXRot);
            this.tail.y = Mth.lerp(fp, tailY, flightTailY);

            this.head.xRot = Mth.lerp(fp, headXRot, flightHeadXRot);
            this.head.y = Mth.lerp(fp, headY, flightHeadY);
            this.head.z = Mth.lerp(fp, headZ, flightHeadZ);
            this.head.yRot = headYaw * (1.0F - fp * 0.4F);

            this.rightLeg.xRot = Mth.lerp(fp, rightLegXRot, flightLegXRot);
            this.leftLeg.xRot = Mth.lerp(fp, leftLegXRot, flightLegXRot);
            this.rightLeg.y = Mth.lerp(fp, rightLegY, flightLegY);
            this.leftLeg.y = Mth.lerp(fp, leftLegY, flightLegY);

            float flap = Mth.sin(state.flapAngle);
            float flapCos = Mth.cos(state.flapAngle);
            float spread = fp * 1.35F;

            this.rightWing.zRot = spread + flap * 0.75F * fp;
            this.leftWing.zRot = -spread - flap * 0.75F * fp;
            this.rightWing.xRot = flapCos * 0.15F * fp;
            this.leftWing.xRot = flapCos * 0.15F * fp;
            this.rightWing.y = Mth.lerp(fp, 16.5F, 16.0F);
            this.leftWing.y = Mth.lerp(fp, 16.5F, 16.0F);

            float wave = flap * 0.25F * fp;
            this.body.y += wave;
            this.head.y += wave;
            this.tail.y += wave;

            float bank = state.bank;
            this.tail.yRot -= bank * 0.40F;
            this.head.zRot -= bank * 0.35F;
        } else {
            this.body.xRot = bodyXRot;
            this.body.y = bodyY;
            this.tail.xRot = tailXRot;
            this.tail.y = tailY;
            this.head.xRot = headXRot;
            this.head.y = headY;
            this.head.z = headZ;
            this.head.yRot = headYaw;
            this.rightLeg.xRot = rightLegXRot;
            this.leftLeg.xRot = leftLegXRot;
            this.rightLeg.y = rightLegY;
            this.leftLeg.y = leftLegY;
            this.rightWing.zRot = 0.0F;
            this.leftWing.zRot = 0.0F;
            this.rightWing.xRot = 0.0F;
            this.leftWing.xRot = 0.0F;
            this.rightWing.y = 16.5F;
            this.leftWing.y = 16.5F;
        }

        if (state.isEating) {
            float eatCycle = Mth.sin(state.ageInTicks * 0.8F);
            this.head.xRot += eatCycle * 0.45F + 0.35F;
            this.head.yRot += Mth.cos(state.ageInTicks * 0.4F) * 0.2F;
        }
    }
}

*/