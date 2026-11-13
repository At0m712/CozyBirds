package atom.cozybirds.client.model;

import atom.cozybirds.CozyBirds;
import atom.cozybirds.client.renderer.state.PigeonRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Ultra-detailed 256x256 Rock Dove / City Pigeon (Columba livia) model.
 * Built with rich anatomical proportions (large HD model) that scales down
 * in the renderer to realistic IRL size with razor-sharp plumage fidelity.
 */
public class PigeonModel extends EntityModel<PigeonRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(CozyBirds.MOD_ID, "pigeon"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightWingFolded;
    private final ModelPart leftWingFolded;
    private final ModelPart rightWingFlying;
    private final ModelPart leftWingFlying;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart tail;

    public PigeonModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightWingFolded = root.getChild("rightWingFolded");
        this.leftWingFolded = root.getChild("leftWingFolded");
        this.rightWingFlying = root.getChild("rightWingFlying");
        this.leftWingFlying = root.getChild("leftWingFlying");
        this.rightLeg = root.getChild("rightLeg");
        this.leftLeg = root.getChild("leftLeg");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 1. Torso: Plump, muscular columbid body (18 x 18 x 24) with deep rounded keel / breast
        partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-9.0F, -9.0F, -12.0F, 18.0F, 18.0F, 24.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        // 2. Head & Beak with Cere: Rounded head with bill and distinct white cere (operculum)
        partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(90, 0)
                        .addBox(-6.0F, -11.0F, -10.0F, 12.0F, 14.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(150, 0)
                        .addBox(-2.0F, -4.5F, -18.0F, 4.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 7.0F, -10.0F));

        // 3. Folded Wings: Folded neatly along flanks when on ground
        partdefinition.addOrReplaceChild("rightWingFolded",
                CubeListBuilder.create().texOffs(90, 32)
                        .addBox(-3.0F, 0.0F, -3.0F, 3.0F, 16.0F, 22.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-9.0F, 7.5F, -4.0F));

        partdefinition.addOrReplaceChild("leftWingFolded",
                CubeListBuilder.create().texOffs(145, 32)
                        .addBox(0.0F, 0.0F, -3.0F, 3.0F, 16.0F, 22.0F, new CubeDeformation(0.0F)),
                PartPose.offset(9.0F, 7.5F, -4.0F));

        // 4. Flying Wings: Wide outstretched wings (40 units span per wing = 98 units wingspan total)
        // PIVOT POINT: Located EXACTLY at the shoulder-body flank junction (-9.0 and +9.0)
        // Box starts at X = 0.0F (shoulder), rotating smoothly without ever penetrating the body!
        partdefinition.addOrReplaceChild("rightWingFlying",
                CubeListBuilder.create().texOffs(0, 85)
                        .addBox(-40.0F, -1.25F, -5.0F, 40.0F, 2.5F, 20.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-9.0F, 8.0F, -3.0F));

        partdefinition.addOrReplaceChild("leftWingFlying",
                CubeListBuilder.create().texOffs(125, 85)
                        .addBox(0.0F, -1.25F, -5.0F, 40.0F, 2.5F, 20.0F, new CubeDeformation(0.0F)),
                PartPose.offset(9.0F, 8.0F, -3.0F));

        // 5. Fan Tail: Broad columbid fan-tail with dark terminal band
        partdefinition.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(-7.0F, -1.5F, 0.0F, 14.0F, 3.0F, 28.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 10.5F, 11.5F, 0.38F, 0.0F, 0.0F));

        // 6. Right Leg & Beautiful Foot ("belles patounes")
        partdefinition.addOrReplaceChild("rightLeg",
                CubeListBuilder.create()
                        // Tarsus / Shin (tige de la patte)
                        .texOffs(0, 115).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.5F, 2.0F, new CubeDeformation(0.0F))
                        // Central foot pad
                        .texOffs(0, 123).addBox(-1.5F, 4.5F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        // Middle front toe & claw
                        .texOffs(0, 128).addBox(-0.75F, 4.5F, -5.0F, 1.5F, 1.0F, 3.5F, new CubeDeformation(0.0F))
                        // Outer front toe (left)
                        .texOffs(11, 128).addBox(-2.6F, 4.5F, -4.2F, 1.2F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        // Inner front toe (right)
                        .texOffs(21, 128).addBox(1.4F, 4.5F, -4.2F, 1.2F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        // Hind toe (pouce arrière)
                        .texOffs(0, 134).addBox(-0.75F, 4.5F, 1.5F, 1.5F, 1.0F, 2.5F, new CubeDeformation(0.0F)),
                PartPose.offset(-4.5F, 18.5F, 1.0F));

        // 7. Left Leg & Beautiful Foot
        partdefinition.addOrReplaceChild("leftLeg",
                CubeListBuilder.create()
                        // Tarsus / Shin
                        .texOffs(35, 115).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.5F, 2.0F, new CubeDeformation(0.0F))
                        // Central foot pad
                        .texOffs(35, 123).addBox(-1.5F, 4.5F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        // Middle front toe & claw
                        .texOffs(35, 128).addBox(-0.75F, 4.5F, -5.0F, 1.5F, 1.0F, 3.5F, new CubeDeformation(0.0F))
                        // Outer front toe
                        .texOffs(46, 128).addBox(-2.6F, 4.5F, -4.2F, 1.2F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        // Inner front toe
                        .texOffs(56, 128).addBox(1.4F, 4.5F, -4.2F, 1.2F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        // Hind toe
                        .texOffs(35, 134).addBox(-0.75F, 4.5F, 1.5F, 1.5F, 1.0F, 2.5F, new CubeDeformation(0.0F)),
                PartPose.offset(4.5F, 18.5F, 1.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(PigeonRenderState state) {
        this.resetPose();

        float fp = state.flightProgress;
        float ground = 1.0F - fp;
        float headYaw = state.yRot * ((float)Math.PI / 180F);
        float headPitch = state.xRot * ((float)Math.PI / 180F);

        // Head tracking
        this.head.yRot = headYaw * (1.0F - fp * 0.4F);
        this.head.xRot = headPitch * 0.5F;

        // Ground walking with signature pigeon head-bobbing!
        float walkLegR = 0.0F;
        float walkLegL = 0.0F;
        if (state.walkAnimationSpeed > 0.02F && ground > 0.5F) {
            float walkPos = state.walkAnimationPos;
            float walkSpeed = Math.min(state.walkAnimationSpeed, 1.0F);

            walkLegR = Mth.cos(walkPos * 1.8F) * 0.70F * walkSpeed;
            walkLegL = Mth.cos(walkPos * 1.8F + (float)Math.PI) * 0.70F * walkSpeed;

            // Signature pigeon head-bobbing: head thrusts forward rhythmically with each step!
            float headBob = Mth.sin(walkPos * 1.8F - 0.5F) * walkSpeed;
            this.head.z += headBob * 3.0F;
            this.head.xRot += headBob * 0.15F;

            // Subtle body waddle and tail balance
            this.body.zRot = Mth.sin(walkPos * 0.9F) * 0.05F * walkSpeed;
            this.tail.yRot = Mth.sin(walkPos * 1.8F) * 0.12F * walkSpeed;
        }

        // Ground Idle Behaviors
        if (ground > 0.5F) {
            if (state.isPecking) {
                applyPeckAnimation(state.ageInTicks);
            } else if (state.isCooing) {
                applyCooAnimation(state.ageInTicks);
            } else if (state.isPreening) {
                applyPreenAnimation(state.ageInTicks);
            }
        }

        // Perched posture
        if (state.isPerched && ground > 0.5F) {
            walkLegR = 0.20F;
            walkLegL = 0.20F;
            this.tail.xRot = 0.42F + 0.05F * Mth.sin(state.ageInTicks * 0.10F);
            this.body.xRot = 0.06F;
        }

        // Legs & Feet ("belles patounes"): always visible!
        // On ground: resting flat at Y=24.0 or walking/perching. In flight: tucked smoothly against belly plumage.
        this.rightLeg.visible = true;
        this.leftLeg.visible = true;
        this.rightLeg.xRot = Mth.lerp(fp, walkLegR, 0.85F);
        this.leftLeg.xRot = Mth.lerp(fp, walkLegL, 0.85F);
        this.rightLeg.y = Mth.lerp(fp, 18.5F, 17.5F);
        this.leftLeg.y = Mth.lerp(fp, 18.5F, 17.5F);
        this.rightLeg.z = Mth.lerp(fp, 1.0F, 2.5F);
        this.leftLeg.z = Mth.lerp(fp, 1.0F, 2.5F);

        // FLIGHT VS GROUND POSTURE
        boolean flyingState = state.isFlying && !state.isPerched && fp > 0.05F;
        if (flyingState) {
            // Flight: outstretch wide wings, hide folded wings
            this.rightWingFolded.visible = false;
            this.leftWingFolded.visible = false;
            this.rightWingFlying.visible = true;
            this.leftWingFlying.visible = true;

            // Fuselage attitude: body leveled forward
            this.body.xRot = Mth.lerp(fp, 0.0F, 0.10F);

            // Tail aligned horizontally into slipstream
            this.tail.xRot = Mth.lerp(fp, 0.38F, 0.05F);

            // Vigorous pigeon wing flapping (5.5 Hz)
            float flap = Mth.sin(state.flapAngle);
            float flapCos = Mth.cos(state.flapAngle);

            this.rightWingFlying.zRot = (flap * 0.55F - 0.03F) * fp;
            this.leftWingFlying.zRot = (-flap * 0.55F + 0.03F) * fp;

            this.rightWingFlying.xRot = flapCos * 0.18F * fp;
            this.leftWingFlying.xRot = flapCos * 0.18F * fp;

            // Aerodynamic flight bobbing wave
            float wave = flap * 0.50F * fp;
            this.body.y += wave;
            this.head.y += wave;
            this.tail.y += wave;

            // Banking in turns
            float bank = state.bank;
            this.body.zRot += bank * 0.8F;
            this.head.zRot += bank * 0.4F;
            this.tail.zRot -= bank * 0.4F;
            this.rightWingFlying.zRot += bank * 0.5F;
            this.leftWingFlying.zRot += bank * 0.5F;
        } else {
            // Ground: Folded wings tucked neatly along flanks
            this.rightWingFolded.visible = true;
            this.leftWingFolded.visible = true;
            this.rightWingFlying.visible = false;
            this.leftWingFlying.visible = false;

            this.rightWingFolded.zRot = 0.0F;
            this.leftWingFolded.zRot = 0.0F;
            this.rightWingFolded.xRot = 0.10F;
            this.leftWingFolded.xRot = 0.10F;
        }
    }

    private void applyPeckAnimation(float ageInTicks) {
        float peckCycle = Mth.sin(ageInTicks * 1.2F);
        this.head.xRot += 0.75F + peckCycle * 0.25F;
        this.head.y += 3.5F + peckCycle * 0.8F;
        this.head.z -= 2.0F;
        this.body.xRot += 0.12F + peckCycle * 0.04F;
    }

    private void applyCooAnimation(float ageInTicks) {
        // Pigeon puffs chest out and tilts head while cooing!
        float coo = (Mth.sin(ageInTicks * 0.35F) + 1.0F) * 0.5F;
        this.body.z -= coo * 1.2F;
        this.body.xRot -= 0.12F - coo * 0.08F;
        this.head.z += coo * 0.8F;
        this.head.xRot -= 0.15F - coo * 0.20F;
        this.tail.xRot += 0.10F - coo * 0.12F;
    }

    private void applyPreenAnimation(float ageInTicks) {
        // Turns head to groom wing feathers
        float preenSpeed = Mth.sin(ageInTicks * 0.8F);
        this.head.yRot += 1.35F;
        this.head.xRot += 0.30F + preenSpeed * 0.10F;
        this.head.z += 1.0F;
        this.rightWingFolded.zRot += 0.18F;
    }
}
