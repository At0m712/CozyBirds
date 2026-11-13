/* V2_DISABLED
package atom.cozybirds.client.model;

import atom.cozybirds.CozyBirds;
import atom.cozybirds.client.renderer.state.GoldenEagleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Magnificent anatomical Golden Eagle (Aquila chrysaetos) model.
 * Features broad fingered soaring wings, golden nape crest, hooked raptor beak with yellow cere,
 * wedge tail, feathered tarsi down to feet, and powerful talons.
 * /
public class GoldenEagleModel extends EntityModel<GoldenEagleRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(CozyBirds.MOD_ID, "golden_eagle"), "main");

    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart tail;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart goldenNape;
    private final ModelPart beakBase;
    private final ModelPart beakHook;
    private final ModelPart rightWingBase;
    private final ModelPart rightWingMid;
    private final ModelPart rightWingTip;
    private final ModelPart leftWingBase;
    private final ModelPart leftWingMid;
    private final ModelPart leftWingTip;
    private final ModelPart rightThigh;
    private final ModelPart rightTarsus;
    private final ModelPart rightTalons;
    private final ModelPart leftThigh;
    private final ModelPart leftTarsus;
    private final ModelPart leftTalons;

    public GoldenEagleModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.chest = this.body.getChild("chest");
        this.tail = this.body.getChild("tail");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.goldenNape = this.head.getChild("golden_nape");
        this.beakBase = this.head.getChild("beak_base");
        this.beakHook = this.beakBase.getChild("beak_hook");

        this.rightWingBase = root.getChild("right_wing_base");
        this.rightWingMid = this.rightWingBase.getChild("right_wing_mid");
        this.rightWingTip = this.rightWingMid.getChild("right_wing_tip");

        this.leftWingBase = root.getChild("left_wing_base");
        this.leftWingMid = this.leftWingBase.getChild("left_wing_mid");
        this.leftWingTip = this.leftWingMid.getChild("left_wing_tip");

        this.rightThigh = root.getChild("right_thigh");
        this.rightTarsus = this.rightThigh.getChild("right_tarsus");
        this.rightTalons = this.rightTarsus.getChild("right_talons");

        this.leftThigh = root.getChild("left_thigh");
        this.leftTarsus = this.leftThigh.getChild("left_tarsus");
        this.leftTalons = this.leftTarsus.getChild("left_talons");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 1. Torso: Powerful muscular raptor body (6x7x8)
        PartDefinition body = partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.0F, -3.5F, -4.0F, 6.0F, 7.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 13.0F, 0.0F));

        // 2. Chest: Muscular keel breast (5x6x4)
        body.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(28, 0)
                        .addBox(-2.5F, -3.0F, -3.5F, 5.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.5F, -3.0F));

        // 3. Wedge Tail: Broad rudder fan tail (6x1x8)
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(-3.0F, -0.5F, 0.0F, 6.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 3.8F, 0.20F, 0.0F, 0.0F));

        // 4. Neck: Robust muscular raptor neck (3.5x4.5x4)
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-1.75F, -4.0F, -2.0F, 3.5F, 4.5F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -1.5F, -4.0F));

        // 5. Head: Broad skull with prominent brow ridge (4x4x5)
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-2.0F, -3.5F, -3.0F, 4.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -3.8F, 0.5F));

        // 6. Golden Nape: Radiant golden crown / nape hackles (4x3x2)
        head.addOrReplaceChild("golden_nape",
                CubeListBuilder.create().texOffs(36, 16)
                        .addBox(-2.0F, -3.8F, 0.5F, 4.0F, 3.5F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // 7. Beak Base: Bright yellow cere (2.6x2.5x2)
        PartDefinition beakBase = head.addOrReplaceChild("beak_base",
                CubeListBuilder.create().texOffs(48, 16)
                        .addBox(-1.3F, -1.5F, -2.0F, 2.6F, 2.5F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, -3.0F));

        // 8. Beak Hook: Formidable sharp curved black raptor hook (2.2x3x2.5)
        beakBase.addOrReplaceChild("beak_hook",
                CubeListBuilder.create().texOffs(56, 16)
                        .addBox(-1.1F, -0.2F, -2.2F, 2.2F, 3.0F, 2.5F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.5F, -0.5F, 0.35F, 0.0F, 0.0F));

        // 9. Right Wing Base: Strong shoulder humerus (1.5x6x7)
        PartDefinition rightWingBase = partdefinition.addOrReplaceChild("right_wing_base",
                CubeListBuilder.create().texOffs(0, 26)
                        .addBox(-1.5F, 0.0F, -3.5F, 1.5F, 6.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.0F, 11.5F, 0.0F));

        // 10. Right Wing Mid: Broad forearm secondaries (1x5x8)
        PartDefinition rightWingMid = rightWingBase.addOrReplaceChild("right_wing_mid",
                CubeListBuilder.create().texOffs(18, 26)
                        .addBox(-1.0F, 0.2F, -2.5F, 1.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.5F, 3.5F));

        // 11. Right Wing Tip: Fingered spread primary flight feathers (1x4x9)
        rightWingMid.addOrReplaceChild("right_wing_tip",
                CubeListBuilder.create().texOffs(37, 26)
                        .addBox(-0.9F, 0.2F, 0.0F, 1.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.3F, 5.0F));

        // 12. Left Wing Base (1.5x6x7)
        PartDefinition leftWingBase = partdefinition.addOrReplaceChild("left_wing_base",
                CubeListBuilder.create().texOffs(0, 39)
                        .addBox(0.0F, 0.0F, -3.5F, 1.5F, 6.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(3.0F, 11.5F, 0.0F));

        // 13. Left Wing Mid (1x5x8)
        PartDefinition leftWingMid = leftWingBase.addOrReplaceChild("left_wing_mid",
                CubeListBuilder.create().texOffs(18, 39)
                        .addBox(0.0F, 0.2F, -2.5F, 1.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.5F, 3.5F));

        // 14. Left Wing Tip (1x4x9)
        leftWingMid.addOrReplaceChild("left_wing_tip",
                CubeListBuilder.create().texOffs(37, 39)
                        .addBox(-0.1F, 0.2F, 0.0F, 1.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.3F, 5.0F));

        // 15. Right Thigh: Muscular feathered thigh (2.5x3.5x3)
        PartDefinition rightThigh = partdefinition.addOrReplaceChild("right_thigh",
                CubeListBuilder.create().texOffs(20, 48)
                        .addBox(-1.25F, 0.0F, -1.5F, 2.5F, 3.5F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 15.5F, 0.5F));

        // 16. Right Tarsus: Fully feathered leg down to feet (true Aquila trait) (2x4x2)
        PartDefinition rightTarsus = rightThigh.addOrReplaceChild("right_tarsus",
                CubeListBuilder.create().texOffs(20, 52)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 3.2F, 0.0F));

        // 17. Right Talons: Powerful golden talons with sharp black curved hallux (3x1.5x4)
        rightTarsus.addOrReplaceChild("right_talons",
                CubeListBuilder.create().texOffs(26, 48)
                        .addBox(-1.5F, 0.0F, -3.0F, 3.0F, 1.5F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 3.8F, 0.0F));

        // 18. Left Thigh (2.5x3.5x3)
        PartDefinition leftThigh = partdefinition.addOrReplaceChild("left_thigh",
                CubeListBuilder.create().texOffs(32, 48)
                        .addBox(-1.25F, 0.0F, -1.5F, 2.5F, 3.5F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 15.5F, 0.5F));

        // 19. Left Tarsus (2x4x2)
        PartDefinition leftTarsus = leftThigh.addOrReplaceChild("left_tarsus",
                CubeListBuilder.create().texOffs(32, 52)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 3.2F, 0.0F));

        // 20. Left Talons (3x1.5x4)
        leftTarsus.addOrReplaceChild("left_talons",
                CubeListBuilder.create().texOffs(38, 48)
                        .addBox(-1.5F, 0.0F, -3.0F, 3.0F, 1.5F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 3.8F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(GoldenEagleRenderState state) {
        this.resetPose();

        float fp = state.flightProgress;
        float ground = 1.0F - fp;
        float headYaw = state.yRot * ((float)Math.PI / 180F);
        float headPitch = state.xRot * ((float)Math.PI / 180F);

        // 1. Proud Raptor Head Posture
        this.neck.xRot = 0.15F;
        this.head.xRot = -0.15F + headPitch * 0.50F;
        this.head.yRot = headYaw * 0.65F;

        // 2. Walking / Ground Locomotion
        if (state.walkAnimationSpeed > 0.02F && fp < 0.5F) {
            float walkPos = state.walkAnimationPos;
            float walkSpeed = Math.min(state.walkAnimationSpeed, 1.0F);

            this.rightThigh.xRot = Mth.cos(walkPos * 0.85F) * 0.65F * walkSpeed * ground;
            this.leftThigh.xRot = Mth.cos(walkPos * 0.85F + (float)Math.PI) * 0.65F * walkSpeed * ground;

            // Alerte predatory walk gait
            this.body.zRot = Mth.sin(walkPos * 0.85F) * 0.05F * walkSpeed * ground;
            this.head.xRot += Mth.sin(walkPos * 1.7F) * 0.08F * walkSpeed * ground;
        }

        // 3. Perched Posture
        if (state.isPerched && ground > 0.5F) {
            this.body.xRot = 0.28F;
            this.neck.xRot = -0.15F;
            this.head.xRot = -0.12F + headPitch * 0.40F;
            this.tail.xRot = 0.55F;
            this.rightThigh.xRot = -0.25F;
            this.leftThigh.xRot = -0.25F;
            this.rightTarsus.xRot = 0.45F;
            this.leftTarsus.xRot = 0.45F;
        }

        // 4. SCREECH / INTIMIDATION DISPLAY
        float screechP = state.screechProgress;
        if (screechP > 0.001F) {
            this.neck.xRot = Mth.lerp(screechP, this.neck.xRot, -0.30F);
            this.head.xRot = Mth.lerp(screechP, this.head.xRot, -0.45F);
            this.beakHook.xRot += 0.35F * screechP; // Open hooked beak to screech
            this.rightWingBase.zRot += 0.40F * screechP;
            this.leftWingBase.zRot -= 0.40F * screechP;
        }

        // 5. HUNTING DIVE (HUNTING STOOP)
        float diveP = state.diveProgress * fp;
        if (diveP > 0.001F) {
            // Body tilted steeply downwards
            this.body.xRot = Mth.lerp(diveP, this.body.xRot, 1.15F);
            this.head.xRot = Mth.lerp(diveP, this.head.xRot, -0.80F);

            // Ailes repliées en delta profilé à grande vitesse
            this.rightWingBase.zRot = Mth.lerp(diveP, this.rightWingBase.zRot, 0.40F);
            this.leftWingBase.zRot = Mth.lerp(diveP, this.leftWingBase.zRot, -0.40F);
            this.rightWingMid.zRot = Mth.lerp(diveP, this.rightWingMid.zRot, 0.25F);
            this.leftWingMid.zRot = Mth.lerp(diveP, this.leftWingMid.zRot, -0.25F);
            this.rightWingTip.zRot = Mth.lerp(diveP, this.rightWingTip.zRot, 0.15F);
            this.leftWingTip.zRot = Mth.lerp(diveP, this.leftWingTip.zRot, -0.15F);

            // Serres prêtes à frapper
            this.rightThigh.xRot = Mth.lerp(diveP, this.rightThigh.xRot, 0.95F);
            this.leftThigh.xRot = Mth.lerp(diveP, this.leftThigh.xRot, 0.95F);
            this.tail.xRot = Mth.lerp(diveP, this.tail.xRot, -0.20F);
        }

        // 6. SOARING / FLIGHT ANIMATIONS
        if (fp > 0.001F && diveP < 0.5F) {
            this.body.xRot = Mth.lerp(fp, this.body.xRot, 0.12F);
            this.tail.xRot = Mth.lerp(fp, this.tail.xRot, 0.05F);

            // Legs tucked back streamlined against undertail coverts
            this.rightThigh.xRot = Mth.lerp(fp, this.rightThigh.xRot, 1.45F);
            this.leftThigh.xRot = Mth.lerp(fp, this.leftThigh.xRot, 1.45F);
            this.rightTarsus.xRot = Mth.lerp(fp, this.rightTarsus.xRot, 0.20F);
            this.leftTarsus.xRot = Mth.lerp(fp, this.leftTarsus.xRot, 0.20F);

            float soarP = state.soarProgress;
            float flap = Mth.sin(state.flapAngle);
            float flapCos = Mth.cos(state.flapAngle);

            if (soarP > 0.5F) {
                // MAJESTIC SOARING: Wings held in a slight V-dihedral, primaires curved upward
                float dihedral = 1.35F + Mth.sin(state.ageInTicks * 0.08F) * 0.03F;
                this.rightWingBase.zRot = dihedral;
                this.leftWingBase.zRot = -dihedral;
                this.rightWingMid.zRot = 0.12F;
                this.leftWingMid.zRot = -0.12F;

                // Fingered primaries raised by thermal updraft
                this.rightWingTip.zRot = 0.22F;
                this.leftWingTip.zRot = -0.22F;

                // Tail spread wide as an aerial rudder
                this.tail.yRot = -state.bank * 0.40F;
            } else {
                // FLAPPING FLIGHT: Slow, rhythmic, powerful raptor wingbeats (~1.6 Hz)
                float spread = fp * 1.35F;

                this.rightWingBase.zRot = spread + flap * 0.55F * fp;
                this.leftWingBase.zRot = -spread - flap * 0.55F * fp;
                this.rightWingBase.xRot = flapCos * 0.08F * fp;
                this.leftWingBase.xRot = flapCos * 0.08F * fp;

                this.rightWingMid.zRot = flap * 0.30F * fp;
                this.leftWingMid.zRot = -flap * 0.30F * fp;
                this.rightWingMid.xRot = flapCos * 0.10F * fp;
                this.leftWingMid.xRot = flapCos * 0.10F * fp;

                this.rightWingTip.zRot = flap * 0.38F * fp;
                this.leftWingTip.zRot = -flap * 0.38F * fp;
                this.rightWingTip.xRot = flapCos * 0.14F * fp;
                this.leftWingTip.xRot = flapCos * 0.14F * fp;

                this.body.y += flap * 0.14F * fp;
            }

            // Banking into turns
            float bank = state.bank;
            this.head.zRot -= bank * 0.25F;
            this.tail.zRot -= bank * 0.30F;
        } else if (fp <= 0.001F) {
            // Folded wings along flanks
            this.rightWingBase.zRot = 0.0F;
            this.leftWingBase.zRot = 0.0F;
            this.rightWingMid.zRot = 0.0F;
            this.leftWingMid.zRot = 0.0F;
            this.rightWingTip.zRot = 0.0F;
            this.leftWingTip.zRot = 0.0F;
        }
    }
}

*/