package com.findtheblock.config;

import java.util.HashMap;
import java.util.Map;

public class ScoreConfig {
    public Map<String, Integer> scores = new HashMap<>();

    public static ScoreConfig createDefault() {
        return new ScoreConfig();
    }
}

