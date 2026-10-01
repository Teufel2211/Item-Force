package com.findtheblock.command;

import com.findtheblock.FindTheBlockMod;
import com.findtheblock.game.GameManager;
import com.findtheblock.team.Team;
import com.findtheblock.team.TeamManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Optional;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class FindBlockCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("findblock")
                // Public read-only (M5)
                .then(literal("status").executes(context -> executeStatus(context.getSource())))
                .then(literal("score")
                        .executes(context -> executeScore(context.getSource(), null))
                        .then(argument("team", StringArgumentType.word()).executes(context -> executeScore(context.getSource(), StringArgumentType.getString(context, "team"))))
                        .then(literal("reset").requires(FindBlockCommand::isAdmin).executes(context -> executeScoreReset(context.getSource()))))
                .then(literal("team")
                        .then(literal("list").executes(context -> executeTeamList(context.getSource())))
                        .then(literal("info").then(argument("team", StringArgumentType.word()).executes(context -> executeTeamInfo(context.getSource(), StringArgumentType.getString(context, "team")))))
                        .then(literal("add").requires(FindBlockCommand::isAdmin).then(argument("player", StringArgumentType.word()).then(argument("team", StringArgumentType.word()).executes(context -> executeTeamAdd(context.getSource(), StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "team"))))))
                        .then(literal("remove").requires(FindBlockCommand::isAdmin).then(argument("player", StringArgumentType.word()).executes(context -> executeTeamRemove(context.getSource(), StringArgumentType.getString(context, "player"))))))
                // Admin write commands (M5)
                .then(literal("start").requires(FindBlockCommand::isAdmin).executes(context -> executeStart(context.getSource())))
                .then(literal("stop").requires(FindBlockCommand::isAdmin).executes(context -> executeStop(context.getSource())))
                .then(literal("pause").requires(FindBlockCommand::isAdmin).executes(context -> executePause(context.getSource())))
                .then(literal("resume").requires(FindBlockCommand::isAdmin).executes(context -> executeResume(context.getSource())))
                .then(literal("restart").requires(FindBlockCommand::isAdmin).executes(context -> executeRestart(context.getSource())))
                .then(literal("next").requires(FindBlockCommand::isAdmin).executes(context -> executeNext(context.getSource())))
                .then(literal("reload").requires(FindBlockCommand::isAdmin).executes(context -> executeReload(context.getSource())))
        );
    }

    private static boolean isAdmin(CommandSourceStack source) {
        try {
            return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        } catch (Exception e) {
            return source.hasPermission(4);
        }
    }

    private static int executeStart(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        boolean ok = gameManager.startGame(getPlayerFromSource(source));
        if (!ok) {
            source.sendSystemMessage(Component.literal("Start nicht moglich (bereits aktiv oder keine Blocke). Siehe Status."));
            return 0;
        }
        return 1;
    }

    private static int executeStop(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        boolean ok = gameManager.stopGame(getPlayerFromSource(source));
        if (!ok) {
            source.sendSystemMessage(Component.literal("Es lauft kein Spiel."));
            return 0;
        }
        source.sendSystemMessage(Component.literal("Spiel gestoppt."));
        return 1;
    }

    private static int executePause(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        boolean ok = gameManager.pauseGame(getPlayerFromSource(source));
        if (!ok) {
            source.sendSystemMessage(Component.literal("Pause nicht moglich (nur AKTIV/COUNTDOWN)."));
            return 0;
        }
        return 1;
    }

    private static int executeResume(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        boolean ok = gameManager.resumeGame(getPlayerFromSource(source));
        if (!ok) {
            source.sendSystemMessage(Component.literal("Resume nicht moglich (nicht pausiert)."));
            return 0;
        }
        return 1;
    }

    private static int executeRestart(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        gameManager.restartGame(getPlayerFromSource(source));
        source.sendSystemMessage(Component.literal("Spiel zuruckgesetzt. Punkte bleiben (siehe /findblock score reset)."));
        return 1;
    }

    private static int executeStatus(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        source.sendSystemMessage(Component.literal(gameManager.getStatus()));
        return 1;
    }

    private static int executeNext(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        boolean ok = gameManager.nextRound();
        if (!ok) {
            source.sendSystemMessage(Component.literal("Next nicht moglich (kein aktives Spiel)."));
            return 0;
        }
        source.sendSystemMessage(Component.literal("Nachste Runde wird gestartet."));
        return 1;
    }

    private static int executeReload(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        gameManager.reloadConfigs();
        source.sendSystemMessage(Component.literal("Konfiguration neu geladen. Hinweis: blockOrder wirkt bei aktivem Spiel erst nach stop+start voll."));
        return 1;
    }

    private static int executeScore(CommandSourceStack source, String teamId) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        if (teamId == null) {
            StringBuilder result = new StringBuilder("Team Punktestand:\n");
            for (Team team : gameManager.getTeamManager().getTeams()) {
                result.append(team.name).append(": ").append(gameManager.getTeamManager().getScore(team.id)).append(" Punkte\n");
            }
            source.sendSystemMessage(Component.literal(result.toString()));
        } else {
            Optional<Team> team = gameManager.getTeamManager().getTeam(teamId);
            if (team.isEmpty()) {
                source.sendSystemMessage(Component.literal("Team nicht gefunden: " + teamId));
                return 0;
            } else {
                source.sendSystemMessage(Component.literal(team.get().name + ": " + gameManager.getTeamManager().getScore(teamId) + " Punkte"));
            }
        }
        return 1;
    }

    private static int executeScoreReset(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        gameManager.resetScores();
        source.sendSystemMessage(Component.literal("Alle Punkte zuruckgesetzt."));
        return 1;
    }

    private static int executeTeamList(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        StringBuilder builder = new StringBuilder("Teams:\n");
        for (Team team : gameManager.getTeamManager().getTeams()) {
            builder.append(team.id).append(" - ").append(team.name)
                    .append(" (").append(gameManager.getTeamManager().getScore(team.id)).append(" Punkte)\n");
        }
        source.sendSystemMessage(Component.literal(builder.toString()));
        return 1;
    }

    private static int executeTeamInfo(CommandSourceStack source, String teamId) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        Optional<Team> team = gameManager.getTeamManager().getTeam(teamId);
        if (team.isEmpty()) {
            source.sendSystemMessage(Component.literal("Team nicht gefunden: " + teamId));
            return 0;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Team: ").append(team.get().name).append("\n");
        builder.append("ID: ").append(team.get().id).append("\n");
        builder.append("Punkte: ").append(gameManager.getTeamManager().getScore(teamId)).append("\n");
        source.sendSystemMessage(Component.literal(builder.toString()));
        return 1;
    }

    private static int executeTeamAdd(CommandSourceStack source, String playerName, String teamId) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        Optional<Team> team = gameManager.getTeamManager().getTeam(teamId);
        if (team.isEmpty()) {
            source.sendSystemMessage(Component.literal("Team nicht gefunden: " + teamId));
            return 0;
        }
        UUID uuid = resolvePlayerUuid(source.getServer(), playerName);
        if (uuid == null) {
            source.sendSystemMessage(Component.literal("Spieler nicht gefunden (auch nicht offline): " + playerName));
            return 0;
        }
        boolean ok = gameManager.getTeamManager().assignPlayer(uuid, teamId);
        if (!ok) {
            source.sendSystemMessage(Component.literal("Zuweisung fehlgeschlagen."));
            return 0;
        }
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(playerName);
        String display = online != null ? online.getName().getString() : playerName;
        source.sendSystemMessage(Component.literal(display + " wurde dem Team " + team.get().name + " zugewiesen."));
        return 1;
    }

    private static int executeTeamRemove(CommandSourceStack source, String playerName) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        UUID uuid = resolvePlayerUuid(source.getServer(), playerName);
        if (uuid == null) {
            source.sendSystemMessage(Component.literal("Spieler nicht gefunden (auch nicht offline): " + playerName));
            return 0;
        }
        boolean removed = gameManager.getTeamManager().removePlayer(uuid);
        if (!removed) {
            source.sendSystemMessage(Component.literal("Spieler war keinem Team zugewiesen: " + playerName));
            return 0;
        }
        source.sendSystemMessage(Component.literal(playerName + " wurde aus dem Team entfernt."));
        return 1;
    }

    private static UUID resolvePlayerUuid(MinecraftServer server, String name) {
        if (server == null || name == null) return null;
        try {
            ServerPlayer online = server.getPlayerList().getPlayerByName(name);
            if (online != null) return online.getUUID();
        } catch (Exception ignored) {
        }
        try {
            var cache = server.getProfileCache();
            if (cache != null) {
                Optional<GameProfile> profile = cache.get(name);
                if (profile.isPresent() && profile.get().getId() != null) {
                    return profile.get().getId();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ServerPlayer getPlayerFromSource(CommandSourceStack source) {
        try {
            return source.getPlayer();
        } catch (Exception e) {
            return null;
        }
    }
}
