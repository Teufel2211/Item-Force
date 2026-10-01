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
        // Reuse objective instead of clear()+recreate to avoid flicker/packet spam (M4)
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = scoreboard.addObjective(
                    OBJECTIVE_NAME,
                    ObjectiveCriteria.DUMMY,
                    Component.literal("Find the Block"),
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    null
            );
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
        }

        if (teams == null) return;
        if (scores == null) scores = Map.of();

        // Stable holder = team.id (no collisions, no 40-char limit issues).
        // Sidebar therefore shows IDs (team1) - stable by design.
        for (Team team : teams) {
            if (team == null || team.id == null) continue;
            int points = scores.getOrDefault(team.id, 0);
            try {
                scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(team.id), objective).set(points);
            } catch (Exception ignored) {
            }
        }

        // Remove stale entries of deleted teams
        try {
            for (String holder : scoreboard.getTrackedPlayers()) {
                boolean known = false;
                for (Team t : teams) {
                    if (t != null && holder.equals(t.id)) {
                        known = true;
                        break;
                    }
                }
                if (!known && holder != null && holder.startsWith("team")) {
                    scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(holder), objective);
                }
            }
        } catch (Exception ignored) {
            // getTrackedPlayers may differ across mappings - stale entries are harmless
        }
    }

    public void clear() {
        Objective existing = scoreboard.getObjective(OBJECTIVE_NAME);
        if (existing != null) {
            try {
                scoreboard.removeObjective(existing);
            } catch (Exception ignored) {
            }
        }
        try {
            if (scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) != null
                    && OBJECTIVE_NAME.equals(scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR).getName())) {
                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
            }
        } catch (Exception ignored) {
        }
    }
}
