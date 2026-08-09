package com.findtheblock.game;

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
        this.server = server;
        this.bossBarManager = new BossBarManager();
        this.scoreDisplay = new ScoreDisplay(server);
        reloadConfigs();
        refreshHud();
    }

    public void tick(MinecraftServer server) {
        this.server = server;

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
                broadcastMessage("Nächste Runde in " + seconds + " ...");
                bossBarManager.setTitle("Nächste Runde in " + seconds);
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

        if (blockOrder.isEmpty()) {
            sendMessage(sender, "Keine gültigen Blöcke gefunden. Bitte blocks.json prüfen.");
            return false;
        }

        paused = false;
        roundNumber = 0;
        currentIndex = 0;
        roundAlreadyWon.set(false);
        state = GameState.COUNTDOWN;
        countdownTicks = ConfigManager.CONFIG.countdownSeconds * 20;
        betweenRoundTicks = 0;

        broadcastMessage("Find the Block Spiel startet!");
        bossBarManager.setColor(BossEvent.BossBarColor.RED);
        bossBarManager.setTitle("Find the Block startet in " + ConfigManager.CONFIG.countdownSeconds);
        bossBarManager.setVisible(true);
        playGlobalSound(SoundEvents.NOTE_BLOCK_PLING);
        refreshHud();
        return true;
    }

    public boolean stopGame(ServerPlayer sender) {
        if (state == GameState.WAITING && !paused) {
            sendMessage(sender, "Es läuft kein Find the Block Spiel.");
            return false;
        }

        paused = false;
        state = GameState.FINISHED;
        broadcastMessage("Find the Block Spiel beendet.");
        bossBarManager.setColor(BossEvent.BossBarColor.RED);
        bossBarManager.setTitle("SPIEL BEENDET");
        refreshHud();
        return true;
    }

    public boolean pauseGame(ServerPlayer sender) {
        if (state != GameState.ACTIVE) {
            sendMessage(sender, "Das Spiel kann jetzt nicht pausiert werden.");
            return false;
        }

        paused = true;
        state = GameState.WAITING;
        sendMessage(sender, "Spiel pausiert.");
        bossBarManager.setTitle("PAUSIERT");
        return true;
    }

    public boolean resumeGame(ServerPlayer sender) {
        if (!paused || state != GameState.WAITING) {
            sendMessage(sender, "Das Spiel ist nicht pausiert.");
            return false;
        }

        paused = false;
        state = GameState.ACTIVE;
        sendMessage(sender, "Spiel fortgesetzt.");
        if (currentTarget != null) {
            bossBarManager.setColor(BossEvent.BossBarColor.YELLOW);
            bossBarManager.setTitle("GESUCHT: " + toReadableName(currentTarget));
        }
        return true;
    }

    public boolean restartGame(ServerPlayer sender) {
        paused = false;
        state = GameState.WAITING;
        roundNumber = 0;
        currentIndex = -1;
        countdownTicks = 0;
        betweenRoundTicks = 0;
        roundAlreadyWon.set(false);
        currentTarget = null;
        sendMessage(sender, "Spiel neu gestartet.");
        bossBarManager.setVisible(false);
        refreshHud();
        return true;
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
        bossBarManager.setVisible(true);
        playGlobalSound(SoundEvents.NOTE_BLOCK_PLING);
        refreshHud();
    }

    public void nextRound() {
        if (state == GameState.ACTIVE || state == GameState.FOUND) {
            roundAlreadyWon.set(false);
            advanceToNextRound();
        }
    }

    private void advanceToNextRound() {
        currentIndex++;
        if (currentIndex >= blockOrder.size()) {
            if (ConfigManager.CONFIG.loopBlocks) {
                currentIndex = 0;
            } else {
                finishGame();
                return;
            }
        }

        if (roundNumber >= ConfigManager.CONFIG.maxRounds) {
            finishGame();
            return;
        }

        state = GameState.COUNTDOWN;
        countdownTicks = ConfigManager.CONFIG.countdownSeconds * 20;
        bossBarManager.setColor(BossEvent.BossBarColor.PURPLE);
        bossBarManager.setTitle("Nächste Runde in " + ConfigManager.CONFIG.countdownSeconds);
        refreshHud();
    }

    private void finishGame() {
        state = GameState.FINISHED;

        Team winner = determineWinner();
        if (winner != null) {
            broadcastMessage("SPIEL BEENDET");
            broadcastMessage("🏆 " + winner.name + " GEWINNT!");
            bossBarManager.setColor(BossEvent.BossBarColor.GREEN);
            bossBarManager.setTitle("🏆 " + winner.name + " GEWINNT!");
        } else {
            broadcastMessage("SPIEL BEENDET");
            broadcastMessage("UNENTSCHIEDEN");
            bossBarManager.setColor(BossEvent.BossBarColor.RED);
            bossBarManager.setTitle("UNENTSCHIEDEN");
        }

        broadcastScores();
        playGlobalSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.PLAYER_LEVELUP));
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
            } else if (score == bestScore && score > 0) {
                tie = true;
            }
        }

        if (bestScore <= 0) {
            return null;
        }
        return tie ? null : best;
    }

    public boolean handleBlockFound(ServerPlayer player, ServerLevel world, BlockState blockState) {
        return handleFound(player, world, blockState.getBlock());
    }

    public boolean handleBlockPlaced(ServerPlayer player, ServerLevel world, Block block) {
        return handleFound(player, world, block);
    }

    private boolean handleFound(ServerPlayer player, ServerLevel world, Block block) {
        if (state != GameState.ACTIVE || currentTarget == null) {
            return false;
        }

        if (!isDimensionAllowed(world)) {
            return false;
        }

        if (!isTargetBlock(block)) {
            return false;
        }

        if (!roundAlreadyWon.compareAndSet(false, true)) {
            return false;
        }

        Optional<Team> playerTeam = teamManager.getTeamForPlayer(player);
        boolean allowed = ConfigManager.CONFIG.allowUnassignedPlayers || playerTeam.isPresent();

        if (!allowed) {
            broadcastMessage(player.getName().getString() + " hat den Block gefunden, ist aber keinem Team zugewiesen!");
            state = GameState.FOUND;
            betweenRoundTicks = ConfigManager.CONFIG.betweenRoundSeconds * 20;
            playGlobalSound(SoundEvents.NOTE_BLOCK_BELL);
            return true;
        }

        if (playerTeam.isPresent()) {
            Team team = playerTeam.get();
            teamManager.addScore(team.id, 1);
            broadcastMessage("✓ " + toReadableName(currentTarget) + " GEFUNDEN!");
            broadcastMessage(player.getName().getString() + " hat den Block gefunden.");
            broadcastMessage("Team " + team.name + " erhält 1 Punkt!");
        } else {
            broadcastMessage("✓ " + toReadableName(currentTarget) + " GEFUNDEN!");
            broadcastMessage(player.getName().getString() + " hat den Block gefunden. Kein Team-Punkt (Spieler ohne Team).");
        }

        state = GameState.FOUND;
        betweenRoundTicks = ConfigManager.CONFIG.betweenRoundSeconds * 20;
        bossBarManager.setColor(BossEvent.BossBarColor.GREEN);
        bossBarManager.setTitle("✓ " + toReadableName(currentTarget) + " GEFUNDEN!");
        playGlobalSound(SoundEvents.NOTE_BLOCK_BELL);
        refreshHud();
        return true;
    }

    private boolean isTargetBlock(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !id.equals(currentTarget)) {
            return false;
        }
        return block == BuiltInRegistries.BLOCK.getValue(currentTarget);
    }

    private boolean isDimensionAllowed(ServerLevel world) {
        List<String> allowed = ConfigManager.CONFIG.allowedDimensions;
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        return allowed.contains(world.dimension().identifier().toString());
    }

    public void onPlayerJoin(ServerPlayer player) {
        if (bossBarManager != null && ConfigManager.CONFIG.showBossBar) {
            bossBarManager.addPlayer(player);
        }
        refreshHud();
    }

    public void onPlayerLeave(ServerPlayer player) {
        if (bossBarManager != null) {
            bossBarManager.removePlayer(player);
        }
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Find the Block\n");
        sb.append("\nStatus: ").append(stateName(state)).append("\n");
        if (state == GameState.WAITING && paused) {
            sb.append("PAUSIERT\n");
        }
        sb.append("Runde: ").append(roundNumber).append("/").append(ConfigManager.CONFIG.maxRounds).append("\n");
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
        switch (s) {
            case WAITING: return "WARTEND";
            case COUNTDOWN: return "COUNTDOWN";
            case ACTIVE: return "AKTIV";
            case FOUND: return "GEFUNDEN";
            case NEXT_ROUND: return "NÄCHSTE RUNDE";
            case FINISHED: return "BEENDET";
            default: return "UNBEKANNT";
        }
    }

    public void reloadConfigs() {
        blockOrder.clear();
        ConfigManager.reload();
        teamManager.reload();

        if (ConfigManager.BLOCKS != null) {
            for (String blockName : ConfigManager.BLOCKS.blocks) {
                try {
                    Identifier id = Identifier.tryParse(blockName);
                    if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                        blockOrder.add(id);
                    }
                } catch (Exception e) {
                    // Skip invalid blocks
                }
            }
        }
        refreshHud();
    }

    private void refreshHud() {
        if (scoreDisplay == null) {
            return;
        }
        if (ConfigManager.CONFIG.showScoreboard) {
            scoreDisplay.update(teamManager.getTeams(), teamManager.getScores());
        } else {
            scoreDisplay.clear();
        }
        if (bossBarManager != null) {
            bossBarManager.setVisible(ConfigManager.CONFIG.showBossBar && state != GameState.WAITING && !paused);
        }
    }

    private String toReadableName(Identifier id) {
        String path = id.getPath();
        return path.replace("_", " ");
    }

    private void broadcastMessage(String message) {
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }
    }

    private void sendMessage(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private void playGlobalSound(Holder<SoundEvent> sound) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
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
        }
    }

    // Getters
    public GameState getState() {
        return state;
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
}
