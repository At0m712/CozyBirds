package atom.cozybirds.client.renderer.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class BirdRenderState extends LivingEntityRenderState {
    public float flapAngle;
    public boolean isFlying;
    public boolean isPerched;
    public float flightProgress = 0.0F;
    public float bank = 0.0F;
    public float pitchTilt = 0.0F;
    public float distanceToCamera = 0.0F;
}
