package com.findtheblock.command;

import com.findtheblock.FindTheBlockMod;
import com.findtheblock.game.GameManager;
import com.findtheblock.team.Team;
import com.findtheblock.team.TeamManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Optional;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class FindBlockCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("findblock")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(literal("start").executes(context -> executeStart(context.getSource())))
                .then(literal("stop").executes(context -> executeStop(context.getSource())))
                .then(literal("pause").executes(context -> executePause(context.getSource())))
                .then(literal("resume").executes(context -> executeResume(context.getSource())))
                .then(literal("restart").executes(context -> executeRestart(context.getSource())))
                .then(literal("status").executes(context -> executeStatus(context.getSource())))
                .then(literal("next").executes(context -> executeNext(context.getSource())))
                .then(literal("reload").executes(context -> executeReload(context.getSource())))
                .then(literal("score")
                        .executes(context -> executeScore(context.getSource(), null))
                        .then(argument("team", StringArgumentType.word()).executes(context -> executeScore(context.getSource(), StringArgumentType.getString(context, "team"))))
                )
                .then(literal("team")
                        .then(literal("list").executes(context -> executeTeamList(context.getSource())))
                        .then(literal("info").then(argument("team", StringArgumentType.word()).executes(context -> executeTeamInfo(context.getSource(), StringArgumentType.getString(context, "team")))))
                        .then(literal("add").then(argument("player", StringArgumentType.word()).then(argument("team", StringArgumentType.word()).executes(context -> executeTeamAdd(context.getSource(), StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "team"))))))
                        .then(literal("remove").then(argument("player", StringArgumentType.word()).executes(context -> executeTeamRemove(context.getSource(), StringArgumentType.getString(context, "player")))))
                )
        );
    }

    private static int executeStart(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        gameManager.startGame(getPlayerFromSource(source));
        return 1;
    }

    private static int executeStop(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        gameManager.stopGame(getPlayerFromSource(source));
        return 1;
    }

    private static int executePause(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        gameManager.pauseGame(getPlayerFromSource(source));
        return 1;
    }

    private static int executeResume(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        gameManager.resumeGame(getPlayerFromSource(source));
        return 1;
    }

    private static int executeRestart(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        gameManager.restartGame(getPlayerFromSource(source));
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
        gameManager.nextRound();
        source.sendSystemMessage(Component.literal("Nächste Runde wird gestartet."));
        return 1;
    }

    private static int executeReload(CommandSourceStack source) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        gameManager.reloadConfigs();
        source.sendSystemMessage(Component.literal("FindTheBlock Konfiguration neu geladen."));
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
            } else {
                source.sendSystemMessage(Component.literal(team.get().name + ": " + gameManager.getTeamManager().getScore(teamId) + " Punkte"));
            }
        }
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
            builder.append(team.id).append(" - ").append(team.name).append("\n");
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
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (player == null) {
            source.sendSystemMessage(Component.literal("Spieler nicht gefunden: " + playerName));
            return 0;
        }
        gameManager.getTeamManager().assignPlayer(player.getUUID(), teamId);
        source.sendSystemMessage(Component.literal(player.getName().getString() + " wurde dem Team " + team.get().name + " zugewiesen."));
        return 1;
    }

    private static int executeTeamRemove(CommandSourceStack source, String playerName) {
        GameManager gameManager = FindTheBlockMod.GAME_MANAGER;
        if (gameManager == null) {
            source.sendSystemMessage(Component.literal("FindTheBlock ist nicht initialisiert."));
            return 0;
        }
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (player == null) {
            source.sendSystemMessage(Component.literal("Spieler nicht gefunden: " + playerName));
            return 0;
        }
        boolean removed = gameManager.getTeamManager().removePlayer(player.getUUID());
        if (!removed) {
            source.sendSystemMessage(Component.literal("Spieler war keinem Team zugewiesen: " + playerName));
            return 0;
        }
        source.sendSystemMessage(Component.literal(player.getName().getString() + " wurde aus dem Team entfernt."));
        return 1;
    }

    private static ServerPlayer getPlayerFromSource(CommandSourceStack source) {
        return source.getPlayer();
    }
}
