package atom.cozybirds;

import atom.cozybirds.registry.ModEntities;
import atom.cozybirds.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CozyBirds implements ModInitializer {
	public static final String MOD_ID = "cozybirds";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing CozyBirds Mod!");
		ModItems.init();
		ModEntities.init();
		atom.cozybirds.entity.spawner.BirdWorldSpawner.init();
		atom.cozybirds.command.ModCommands.init();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
