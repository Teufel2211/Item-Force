package com.findtheblock.game;

import com.findtheblock.FindTheBlockMod;
import com.findtheblock.config.ConfigManager;
import com.findtheblock.config.FindBlockConfig;
import com.findtheblock.hud.BossBarManager;
import com.findtheblock.hud.ScoreDisplay;
import com.findtheblock.team.Team;
import com.findtheblock.team.TeamManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameManager {
    private final TeamManager teamManager;
    private GameState state = GameState.WAITING;
    private GameState stateBeforePause = null;
    private int roundNumber = 0;
    private final List<Identifier> blockOrder = new ArrayList<>();
    private int currentIndex = -1;
    private int countdownTicks = 0;
    private int betweenRoundTicks = 0;
    private final AtomicBoolean roundAlreadyWon = new AtomicBoolean(false);
    private Identifier currentTarget;
    private MinecraftServer server;
    private BossBarManager bossBarManager;
    private ScoreDisplay scoreDisplay;
    private boolean paused = false;

    public GameManager() {
        this.teamManager = new TeamManager();
    }

    public void initialize(MinecraftServer server) {
        // Old bossbar players cleanup (M4): avoid leaking viewers across re-init
        if (this.bossBarManager != null) {
            try {
                this.bossBarManager.removeAllPlayers();
            } catch (Exception ignored) {
            }
        }
        this.server = server;
        this.bossBarManager = new BossBarManager();
        this.scoreDisplay = new ScoreDisplay(server);
        reloadConfigs();
        refreshHud();
    }

    public void tick(MinecraftServer server) {
        this.server = server;
        if (paused) {
            return;
        }

        switch (state) {
            case WAITING:
                break;
            case COUNTDOWN:
                tickCountdown();
                break;
            case ACTIVE:
                break;
            case FOUND:
            case NEXT_ROUND:
                if (betweenRoundTicks > 0) {
                    betweenRoundTicks--;
                } else {
                    advanceToNextRound();
                }
                break;
            case FINISHED:
                break;
        }
    }

    private void tickCountdown() {
        if (countdownTicks <= 0) {
            startRound();
            return;
        }

        countdownTicks--;
        if (countdownTicks > 0 && countdownTicks % 20 == 0) {
            int seconds = countdownTicks / 20;
            if (roundNumber == 0) {
                broadcastMessage("Spiel startet in " + seconds + " Sekunden!");
                bossBarManager.setTitle("Spiel startet in " + seconds);
            } else {
                broadcastMessage("Nachste Runde in " + seconds + " ...");
                bossBarManager.setTitle("Nachste Runde in " + seconds);
            }
        }

        if (countdownTicks <= 0) {
            startRound();
        }
    }

    public boolean startGame(ServerPlayer sender) {
        if (paused || (state != GameState.WAITING && state != GameState.FINISHED)) {
            sendMessage(sender, "Find the Block ist bereits gestartet.");
            return false;
        }

        if (ConfigManager.CONFIG != null) {
            ConfigManager.CONFIG.validate();
        }
        if (blockOrder.isEmpty()) {
            sendMessage(sender, "Keine gultigen Blocke gefunden. Bitte blocks.json prufen.");
            broadcastMessage("Keine gultigen Blocke gefunden. Bitte blocks.json prufen.");
            return false;
        }

        paused = false;
        stateBeforePause = null;
        roundNumber = 0;
        currentIndex = 0;
        roundAlreadyWon.set(false);
        state = GameState.COUNTDOWN;
        int cd = ConfigManager.CONFIG != null ? ConfigManager.CONFIG.countdownSeconds : 5;
        if (cd < 1) cd = 5;
        countdownTicks = cd * 20;
        betweenRoundTicks = 0;

        broadcastMessage("Find the Block Spiel startet!");
        bossBarManager.setColor(BossEvent.BossBarColor.RED);
        bossBarManager.setTitle("Find the Block startet in " + cd);
        bossBarManager.setVisible(true);
        playGlobalSound(SoundEvents.NOTE_BLOCK_PLING);
        refreshHud();
        return true;
    }

    public boolean stopGame(ServerPlayer sender) {
        if (state == GameState.WAITING && !paused) {
            sendMessage(sender, "Es lauft kein Find the Block Spiel.");
            return false;
        }

        paused = false;
        stateBeforePause = null;
        state = GameState.FINISHED;
        broadcastMessage("Find the Block Spiel beendet.");
        bossBarManager.setColor(BossEvent.BossBarColor.RED);
        bossBarManager.setTitle("SPIEL BEENDET");
        // Hide after stop so HUD does not stick forever (M4)
        bossBarManager.setVisible(false);
        refreshHud();
        return true;
    }

    public boolean pauseGame(ServerPlayer sender) {
        if (paused) {
            sendMessage(sender, "Das Spiel ist bereits pausiert.");
            return false;
        }
        if (state != GameState.ACTIVE && state != GameState.COUNTDOWN) {
            sendMessage(sender, "Das Spiel kann jetzt nicht pausiert werden (nur AKTIV/COUNTDOWN).");
            return false;
        }

        paused = true;
        stateBeforePause = state;
        state = GameState.WAITING;
        sendMessage(sender, "Spiel pausiert.");
        broadcastMessage("Spiel pausiert.");
        bossBarManager.setTitle("PAUSIERT");
        refreshHud();
        return true;
    }

    public boolean resumeGame(ServerPlayer sender) {
        if (!paused) {
            sendMessage(sender, "Das Spiel ist nicht pausiert.");
            return false;
        }

        paused = false;
        state = stateBeforePause != null ? stateBeforePause : GameState.ACTIVE;
        stateBeforePause = null;
        sendMessage(sender, "Spiel fortgesetzt.");
        broadcastMessage("Spiel fortgesetzt.");
        if (currentTarget != null && state == GameState.ACTIVE) {
            bossBarManager.setColor(BossEvent.BossBarColor.YELLOW);
            bossBarManager.setTitle("GESUCHT: " + toReadableName(currentTarget));
        }
        refreshHud();
        return true;
    }

    public boolean restartGame(ServerPlayer sender) {
        paused = false;
        stateBeforePause = null;
        state = GameState.WAITING;
        roundNumber = 0;
        currentIndex = -1;
        countdownTicks = 0;
        betweenRoundTicks = 0;
        roundAlreadyWon.set(false);
        currentTarget = null;
        sendMessage(sender, "Spiel neu gestartet. Punkte bleiben erhalten (siehe /findblock score reset).");
        bossBarManager.setVisible(false);
        refreshHud();
        return true;
    }

    public void resetScores() {
        teamManager.resetScores();
        refreshHud();
    }

    private void startRound() {
        if (currentIndex < 0 || currentIndex >= blockOrder.size()) {
            finishGame();
            return;
        }

        roundNumber++;
        currentTarget = blockOrder.get(currentIndex);
        roundAlreadyWon.set(false);
        state = GameState.ACTIVE;

        broadcastMessage("Neue Runde " + roundNumber + "! Gesucht wird: " + toReadableName(currentTarget));
        bossBarManager.setColor(BossEvent.BossBarColor.YELLOW);
        bossBarManager.setTitle("GESUCHT: " + toReadableName(currentTarget));
        bossBarManager.setVisible(shouldShowBossBar());
        playGlobalSound(SoundEvents.NOTE_BLOCK_PLING);
        refreshHud();
    }

    public boolean nextRound() {
        if (state == GameState.ACTIVE || state == GameState.FOUND || state == GameState.COUNTDOWN) {
            roundAlreadyWon.set(false);
            advanceToNextRound();
            return true;
        }
        return false;
    }

    private void advanceToNextRound() {
        currentIndex++;
        int size = blockOrder.size();
        boolean loop = ConfigManager.CONFIG != null && ConfigManager.CONFIG.loopBlocks;
        int maxRounds = ConfigManager.CONFIG != null ? ConfigManager.CONFIG.maxRounds : 10;
        if (maxRounds < 1) maxRounds = 10;

        if (loop) {
            // With loop: play until maxRounds reached, wrap block list (M6)
            if (roundNumber >= maxRounds) {
                finishGame();
                return;
            }
            if (size <= 0) {
                finishGame();
                return;
            }
            if (currentIndex >= size) {
                currentIndex = 0;
            }
        } else {
            // Without loop: effective end = min(blocks, maxRounds) (M6)
            int effectiveMax = Math.min(size, maxRounds);
            if (roundNumber >= effectiveMax || currentIndex >= size) {
                finishGame();
                return;
            }
        }

        state = GameState.COUNTDOWN;
        int cd = ConfigManager.CONFIG != null ? ConfigManager.CONFIG.countdownSeconds : 5;
        if (cd < 1) cd = 5;
        countdownTicks = cd * 20;
        bossBarManager.setColor(BossEvent.BossBarColor.PURPLE);
        bossBarManager.setTitle("Nachste Runde in " + cd);
        refreshHud();
    }

    private void finishGame() {
        state = GameState.FINISHED;
        paused = false;
        stateBeforePause = null;

        Team winner = determineWinner();
        if (winner != null) {
            broadcastMessage("SPIEL BEENDET");
            broadcastMessage(winner.name + " GEWINNT!");
            bossBarManager.setColor(BossEvent.BossBarColor.GREEN);
            bossBarManager.setTitle(winner.name + " GEWINNT!");
        } else {
            broadcastMessage("SPIEL BEENDET");
            broadcastMessage("UNENTSCHIEDEN");
            bossBarManager.setColor(BossEvent.BossBarColor.RED);
            bossBarManager.setTitle("UNENTSCHIEDEN");
        }

        bossBarManager.setVisible(shouldShowBossBar());
        broadcastScores();
        try {
            playGlobalSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.PLAYER_LEVELUP));
        } catch (Exception e) {
            playGlobalSound(SoundEvents.NOTE_BLOCK_BELL);
        }
        refreshHud();
    }

    private Team determineWinner() {
        Team best = null;
        int bestScore = -1;
        boolean tie = false;

        for (Team team : teamManager.getTeams()) {
            int score = teamManager.getScore(team.id);
            if (score > bestScore) {
                bestScore = score;
                best = team;
                tie = false;
            } else if (score == bestScore) {
                tie = true;
            }
        }

        if (bestScore <= 0) {
            return null;
        }
        return tie ? null : best;
    }

    public boolean handleBlockFound(ServerPlayer player, ServerLevel world, BlockState blockState) {
        if (blockState == null) return false;
        return handleFound(player, world, blockState.getBlock());
    }

    public boolean handleBlockPlaced(ServerPlayer player, ServerLevel world, Block block) {
        return handleFound(player, world, block);
    }

    private boolean handleFound(ServerPlayer player, ServerLevel world, Block block) {
        if (player == null || world == null || block == null) return false;
        if (state != GameState.ACTIVE || currentTarget == null) {
            return false;
        }

        if (!isDimensionAllowed(world)) {
            return false;
        }

        if (!isTargetBlock(block)) {
            return false;
        }

        // Team check BEFORE consuming roundAlreadyWon (C3)
        Optional<Team> playerTeam = teamManager.getTeamForPlayer(player);
        boolean allowUnassigned = ConfigManager.CONFIG != null && ConfigManager.CONFIG.allowUnassignedPlayers;
        boolean allowed = allowUnassigned || playerTeam.isPresent();

        if (!allowed) {
            player.sendSystemMessage(Component.literal("Du bist keinem Team zugewiesen - Runde lauft weiter!"));
            return false;
        }

        if (!roundAlreadyWon.compareAndSet(false, true)) {
            return false;
        }

        if (playerTeam.isPresent()) {
            Team team = playerTeam.get();
            teamManager.addScore(team.id, 1);
            broadcastMessage(toReadableName(currentTarget) + " GEFUNDEN!");
            broadcastMessage(player.getName().getString() + " hat den Block gefunden.");
            broadcastMessage("Team " + team.name + " erhalt 1 Punkt!");
        } else {
            broadcastMessage(toReadableName(currentTarget) + " GEFUNDEN!");
            broadcastMessage(player.getName().getString() + " hat den Block gefunden. Kein Team-Punkt (Spieler ohne Team).");
        }

        state = GameState.FOUND;
        int btw = ConfigManager.CONFIG != null ? ConfigManager.CONFIG.betweenRoundSeconds : 3;
        if (btw < 0) btw = 3;
        betweenRoundTicks = btw * 20;
        bossBarManager.setColor(BossEvent.BossBarColor.GREEN);
        bossBarManager.setTitle(toReadableName(currentTarget) + " GEFUNDEN!");
        playGlobalSound(SoundEvents.NOTE_BLOCK_BELL);
        refreshHud();
        return true;
    }

    private boolean isTargetBlock(Block block) {
        try {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || currentTarget == null || !id.equals(currentTarget)) {
                return false;
            }
            return block == BuiltInRegistries.BLOCK.getValue(currentTarget);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isDimensionAllowed(ServerLevel world) {
        try {
            if (ConfigManager.CONFIG == null) return true;
            List<String> allowed = ConfigManager.CONFIG.allowedDimensions;
            if (allowed == null || allowed.isEmpty()) {
                return true;
            }
            String dimId = dimensionId(world);
            return allowed.contains(dimId);
        } catch (Exception e) {
            return true;
        }
    }

    private static String dimensionId(ServerLevel level) {
        try {
            Object key = level.dimension();
            if (key == null) return "";
            // Mojmap: location(), Yarn-alt: identifier()/getValue() - via reflection for compat (M7)
            for (String m : new String[]{"location", "identifier", "getValue"}) {
                try {
                    var method = key.getClass().getMethod(m);
                    Object id = method.invoke(key);
                    if (id != null) return String.valueOf(id);
                } catch (NoSuchMethodException ignored) {
                }
            }
            return key.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public void onPlayerJoin(ServerPlayer player) {
        if (player == null) return;
        try {
            if (bossBarManager != null && ConfigManager.CONFIG != null && ConfigManager.CONFIG.showBossBar) {
                bossBarManager.addPlayer(player);
            }
        } catch (Exception ignored) {
        }
        refreshHud();
    }

    public void onPlayerLeave(ServerPlayer player) {
        if (player == null) return;
        try {
            if (bossBarManager != null) {
                bossBarManager.removePlayer(player);
            }
        } catch (Exception ignored) {
        }
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Find the Block\n");
        sb.append("\nStatus: ").append(stateName(state)).append("\n");
        if (paused) {
            sb.append("PAUSIERT\n");
        }
        int maxRounds = ConfigManager.CONFIG != null ? ConfigManager.CONFIG.maxRounds : 10;
        sb.append("Runde: ").append(roundNumber).append("/").append(maxRounds).append("\n");
        if (currentTarget != null) {
            sb.append("Gesuchter Block: ").append(toReadableName(currentTarget)).append("\n");
        }
        sb.append("\n");
        sb.append(scoreLines());
        return sb.toString();
    }

    private String scoreLines() {
        StringBuilder sb = new StringBuilder();
        for (Team team : teamManager.getTeams()) {
            int score = teamManager.getScore(team.id);
            sb.append(team.name).append(": ").append(score).append(score == 1 ? " Punkt" : " Punkte").append("\n");
        }
        return sb.toString();
    }

    private void broadcastScores() {
        String lines = scoreLines().trim();
        if (!lines.isEmpty()) {
            broadcastMessage(lines);
        }
    }

    private String stateName(GameState s) {
        if (s == null) return "UNBEKANNT";
        switch (s) {
            case WAITING: return "WARTEND";
            case COUNTDOWN: return "COUNTDOWN";
            case ACTIVE: return "AKTIV";
            case FOUND: return "GEFUNDEN";
            case NEXT_ROUND: return "NAECHSTE RUNDE";
            case FINISHED: return "BEENDET";
            default: return "UNBEKANNT";
        }
    }

    public void reloadConfigs() {
        boolean gameActive = (state == GameState.ACTIVE || state == GameState.COUNTDOWN || state == GameState.FOUND);
        Identifier oldTarget = currentTarget;

        ConfigManager.reload();
        if (ConfigManager.CONFIG != null) {
            ConfigManager.CONFIG.validate();
        }
        teamManager.reload();

        List<Identifier> fresh = new ArrayList<>();
        if (ConfigManager.BLOCKS != null && ConfigManager.BLOCKS.blocks != null) {
            for (String blockName : ConfigManager.BLOCKS.blocks) {
                try {
                    if (blockName == null) continue;
                    Identifier id = Identifier.tryParse(blockName);
                    if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                        fresh.add(id);
                    } else {
                        FindTheBlockMod.LOGGER.warn("Ungultiger Block in blocks.json ubersprungen: {}", blockName);
                    }
                } catch (Exception e) {
                    FindTheBlockMod.LOGGER.warn("Ungultiger Block in blocks.json ubersprungen: {}", blockName);
                }
            }
        }
        blockOrder.clear();
        blockOrder.addAll(fresh);

        if (blockOrder.isEmpty()) {
            FindTheBlockMod.LOGGER.warn("Keine gultigen Blocke nach Reload - Spiel kann nicht starten.");
        }

        if (gameActive) {
            // Do not corrupt running round (M2): keep current target for this round
            if (oldTarget != null) {
                currentTarget = oldTarget;
                int idx = blockOrder.indexOf(oldTarget);
                if (idx >= 0) {
                    currentIndex = idx;
                }
                FindTheBlockMod.LOGGER.warn("Reload wahrend aktivem Spiel: blockOrder aktualisiert, laufende Runde bleibt beim alten Ziel. Voll wirksam nach /findblock stop+start.");
            } else if (currentIndex >= blockOrder.size()) {
                currentIndex = blockOrder.isEmpty() ? -1 : 0;
            }
        } else {
            currentIndex = -1;
            if (state == GameState.WAITING || state == GameState.FINISHED) {
                currentTarget = null;
            }
        }
        refreshHud();
    }

    private void refreshHud() {
        try {
            if (scoreDisplay == null || ConfigManager.CONFIG == null) {
                return;
            }
            if (ConfigManager.CONFIG.showScoreboard) {
                scoreDisplay.update(teamManager.getTeams(), teamManager.getScores());
            } else {
                scoreDisplay.clear();
            }
            if (bossBarManager != null) {
                bossBarManager.setVisible(shouldShowBossBar());
            }
        } catch (Exception e) {
            FindTheBlockMod.LOGGER.warn("HUD-Refresh fehlgeschlagen", e);
        }
    }

    private boolean shouldShowBossBar() {
        if (ConfigManager.CONFIG == null || !ConfigManager.CONFIG.showBossBar) return false;
        if (paused) return true;
        return state != GameState.WAITING;
    }

    private String toReadableName(Identifier id) {
        if (id == null) return "?";
        try {
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block != null) {
                String localized = block.getName().getString();
                if (localized != null && !localized.isBlank()) {
                    return localized;
                }
            }
        } catch (Exception ignored) {
        }
        return id.getPath().replace("_", " ");
    }

    private void broadcastMessage(String message) {
        if (server != null && message != null) {
            try {
                server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
            } catch (Exception ignored) {
            }
        }
    }

    private void sendMessage(ServerPlayer player, String message) {
        if (player != null && message != null) {
            try {
                player.sendSystemMessage(Component.literal(message));
            } catch (Exception ignored) {
            }
        }
    }

    private void playGlobalSound(Holder<SoundEvent> sound) {
        if (server == null || sound == null) {
            return;
        }
        try {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                try {
                    player.level().playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            sound,
                            SoundSource.MASTER,
                            1.0F,
                            1.0F
                    );
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    // Getters
    public GameState getState() {
        return state;
    }

    public boolean isPaused() {
        return paused;
    }

    public Identifier getCurrentTarget() {
        return currentTarget;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public FindBlockConfig getConfig() {
        return ConfigManager.CONFIG;
    }
}
