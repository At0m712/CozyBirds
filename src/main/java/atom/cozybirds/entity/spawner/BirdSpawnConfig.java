package atom.cozybirds.entity.spawner;

public class BirdSpawnConfig {
    public static boolean PIGEON_SPAWN_ENABLED = true;
    public static boolean MAGPIE_SPAWN_ENABLED = true;

    public static boolean isSpawnEnabled(String species) {
        String s = species.toLowerCase();
        if (s.equals("pigeon") || s.equals("pigeons")) return PIGEON_SPAWN_ENABLED;
        if (s.equals("magpie") || s.equals("magpies") || s.equals("pie") || s.equals("pies")) return MAGPIE_SPAWN_ENABLED;
        return true;
    }

    public static boolean setSpawnEnabled(String species, boolean enabled) {
        String s = species.toLowerCase();
        if (s.equals("pigeon") || s.equals("pigeons")) {
            PIGEON_SPAWN_ENABLED = enabled;
            return true;
        }
        if (s.equals("magpie") || s.equals("magpies") || s.equals("pie") || s.equals("pies")) {
            MAGPIE_SPAWN_ENABLED = enabled;
            return true;
        }
        return false;
    }

    public static Boolean toggleSpawn(String species) {
        String s = species.toLowerCase();
        if (s.equals("pigeon") || s.equals("pigeons")) {
            PIGEON_SPAWN_ENABLED = !PIGEON_SPAWN_ENABLED;
            return PIGEON_SPAWN_ENABLED;
        }
        if (s.equals("magpie") || s.equals("magpies") || s.equals("pie") || s.equals("pies")) {
            MAGPIE_SPAWN_ENABLED = !MAGPIE_SPAWN_ENABLED;
            return MAGPIE_SPAWN_ENABLED;
        }
        return null;
    }
}
