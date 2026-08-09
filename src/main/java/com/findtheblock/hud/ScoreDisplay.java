package com.findtheblock.hud;

import com.findtheblock.team.Team;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.Collection;
import java.util.Map;

public class ScoreDisplay {
    private static final String OBJECTIVE_NAME = "ftbscore";

    private final ServerScoreboard scoreboard;

    public ScoreDisplay(MinecraftServer server) {
        this.scoreboard = server.getScoreboard();
    }

    public void update(Collection<Team> teams, Map<String, Integer> scores) {
        clear();

        Objective objective = scoreboard.addObjective(
                OBJECTIVE_NAME,
                ObjectiveCriteria.DUMMY,
                Component.literal("Find the Block"),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null
        );
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);

        for (Team team : teams) {
            int points = scores.getOrDefault(team.id, 0);
            String line = (team.name == null || team.name.isBlank()) ? team.id : team.name;
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(line), objective).set(points);
        }
    }

    public void clear() {
        Objective existing = scoreboard.getObjective(OBJECTIVE_NAME);
        if (existing != null) {
            scoreboard.removeObjective(existing);
        }
    }
}
