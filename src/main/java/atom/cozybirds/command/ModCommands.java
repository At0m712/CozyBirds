package atom.cozybirds.command;

import atom.cozybirds.entity.magpie.MagpieEntity;
import atom.cozybirds.entity.pigeon.PigeonEntity;
import atom.cozybirds.entity.spawner.BirdSpawnConfig;
import atom.cozybirds.registry.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

import java.util.List;
import java.util.UUID;

public class ModCommands {
    private static final List<String> SPECIES_LIST = List.of("pigeon", "magpie");

    private static final SuggestionProvider<CommandSourceStack> SPECIES_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(SPECIES_LIST, builder);

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // --- 1. COMMANDES DE SPAWN UNIVERSELLES ---
        // /spawnbird <species> [count]
        dispatcher.register(Commands.literal("spawnbird")
                .requires(CommandSourceStack::isPlayer)
                .then(Commands.argument("species", StringArgumentType.word())
                        .suggests(SPECIES_SUGGESTIONS)
                        .executes(context -> spawnSpecies(context.getSource(), StringArgumentType.getString(context, "species"), 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 60))
                                .executes(context -> spawnSpecies(context.getSource(),
                                        StringArgumentType.getString(context, "species"),
                                        IntegerArgumentType.getInteger(context, "count"))))));

        // /cozybirds spawn <species> [count]
        dispatcher.register(Commands.literal("cozybirds")
                .then(Commands.literal("spawn")
                        .requires(CommandSourceStack::isPlayer)
                        .then(Commands.argument("species", StringArgumentType.word())
                                .suggests(SPECIES_SUGGESTIONS)
                                .executes(context -> spawnSpecies(context.getSource(), StringArgumentType.getString(context, "species"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 60))
                                        .executes(context -> spawnSpecies(context.getSource(),
                                                StringArgumentType.getString(context, "species"),
                                                IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("disablespawn")
                        .requires(CommandSourceStack::isPlayer)
                        .then(Commands.argument("species", StringArgumentType.word())
                                .suggests(SPECIES_SUGGESTIONS)
                                .executes(context -> setSpeciesSpawn(context.getSource(), StringArgumentType.getString(context, "species"), false))))
                .then(Commands.literal("enablespawn")
                        .requires(CommandSourceStack::isPlayer)
                        .then(Commands.argument("species", StringArgumentType.word())
                                .suggests(SPECIES_SUGGESTIONS)
                                .executes(context -> setSpeciesSpawn(context.getSource(), StringArgumentType.getString(context, "species"), true))))
                .then(Commands.literal("togglespawn")
                        .requires(CommandSourceStack::isPlayer)
                        .then(Commands.argument("species", StringArgumentType.word())
                                .suggests(SPECIES_SUGGESTIONS)
                                .executes(context -> toggleSpeciesSpawn(context.getSource(), StringArgumentType.getString(context, "species")))))
                .then(Commands.literal("spawnstatus")
                        .executes(context -> showSpawnStatus(context.getSource()))));

        // --- 2. COMMANDES DIRECTES D'ACTIVATION / DÉSACTIVATION ---
        // /disablespawn <species>
        dispatcher.register(Commands.literal("disablespawn")
                .requires(CommandSourceStack::isPlayer)
                .then(Commands.argument("species", StringArgumentType.word())
                        .suggests(SPECIES_SUGGESTIONS)
                        .executes(context -> setSpeciesSpawn(context.getSource(), StringArgumentType.getString(context, "species"), false))));

        // /enablespawn <species>
        dispatcher.register(Commands.literal("enablespawn")
                .requires(CommandSourceStack::isPlayer)
                .then(Commands.argument("species", StringArgumentType.word())
                        .suggests(SPECIES_SUGGESTIONS)
                        .executes(context -> setSpeciesSpawn(context.getSource(), StringArgumentType.getString(context, "species"), true))));

        // /togglespawn <species>
        dispatcher.register(Commands.literal("togglespawn")
                .requires(CommandSourceStack::isPlayer)
                .then(Commands.argument("species", StringArgumentType.word())
                        .suggests(SPECIES_SUGGESTIONS)
                        .executes(context -> toggleSpeciesSpawn(context.getSource(), StringArgumentType.getString(context, "species")))));

        // --- 3. RACCOURCIS DE SPAWN SPÉCIFIQUES ---
        dispatcher.register(Commands.literal("spawnpigeons")
                .requires(CommandSourceStack::isPlayer)
                .executes(context -> spawnSpecies(context.getSource(), "pigeon", 6))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 60))
                        .executes(context -> spawnSpecies(context.getSource(), "pigeon", IntegerArgumentType.getInteger(context, "count")))));

        dispatcher.register(Commands.literal("spawnmagpies")
                .requires(CommandSourceStack::isPlayer)
                .executes(context -> spawnSpecies(context.getSource(), "magpie", 35))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 60))
                        .executes(context -> spawnSpecies(context.getSource(), "magpie", IntegerArgumentType.getInteger(context, "count")))));

        dispatcher.register(Commands.literal("magpiescount")
                .requires(CommandSourceStack::isPlayer)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = context.getSource().getLevel();
                    net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(64.0);
                    int count = level.getEntitiesOfClass(MagpieEntity.class, box).size();
                    context.getSource().sendSuccess(() -> Component.literal("§ePies à proximité (rayon 64 blocs): §f" + count), false);
                    return count;
                }));

        dispatcher.register(Commands.literal("killmagpies")
                .requires(CommandSourceStack::isPlayer)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = context.getSource().getLevel();
                    net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(128.0);
                    var magpies = level.getEntitiesOfClass(MagpieEntity.class, box);
                    int count = magpies.size();
                    for (MagpieEntity magpie : magpies) {
                        magpie.discard();
                    }
                    context.getSource().sendSuccess(() -> Component.literal("§cSuppression de " + count + " pies."), true);
                    return count;
                }));
    }

    private static int spawnSpecies(CommandSourceStack source, String species, int count) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Cette commande doit être exécutée par un joueur."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        String s = species.toLowerCase();

        if (s.equals("pigeon") || s.equals("pigeons")) {
            for (int i = 0; i < count; i++) {
                PigeonEntity pigeon = ModEntities.PIGEON.create(level, EntitySpawnReason.COMMAND);
                if (pigeon != null) {
                    double ox = (player.getRandom().nextDouble() - 0.5) * 4.0;
                    double oz = (player.getRandom().nextDouble() - 0.5) * 4.0;
                    pigeon.setPos(player.getX() + ox, player.getY(), player.getZ() + oz);
                    level.addFreshEntity(pigeon);
                }
            }
            source.sendSuccess(() -> Component.literal("§bApparition de §f" + count + " §bpigeon(s) avec succès !"), true);
            return count;
        }

        if (s.equals("magpie") || s.equals("magpies") || s.equals("pie") || s.equals("pies")) {
            UUID flockUuid = UUID.randomUUID();
            MagpieEntity leader = null;
            double px = player.getX();
            double py = player.getY() + (count > 5 ? 12.0 : 0.0);
            double pz = player.getZ();

            for (int i = 0; i < count; i++) {
                MagpieEntity magpie = ModEntities.MAGPIE.create(level, EntitySpawnReason.COMMAND);
                if (magpie != null) {
                    magpie.setFlockId(flockUuid);
                    double ox = (player.getRandom().nextDouble() - 0.5) * 3.0;
                    double oy = (count > 5) ? (player.getRandom().nextDouble() - 0.5) * 2.0 : 0.0;
                    double oz = (player.getRandom().nextDouble() - 0.5) * 3.0;
                    magpie.setPos(px + ox, py + oy, pz + oz);

                    if (count > 5) {
                        magpie.setDeltaMovement(0.35, 0.05, 0.1);
                    }

                    if (leader == null) {
                        leader = magpie;
                    } else {
                        magpie.setFlockLeader(leader);
                    }

                    level.addFreshEntity(magpie);
                }
            }
            source.sendSuccess(() -> Component.literal("§aApparition de §f" + count + " §apie(s) en essaim soudé !"), true);
            return count;
        }

        source.sendFailure(Component.literal("Espèce inconnue: '" + species + "'. Espèces disponibles: pigeon, magpie"));
        return 0;
    }

    private static int setSpeciesSpawn(CommandSourceStack source, String species, boolean enabled) {
        boolean ok = BirdSpawnConfig.setSpawnEnabled(species, enabled);
        if (!ok) {
            source.sendFailure(Component.literal("Espèce inconnue: '" + species + "'. Espèces disponibles: pigeon, magpie"));
            return 0;
        }

        String stateStr = enabled ? "§aACTIVÉ" : "§cDÉSACTIVÉ";
        source.sendSuccess(() -> Component.literal("§6[CozyBirds] §fLe spawn naturel de §e" + species.toLowerCase() + " §fest désormais " + stateStr + "§f."), true);
        return 1;
    }

    private static int toggleSpeciesSpawn(CommandSourceStack source, String species) {
        Boolean newState = BirdSpawnConfig.toggleSpawn(species);
        if (newState == null) {
            source.sendFailure(Component.literal("Espèce inconnue: '" + species + "'. Espèces disponibles: pigeon, magpie"));
            return 0;
        }

        String stateStr = newState ? "§aACTIVÉ" : "§cDÉSACTIVÉ";
        source.sendSuccess(() -> Component.literal("§6[CozyBirds] §fLe spawn naturel de §e" + species.toLowerCase() + " §fest désormais " + stateStr + "§f."), true);
        return 1;
    }

    private static int showSpawnStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Statut du Spawn Naturel CozyBirds ==="), false);
        String pState = BirdSpawnConfig.PIGEON_SPAWN_ENABLED ? "§a[ACTIVÉ]" : "§c[DÉSACTIVÉ]";
        String mState = BirdSpawnConfig.MAGPIE_SPAWN_ENABLED ? "§a[ACTIVÉ]" : "§c[DÉSACTIVÉ]";
        source.sendSuccess(() -> Component.literal("§7- Pigeon: " + pState), false);
        source.sendSuccess(() -> Component.literal("§7- Pie (Magpie): " + mState), false);
        return 1;
    }
}
