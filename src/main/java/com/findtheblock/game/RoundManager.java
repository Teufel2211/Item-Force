package com.findtheblock.game;

import net.minecraft.resources.Identifier;

import java.util.concurrent.atomic.AtomicBoolean;

public class RoundManager {
    public GameState state = GameState.WAITING;
    public int roundNumber = 0;
    public int currentIndex = -1;
    public Identifier currentTarget;
    public int countdownTicks = 0;
    public int betweenRoundTicks = 0;
    public final AtomicBoolean roundAlreadyWon = new AtomicBoolean(false);

    public void reset() {
        state = GameState.WAITING;
        roundNumber = 0;
        currentIndex = -1;
        currentTarget = null;
        countdownTicks = 0;
        betweenRoundTicks = 0;
        roundAlreadyWon.set(false);
    }
}
