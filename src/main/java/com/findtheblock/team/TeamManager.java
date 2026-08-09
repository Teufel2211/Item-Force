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
            for (Team team : ConfigManager.TEAMS.teams) {
                teamById.put(team.id, team);
                if (ConfigManager.SCORES != null) {
                    ConfigManager.SCORES.scores.putIfAbsent(team.id, 0);
                }
            }
        }

        playerTeam.clear();
        if (ConfigManager.PLAYERS != null) {
            for (Map.Entry<String, String> entry : ConfigManager.PLAYERS.players.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    playerTeam.put(uuid, entry.getValue());
                } catch (IllegalArgumentException ignored) {
                    FindTheBlockMod.LOGGER.warn("Ungültige UUID in players.json: {}", entry.getKey());
                }
            }
        }
    }

    public Collection<Team> getTeams() {
        return teamById.values();
    }

    public Optional<Team> getTeam(String id) {
        return Optional.ofNullable(teamById.get(id));
    }

    public Optional<Team> getTeamForPlayer(UUID uuid) {
        String teamId = playerTeam.get(uuid);
        if (teamId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(teamById.get(teamId));
    }

    public boolean assignPlayer(UUID uuid, String teamId) {
        Team team = teamById.get(teamId);
        if (team == null) {
            return false;
        }
        playerTeam.put(uuid, team.id);
        ConfigManager.PLAYERS.players.put(uuid.toString(), team.id);
        ConfigManager.savePlayers();
        return true;
    }

    public boolean removePlayer(UUID uuid) {
        if (!playerTeam.containsKey(uuid)) {
            return false;
        }
        playerTeam.remove(uuid);
        ConfigManager.PLAYERS.players.remove(uuid.toString());
        ConfigManager.savePlayers();
        return true;
    }

    public Map<String, Integer> getScores() {
        return ConfigManager.SCORES != null ? ConfigManager.SCORES.scores : new HashMap<>();
    }

    public int getScore(String teamId) {
        return ConfigManager.SCORES != null ? ConfigManager.SCORES.scores.getOrDefault(teamId, 0) : 0;
    }

    public void addScore(String teamId, int points) {
        if (ConfigManager.SCORES != null) {
            ConfigManager.SCORES.scores.put(teamId, getScore(teamId) + points);
            ConfigManager.saveScores();
        }
    }

    public Optional<Team> getTeamForPlayer(ServerPlayer player) {
        return getTeamForPlayer(player.getUUID());
    }

    public boolean hasTeam(ServerPlayer player) {
        return getTeamForPlayer(player).isPresent();
    }
}
