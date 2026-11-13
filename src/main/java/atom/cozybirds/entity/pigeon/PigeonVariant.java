package atom.cozybirds.entity.pigeon;

import atom.cozybirds.CozyBirds;
import net.minecraft.resources.Identifier;

public enum PigeonVariant {
    CITY(0, "pigeon_city"),
    DARK(1, "pigeon_dark"),
    LIGHT(2, "pigeon_light");

    private final int id;
    private final Identifier texture;

    PigeonVariant(int id, String name) {
        this.id = id;
        this.texture = Identifier.fromNamespaceAndPath(CozyBirds.MOD_ID, "textures/entity/pigeon/" + name + ".png");
    }

    public int getId() {
        return this.id;
    }

    public Identifier getTexture() {
        return this.texture;
    }

    public static PigeonVariant byId(int id) {
        PigeonVariant[] values = values();
        if (id < 0 || id >= values.length) {
            return CITY;
        }
        return values[id];
    }
}
