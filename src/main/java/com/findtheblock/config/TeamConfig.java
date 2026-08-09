package com.findtheblock.config;

import com.findtheblock.team.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TeamConfig {
    public List<Team> teams = Arrays.asList(
            new Team("team1", "Team 1"),
            new Team("team2", "Team 2"),
            new Team("team3", "Team 3"),
            new Team("team4", "Team 4")
    );

    public static TeamConfig createDefault() {
        return new TeamConfig();
    }

    public void fixMissingIds() {
        if (teams == null) {
            teams = new ArrayList<>();
        }
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            if (team.id == null || team.id.isBlank()) {
                team.id = "team" + (i + 1);
            }
            if (team.name == null || team.name.isBlank()) {
                team.name = "Team " + (i + 1);
            }
        }
    }
}

