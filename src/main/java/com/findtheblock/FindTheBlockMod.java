package com.findtheblock;

import com.findtheblock.command.FindBlockCommand;
import com.findtheblock.config.ConfigManager;
import com.findtheblock.game.GameManager;
import com.findtheblock.event.BlockEventHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FindTheBlockMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("findtheblock");
    public static GameManager GAME_MANAGER;

    public static GameManager getGameManager() {
        return GAME_MANAGER;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("FindTheBlock mod initializing");

        ConfigManager.init();
        GAME_MANAGER = new GameManager();

        BlockEventHandler.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            FindBlockCommand.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (GAME_MANAGER != null) {
                GAME_MANAGER.onPlayerJoin(handler.player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (GAME_MANAGER != null) {
                GAME_MANAGER.onPlayerLeave(handler.player);
            }
        });
    }

    private void onServerStarted(MinecraftServer server) {
        if (GAME_MANAGER != null) {
            GAME_MANAGER.initialize(server);
            LOGGER.info("FindTheBlock game manager initialized");
        }
    }

    private void onServerTick(MinecraftServer server) {
        if (GAME_MANAGER != null) {
            GAME_MANAGER.tick(server);
        }
    }
}
