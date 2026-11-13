package atom.cozybirds.client.model;

import atom.cozybirds.CozyBirds;
import atom.cozybirds.client.renderer.state.MagpieRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * High-definition 256x256 Eurasian Magpie (Pica pica) model.
 * Built with rich anatomical proportions (large HD model) that scales down
 * in the renderer to realistic IRL size with razor-sharp plumage fidelity.
 */
public class MagpieModel extends EntityModel<MagpieRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(CozyBirds.MOD_ID, "magpie"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightWingFolded;
    private final ModelPart leftWingFolded;
    private final ModelPart rightWingFlying;
    private final ModelPart leftWingFlying;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart tail;

    public MagpieModel(ModelPart root) {
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

        // 1. Torso: Aerodynamic high-definition fuselage (10 x 10 x 14)
        partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-5.0F, -5.0F, -7.0F, 10.0F, 10.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        // 2. Head & Beak: Slender corvid head with tapered sharp beak
        partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(56, 0)
                        .addBox(-3.5F, -6.0F, -5.0F, 7.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(90, 0)
                        .addBox(-1.5F, -3.0F, -12.0F, 3.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 7.5F, -6.0F));

        // 3. Folded Wings: Folded neatly along flanks when on ground
        partdefinition.addOrReplaceChild("rightWingFolded",
                CubeListBuilder.create().texOffs(70, 30)
                        .addBox(-1.5F, 0.0F, -2.0F, 1.5F, 9.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 8.0F, -3.0F));

        partdefinition.addOrReplaceChild("leftWingFolded",
                CubeListBuilder.create().texOffs(102, 30)
                        .addBox(0.0F, 0.0F, -2.0F, 1.5F, 9.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offset(5.0F, 8.0F, -3.0F));

        // 4. Flying Wings: Wide outstretched wings (22 units span per wing = 54 units wingspan total)
        // Leading edge at Z = -3.0F, trailing edge at Z = 8.0F (strictly within body, never overflows to tail)
        partdefinition.addOrReplaceChild("rightWingFlying",
                CubeListBuilder.create().texOffs(0, 65)
                        .addBox(-22.0F, -0.75F, -3.0F, 22.0F, 1.5F, 11.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 8.0F, -2.0F));

        partdefinition.addOrReplaceChild("leftWingFlying",
                CubeListBuilder.create().texOffs(70, 65)
                        .addBox(0.0F, -0.75F, -3.0F, 22.0F, 1.5F, 11.0F, new CubeDeformation(0.0F)),
                PartPose.offset(5.0F, 8.0F, -2.0F));

        // 5. The ONLY Tail: Attached cleanly to rear of body (never overlaps with wings)
        partdefinition.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 30)
                        .addBox(-3.0F, -1.0F, 0.0F, 6.0F, 2.0F, 26.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 10.5F, 6.5F, 0.35F, 0.0F, 0.0F));

        // 6. Right Leg (2x8x2 leg + 3x1x5 foot) - Ground feet reach Y = 24.0F!
        partdefinition.addOrReplaceChild("rightLeg",
                CubeListBuilder.create().texOffs(0, 90)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 102)
                        .addBox(-1.5F, 7.2F, -4.0F, 3.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.5F, 16.0F, 0.0F));

        // 7. Left Leg
        partdefinition.addOrReplaceChild("leftLeg",
                CubeListBuilder.create().texOffs(20, 90)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(20, 102)
                        .addBox(-1.5F, 7.2F, -4.0F, 3.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.5F, 16.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(MagpieRenderState state) {
        this.resetPose();

        float fp = state.flightProgress;
        float ground = 1.0F - fp;
        float headYaw = state.yRot * ((float)Math.PI / 180F);
        float headPitch = state.xRot * ((float)Math.PI / 180F);

        // Head tracking
        this.head.yRot = headYaw * (1.0F - fp * 0.4F);
        this.head.xRot = headPitch * 0.5F;

        // Walking / Hopping ground gait
        float walkLegR = 0.0F;
        float walkLegL = 0.0F;
        if (state.walkAnimationSpeed > 0.02F && ground > 0.5F) {
            float walkPos = state.walkAnimationPos;
            float walkSpeed = Math.min(state.walkAnimationSpeed, 1.0F);

            walkLegR = Mth.cos(walkPos * 1.6F) * 0.70F * walkSpeed;
            walkLegL = Mth.cos(walkPos * 1.6F + (float)Math.PI) * 0.70F * walkSpeed;

            // Tail bobbing and balance
            this.tail.yRot = Mth.sin(walkPos * 1.6F) * 0.20F * walkSpeed;
            this.tail.xRot += Mth.cos(walkPos * 3.2F) * 0.08F * walkSpeed;
        }

        // Perched posture
        if (state.isPerched && ground > 0.5F) {
            walkLegR = 0.20F;
            walkLegL = 0.20F;
            this.tail.xRot = 0.45F + 0.05F * Mth.sin(state.ageInTicks * 0.12F);
            this.body.xRot = 0.08F;
        }

        // FLIGHT VS GROUND POSTURE
        if (fp > 0.05F) {
            // Flight: outstretch wide wings, hide folded wings
            this.rightWingFolded.visible = false;
            this.leftWingFolded.visible = false;
            this.rightWingFlying.visible = true;
            this.leftWingFlying.visible = true;

            // Fuselage aerodynamic attitude: body leveled forward
            this.body.xRot = Mth.lerp(fp, 0.0F, 0.12F);

            // Legs retracted into plumage during flight (clean aerodynamic silhouette, no dangling legs!)
            boolean showLegs = fp < 0.35F;
            this.rightLeg.visible = showLegs;
            this.leftLeg.visible = showLegs;
            if (showLegs) {
                this.rightLeg.xRot = Mth.lerp(fp, walkLegR, 1.45F);
                this.leftLeg.xRot = Mth.lerp(fp, walkLegL, 1.45F);
                this.rightLeg.y = Mth.lerp(fp, 16.0F, 14.5F);
                this.leftLeg.y = Mth.lerp(fp, 16.0F, 14.5F);
            }

            // Single tail aligned horizontally into slipstream
            this.tail.xRot = Mth.lerp(fp, 0.35F, 0.05F);
            this.tail.xRot += Mth.sin(state.flapAngle * 0.5F) * 0.06F * fp;

            // Organic flap-bounding: birds alternate between vigorous wingbeats and locked-wing glides
            // Each bird has a completely independent cycle length and phase based on state.entityId
            int id = Math.abs(state.entityId);
            int cycleLength = 40 + ((id * 7) % 20); // 40 to 60 ticks per flap/glide cycle
            int phaseOffset = ((id * 19) % cycleLength);
            int currentTick = (int)(state.ageInTicks + phaseOffset) % cycleLength;
            if (currentTick < 0) currentTick += cycleLength;

            // Staggered gliding: ~14 ticks locked in glide, rest flapping
            boolean isGliding = currentTick < 14;
            float flapAmp = isGliding ? 0.05F : 0.55F;

            // Wings flapping rhythmically up and down
            float flap = Mth.sin(state.flapAngle);
            float flapCos = Mth.cos(state.flapAngle);

            this.rightWingFlying.zRot = (flap * flapAmp - 0.04F) * fp;
            this.leftWingFlying.zRot = (-flap * flapAmp + 0.04F) * fp;

            this.rightWingFlying.xRot = flapCos * flapAmp * 0.22F * fp;
            this.leftWingFlying.xRot = flapCos * flapAmp * 0.22F * fp;

            // Aerodynamic lift wave
            float wave = flap * flapAmp * 0.45F * fp;
            this.body.y += wave;
            this.head.y += wave;
            this.tail.y += wave;

            // Banking in murmuration turns
            float bank = state.bank;
            this.body.zRot += bank * 0.8F;
            this.head.zRot += bank * 0.4F;
            this.tail.zRot -= bank * 0.4F;
            this.rightWingFlying.zRot += bank * 0.5F;
            this.leftWingFlying.zRot += bank * 0.5F;
        } else {
            // Ground / Perched: Folded wings neatly tucked along flanks, hide flying wings
            this.rightWingFolded.visible = true;
            this.leftWingFolded.visible = true;
            this.rightWingFlying.visible = false;
            this.leftWingFlying.visible = false;
            this.rightLeg.visible = true;
            this.leftLeg.visible = true;

            this.rightWingFolded.zRot = 0.0F;
            this.leftWingFolded.zRot = 0.0F;
            this.rightWingFolded.xRot = 0.12F;
            this.leftWingFolded.xRot = 0.12F;

            this.rightLeg.xRot = walkLegR;
            this.leftLeg.xRot = walkLegL;
        }
    }
}
