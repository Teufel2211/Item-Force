package com.findtheblock.config;

import java.util.Arrays;
import java.util.List;

public class FindBlockConfig {
    public int countdownSeconds = 5;
    public int betweenRoundSeconds = 3;
    public int maxRounds = 10;
    public String triggerMode = "BREAK";
    public boolean showBossBar = true;
    public boolean showScoreboard = true;
    public boolean allowUnassignedPlayers = false;
    public boolean ignorePlacedDuringRound = true;
    public boolean ignoreCreativePlayers = true;
    public boolean loopBlocks = false;
    public List<String> allowedDimensions = Arrays.asList(
            "minecraft:overworld",
            "minecraft:the_nether",
            "minecraft:the_end"
    );

    public static FindBlockConfig createDefault() {
        FindBlockConfig cfg = new FindBlockConfig();
        cfg.validate();
        return cfg;
    }

    public void validate() {
        if (countdownSeconds < 1 || countdownSeconds > 300) {
            countdownSeconds = 5;
        }
        if (betweenRoundSeconds < 0 || betweenRoundSeconds > 300) {
            betweenRoundSeconds = 3;
        }
        if (maxRounds < 1 || maxRounds > 10000) {
            maxRounds = 10;
        }
        if (triggerMode == null) {
            triggerMode = "BREAK";
        } else {
            String m = triggerMode.toUpperCase();
            if (!m.equals("BREAK") && !m.equals("PLACE") && !m.equals("INTERACT")) {
                triggerMode = "BREAK";
            } else {
                triggerMode = m;
            }
        }
        if (allowedDimensions == null) {
            allowedDimensions = Arrays.asList(
                    "minecraft:overworld",
                    "minecraft:the_nether",
                    "minecraft:the_end"
            );
        }
    }

    public String normalizedTriggerMode() {
        if (triggerMode == null) return "BREAK";
        return triggerMode.toUpperCase();
    }
}
