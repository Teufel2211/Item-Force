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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScoreDisplay {
    private static final String OBJECTIVE_NAME = "ftbscore";

    private final ServerScoreboard scoreboard;

    public ScoreDisplay(MinecraftServer server) {
        this.scoreboard = server.getScoreboard();
    }

    public void update(Collection<Team> teams, Map<String, Integer> scores) {
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

        Map<String, String> holderByTeam = holderNames(teams);
        for (Team team : teams) {
            if (team == null || team.id == null) continue;
            int points = scores.getOrDefault(team.id, 0);
            String holder = holderByTeam.getOrDefault(team.id, team.id);
            try {
                scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(holder), objective).set(points);
            } catch (Exception ignored) {
            }
        }

        // Objective is ours, so unknown holders are safe to drop (also migrates old team-id holders)
        try {
            java.util.Set<String> current = new java.util.HashSet<>(holderByTeam.values());
            for (ScoreHolder holder : scoreboard.getTrackedPlayers()) {
                if (holder != null && !current.contains(holder.getScoreboardName())) {
                    scoreboard.resetSinglePlayerScore(holder, objective);
                }
            }
        } catch (Exception ignored) {
            // stale entries are harmless
        }
    }

    static Map<String, String> holderNames(Collection<Team> teams) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Team t : teams) {
            if (t == null || t.id == null) continue;
            String base = (t.name == null || t.name.isBlank()) ? t.id : t.name;
            if (base.length() > 36) base = base.substring(0, 36);
            String holder = base;
            if (out.containsValue(holder)) {
                String suffix = " (" + t.id + ")";
                int room = 40 - suffix.length();
                String cut = base.length() > room ? base.substring(0, Math.max(0, room)) : base;
                holder = cut + suffix;
                if (holder.length() > 40) holder = holder.substring(0, 40);
            }
            int k = 2;
            while (out.containsValue(holder)) {
                String s = " #" + k++;
                holder = holder.length() + s.length() > 40 ? holder.substring(0, 40 - s.length()) + s : holder + s;
            }
            out.put(t.id, holder);
        }
        return out;
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
