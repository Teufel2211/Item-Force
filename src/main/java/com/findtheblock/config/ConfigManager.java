package com.findtheblock.config;

import com.findtheblock.FindTheBlockMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("findtheblock");
    private static final Path CONFIG_FILE = ROOT.resolve("config.json");
    private static final Path BLOCKS_FILE = ROOT.resolve("blocks.json");
    private static final Path TEAMS_FILE = ROOT.resolve("teams.json");
    private static final Path PLAYERS_FILE = ROOT.resolve("players.json");
    private static final Path SCORES_FILE = ROOT.resolve("scores.json");

    public static FindBlockConfig CONFIG;
    public static BlockConfig BLOCKS;
    public static TeamConfig TEAMS;
    public static PlayerConfig PLAYERS;
    public static ScoreConfig SCORES;

    public static void init() {
        try {
            Files.createDirectories(ROOT);
        } catch (IOException e) {
            FindTheBlockMod.LOGGER.error("Konnte config/findtheblock nicht erstellen", e);
        }
        CONFIG = loadConfig();
        BLOCKS = loadBlocks();
        TEAMS = loadTeams();
        PLAYERS = loadPlayers();
        SCORES = loadScores();
    }

    public static void reload() {
        CONFIG = loadConfig();
        BLOCKS = loadBlocks();
        TEAMS = loadTeams();
        PLAYERS = loadPlayers();
        SCORES = loadScores();
    }

    /** Alias kept for compatibility */
    public static void loadConfigs() {
        reload();
    }

    public static void saveAll() {
        saveConfig();
        saveBlocks();
        saveTeams();
        savePlayers();
        saveScores();
    }

    public static void saveConfig() { writeJson(CONFIG_FILE, CONFIG); }
    public static void saveBlocks() { writeJson(BLOCKS_FILE, BLOCKS); }
    public static void saveTeams() { writeJson(TEAMS_FILE, TEAMS); }
    public static void savePlayers() { writeJson(PLAYERS_FILE, PLAYERS); }
    public static void saveScores() { writeJson(SCORES_FILE, SCORES); }

    private static FindBlockConfig loadConfig() {
        FindBlockConfig cfg = readJson(CONFIG_FILE, FindBlockConfig.class, FindBlockConfig::createDefault, true);
        if (cfg != null) {
            cfg.validate();
        }
        return cfg;
    }

    private static BlockConfig loadBlocks() {
        return readJson(BLOCKS_FILE, BlockConfig.class, BlockConfig::createDefault, true);
    }

    private static TeamConfig loadTeams() {
        TeamConfig config = readJson(TEAMS_FILE, TeamConfig.class, TeamConfig::createDefault, true);
        config.fixMissingIds();
        return config;
    }

    private static PlayerConfig loadPlayers() {
        PlayerConfig cfg = readJson(PLAYERS_FILE, PlayerConfig.class, PlayerConfig::createDefault, true);
        if (cfg != null && cfg.players == null) {
            cfg.players = new HashMap<>();
        }
        return cfg;
    }

    private static ScoreConfig loadScores() {
        ScoreConfig cfg = readJson(SCORES_FILE, ScoreConfig.class, ScoreConfig::createDefault, true);
        if (cfg != null && cfg.scores == null) {
            cfg.scores = new HashMap<>();
        }
        return cfg;
    }

    private static <T> T readJson(Path path, Class<T> type, Supplier<T> defaultValues, boolean saveIfMissing) {
        if (Files.notExists(path)) {
            T value = defaultValues.get();
            if (saveIfMissing) {
                writeJson(path, value);
            }
            return value;
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            T parsed = GSON.fromJson(json, type);
            if (parsed == null) {
                throw new JsonSyntaxException("JSON ist leer oder ungultig");
            }
            if (parsed instanceof BlockConfig blockConfig) {
                blockConfig.validate();
            }
            if (parsed instanceof TeamConfig teamConfig) {
                teamConfig.fixMissingIds();
            }
            if (parsed instanceof FindBlockConfig findCfg) {
                findCfg.validate();
            }
            return parsed;
        } catch (IOException | JsonSyntaxException e) {
            FindTheBlockMod.LOGGER.warn("Fehler beim Laden von {}: {}. Standard wird verwendet, defekte Datei nach .bak gesichert.", path, e.getMessage());
            backupCorrupt(path);
            T value = defaultValues.get();
            writeJson(path, value);
            return value;
        }
    }

    private static void backupCorrupt(Path path) {
        try {
            if (Files.exists(path)) {
                Path bak = path.resolveSibling(path.getFileName().toString() + ".bak");
                Files.copy(path, bak, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {
        }
    }

    private static void writeJson(Path path, Object data) {
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            Files.writeString(temp, GSON.toJson(data), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            FindTheBlockMod.LOGGER.error("Fehler beim Speichern von {}", path, e);
        }
    }

    public static Optional<Block> parseBlock(String value) {
        try {
            Identifier id = Identifier.tryParse(value);
            if (id == null) return Optional.empty();
            return BuiltInRegistries.BLOCK.getOptional(id);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
