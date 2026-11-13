package atom.cozybirds.registry;

import atom.cozybirds.CozyBirds;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.function.Function;

public class ModItems {
    public static final Item PIGEON_FEATHER = registerItem("pigeon_feather", Item::new);
    public static final Item MAGPIE_FEATHER = registerItem("magpie_feather", Item::new);
    public static final Item FALCON_FEATHER = registerItem("falcon_feather", Item::new);
    public static final Item FLAMINGO_FEATHER = registerItem("flamingo_feather", Item::new);
    public static final Item GOLDEN_EAGLE_FEATHER = registerItem("golden_eagle_feather", Item::new);

    public static final Item PIGEON_SPAWN_EGG = registerItem("pigeon_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(ModEntities.PIGEON)));
    public static final Item MAGPIE_SPAWN_EGG = registerItem("magpie_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(ModEntities.MAGPIE)));
    /* V2 birds: commented for V1
    public static final Item FALCON_SPAWN_EGG = registerItem("falcon_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(ModEntities.FALCON)));
    public static final Item FLAMINGO_SPAWN_EGG = registerItem("flamingo_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(ModEntities.FLAMINGO)));
    public static final Item GOLDEN_EAGLE_SPAWN_EGG = registerItem("golden_eagle_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(ModEntities.GOLDEN_EAGLE)));
    */

    public static <T extends Item> T registerItem(String name, Function<Item.Properties, T> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CozyBirds.MOD_ID, name));
        T item = factory.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void init() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
            output.accept(PIGEON_FEATHER);
            output.accept(MAGPIE_FEATHER);
            /*
            output.accept(FALCON_FEATHER);
            output.accept(FLAMINGO_FEATHER);
            output.accept(GOLDEN_EAGLE_FEATHER);
            */
        });

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> {
            output.accept(PIGEON_SPAWN_EGG);
            output.accept(MAGPIE_SPAWN_EGG);
            /*
            output.accept(FALCON_SPAWN_EGG);
            output.accept(FLAMINGO_SPAWN_EGG);
            output.accept(GOLDEN_EAGLE_SPAWN_EGG);
            */
        });
    }
}
