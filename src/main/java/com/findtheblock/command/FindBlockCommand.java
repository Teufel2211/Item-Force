package com.findtheblock.command;

import com.findtheblock.FindTheBlockMod;
import com.findtheblock.game.GameManager;
import com.findtheblock.team.Team;
import com.findtheblock.team.TeamManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Collection;
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
                        .then(literal("add").requires(FindBlockCommand::isAdmin).then(argument("player", GameProfileArgument.gameProfile()).then(argument("team", StringArgumentType.word()).executes(context -> executeTeamAdd(context.getSource(), GameProfileArgument.getGameProfiles(context, "player"), StringArgumentType.getString(context, "team"))))))
                        .then(literal("remove").requires(FindBlockCommand::isAdmin).then(argument("player", GameProfileArgument.gameProfile()).executes(context -> executeTeamRemove(context.getSource(), GameProfileArgument.getGameProfiles(context, "player"))))))
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
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static int executeStart(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        audit(source, "start");
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
        audit(source, "stop");
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
        audit(source, "pause");
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
        audit(source, "resume");
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
        audit(source, "restart");
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
        audit(source, "next");
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
        audit(source, "reload");
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
        audit(source, "score reset");
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

    private static int executeTeamAdd(CommandSourceStack source, Collection<GameProfile> profiles, String teamId) {
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
        if (profiles == null || profiles.isEmpty()) {
            source.sendSystemMessage(Component.literal("Kein Spieler angegeben."));
            return 0;
        }
        GameProfile profile = profiles.iterator().next();
        UUID uuid = profile.id();
        if (uuid == null) {
            source.sendSystemMessage(Component.literal("Spieler hat keine UUID (Offline-Profil unvollstaendig)."));
            return 0;
        }
        audit(source, "team add " + profile.name() + " " + teamId);
        boolean ok = gameManager.getTeamManager().assignPlayer(uuid, teamId);
        if (!ok) {
            source.sendSystemMessage(Component.literal("Zuweisung fehlgeschlagen."));
            return 0;
        }
        source.sendSystemMessage(Component.literal(profile.name() + " wurde dem Team " + team.get().name + " zugewiesen."));
        return 1;
    }

    private static int executeTeamRemove(CommandSourceStack source, Collection<GameProfile> profiles) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        if (profiles == null || profiles.isEmpty()) {
            source.sendSystemMessage(Component.literal("Kein Spieler angegeben."));
            return 0;
        }
        GameProfile profile = profiles.iterator().next();
        UUID uuid = profile.id();
        if (uuid == null) {
            source.sendSystemMessage(Component.literal("Spieler hat keine UUID (Offline-Profil unvollstaendig)."));
            return 0;
        }
        audit(source, "team remove " + profile.name());
        boolean removed = gameManager.getTeamManager().removePlayer(uuid);
        if (!removed) {
            source.sendSystemMessage(Component.literal("Spieler war keinem Team zugewiesen: " + profile.name()));
            return 0;
        }
        source.sendSystemMessage(Component.literal(profile.name() + " wurde aus dem Team entfernt."));
        return 1;
    }

    static void audit(CommandSourceStack source, String action) {
        try {
            String who;
            try {
                ServerPlayer p = source.getPlayer();
                who = p != null ? p.getName().getString() : source.getDisplayName().getString();
            } catch (Exception e) {
                who = source.getDisplayName().getString();
            }
            FindTheBlockMod.LOGGER.info("findblock: {} -> {}", who, action);
        } catch (Exception ignored) {
        }
    }

    private static ServerPlayer getPlayerFromSource(CommandSourceStack source) {
        try {
            return source.getPlayer();
        } catch (Exception e) {
            return null;
        }
    }
}
