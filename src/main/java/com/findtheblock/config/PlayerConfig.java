package com.findtheblock.config;

import java.util.HashMap;
import java.util.Map;

public class PlayerConfig {
    public Map<String, String> players = new HashMap<>();

    public static PlayerConfig createDefault() {
        return new PlayerConfig();
    }
}

