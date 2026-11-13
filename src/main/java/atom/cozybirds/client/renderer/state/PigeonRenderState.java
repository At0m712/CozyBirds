package atom.cozybirds.client.renderer.state;

import atom.cozybirds.entity.pigeon.PigeonVariant;

public class PigeonRenderState extends BirdRenderState {
    public PigeonVariant variant = PigeonVariant.CITY;
    public int action = 0;
    public boolean isPecking = false;
    public boolean isCooing = false;
    public boolean isPreening = false;
}
