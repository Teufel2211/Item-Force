package com.findtheblock.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.UUID;

public class BossBarManager {
    private final ServerBossEvent bossBar;

    public BossBarManager() {
        this.bossBar = new ServerBossEvent(
                UUID.randomUUID(),
                Component.literal("Find the Block"),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS
        );
        this.bossBar.setVisible(false);
        this.bossBar.setProgress(1.0F);
    }

    public void setTitle(String text) {
        bossBar.setName(Component.literal(text));
    }

    public void setColor(BossEvent.BossBarColor color) {
        bossBar.setColor(color);
    }

    public void setVisible(boolean visible) {
        bossBar.setVisible(visible);
    }

    public void addPlayer(ServerPlayer player) {
        bossBar.addPlayer(player);
    }

    public void removePlayer(ServerPlayer player) {
        bossBar.removePlayer(player);
    }

    public void removeAllPlayers() {
        bossBar.removeAllPlayers();
    }
}
