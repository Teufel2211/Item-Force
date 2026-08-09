package com.findtheblock.game;

import com.findtheblock.team.TeamManager;

import java.util.Map;

public class ScoreManager {
    private final TeamManager teamManager;

    public ScoreManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void resetScores() {
        teamManager.getScores().clear();
        teamManager.getTeams().forEach(team -> teamManager.getScores().put(team.id, 0));
    }

    public void addPoint(String teamId) {
        teamManager.addScore(teamId, 1);
    }

    public void resetTeam(String teamId) {
        teamManager.getScores().put(teamId, 0);
    }

    public Map<String, Integer> getScores() {
        return teamManager.getScores();
    }

    public int getScore(String teamId) {
        return teamManager.getScore(teamId);
    }
}

