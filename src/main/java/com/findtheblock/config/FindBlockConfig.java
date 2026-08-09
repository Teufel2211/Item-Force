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
    public boolean loopBlocks = false;
    public List<String> allowedDimensions = Arrays.asList(
            "minecraft:overworld",
            "minecraft:the_nether",
            "minecraft:the_end"
    );

    public static FindBlockConfig createDefault() {
        return new FindBlockConfig();
    }
}
