package com.findtheblock.config;

public class GameStateStore {
    public String state = "WAITING";
    public String stateBeforePause = null;
    public int roundNumber = 0;
    public int currentIndex = -1;
    public String currentTarget = null;
    public int countdownTicks = 0;
    public int betweenRoundTicks = 0;
    public boolean paused = false;

    public static GameStateStore createDefault() {
        return new GameStateStore();
    }

    public boolean isMidGame() {
        if (paused) return true;
        return "ACTIVE".equals(state) || "COUNTDOWN".equals(state) || "FOUND".equals(state);
    }
}
