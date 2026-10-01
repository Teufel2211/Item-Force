package com.findtheblock.team;

import com.findtheblock.FindTheBlockMod;
import com.findtheblock.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class TeamManager {
    private final Map<String, Team> teamById = new LinkedHashMap<>();
    private final Map<UUID, String> playerTeam = new HashMap<>();

    public TeamManager() {
        reload();
    }

    public void reload() {
        teamById.clear();
        if (ConfigManager.TEAMS != null) {
            if (ConfigManager.TEAMS.teams == null) {
                ConfigManager.TEAMS.teams = new ArrayList<>();
            }
            for (Team team : ConfigManager.TEAMS.teams) {
                if (team == null || team.id == null) continue;
                teamById.put(team.id, team);
                if (ConfigManager.SCORES != null) {
                    if (ConfigManager.SCORES.scores == null) {
                        ConfigManager.SCORES.scores = new HashMap<>();
                    }
                    ConfigManager.SCORES.scores.putIfAbsent(team.id, 0);
                }
            }
        }

        // Remove scores of deleted teams to avoid unbounded growth (M1)
        if (ConfigManager.SCORES != null && ConfigManager.SCORES.scores != null) {
            boolean changed = ConfigManager.SCORES.scores.keySet().retainAll(teamById.keySet());
            if (changed) {
                ConfigManager.saveScores();
            }
        }

        playerTeam.clear();
        boolean playersDirty = false;
        if (ConfigManager.PLAYERS != null) {
            if (ConfigManager.PLAYERS.players == null) {
                ConfigManager.PLAYERS.players = new HashMap<>();
            }
            for (Map.Entry<String, String> entry : new HashMap<>(ConfigManager.PLAYERS.players).entrySet()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(entry.getKey());
                } catch (IllegalArgumentException ignored) {
                    FindTheBlockMod.LOGGER.warn("Ungultige UUID in players.json: {}", entry.getKey());
                    ConfigManager.PLAYERS.players.remove(entry.getKey());
                    playersDirty = true;
                    continue;
                }
                // Drop assignments to non-existent teams (M3)
                if (!teamById.containsKey(entry.getValue())) {
                    FindTheBlockMod.LOGGER.warn("Spieler {} verweist auf unbekanntes Team {}, Zuweisung entfernt.", entry.getKey(), entry.getValue());
                    ConfigManager.PLAYERS.players.remove(entry.getKey());
                    playersDirty = true;
                    continue;
                }
                playerTeam.put(uuid, entry.getValue());
            }
            if (playersDirty) {
                ConfigManager.savePlayers();
            }
        }
    }

    public Collection<Team> getTeams() {
        return teamById.values();
    }

    public Optional<Team> getTeam(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(teamById.get(id));
    }

    public Optional<Team> getTeamForPlayer(UUID uuid) {
        if (uuid == null) return Optional.empty();
        String teamId = playerTeam.get(uuid);
        if (teamId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(teamById.get(teamId));
    }

    public boolean assignPlayer(UUID uuid, String teamId) {
        if (uuid == null || teamId == null) return false;
        Team team = teamById.get(teamId);
        if (team == null) {
            return false;
        }
        playerTeam.put(uuid, team.id);
        if (ConfigManager.PLAYERS != null) {
            if (ConfigManager.PLAYERS.players == null) {
                ConfigManager.PLAYERS.players = new HashMap<>();
            }
            ConfigManager.PLAYERS.players.put(uuid.toString(), team.id);
            ConfigManager.savePlayers();
        }
        return true;
    }

    public boolean removePlayer(UUID uuid) {
        if (uuid == null || !playerTeam.containsKey(uuid)) {
            return false;
        }
        playerTeam.remove(uuid);
        if (ConfigManager.PLAYERS != null && ConfigManager.PLAYERS.players != null) {
            ConfigManager.PLAYERS.players.remove(uuid.toString());
            ConfigManager.savePlayers();
        }
        return true;
    }

    public Map<String, Integer> getScores() {
        if (ConfigManager.SCORES != null && ConfigManager.SCORES.scores != null) {
            return ConfigManager.SCORES.scores;
        }
        return new HashMap<>();
    }

    public int getScore(String teamId) {
        if (ConfigManager.SCORES == null || ConfigManager.SCORES.scores == null || teamId == null) return 0;
        return ConfigManager.SCORES.scores.getOrDefault(teamId, 0);
    }

    public void addScore(String teamId, int points) {
        if (ConfigManager.SCORES == null || teamId == null) return;
        if (ConfigManager.SCORES.scores == null) {
            ConfigManager.SCORES.scores = new HashMap<>();
        }
        ConfigManager.SCORES.scores.put(teamId, getScore(teamId) + points);
        ConfigManager.saveScores();
    }

    public void resetScores() {
        if (ConfigManager.SCORES == null) return;
        if (ConfigManager.SCORES.scores == null) {
            ConfigManager.SCORES.scores = new HashMap<>();
        }
        for (String id : teamById.keySet()) {
            ConfigManager.SCORES.scores.put(id, 0);
        }
        ConfigManager.saveScores();
    }

    public void resetTeam(String teamId) {
        if (ConfigManager.SCORES == null || teamId == null) return;
        if (ConfigManager.SCORES.scores == null) {
            ConfigManager.SCORES.scores = new HashMap<>();
        }
        ConfigManager.SCORES.scores.put(teamId, 0);
        ConfigManager.saveScores();
    }

    public Optional<Team> getTeamForPlayer(ServerPlayer player) {
        if (player == null) return Optional.empty();
        return getTeamForPlayer(player.getUUID());
    }

    public boolean hasTeam(ServerPlayer player) {
        return getTeamForPlayer(player).isPresent();
    }
}
