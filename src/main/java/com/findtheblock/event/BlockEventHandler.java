package com.findtheblock.event;

import com.findtheblock.FindTheBlockMod;
import com.findtheblock.config.ConfigManager;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;

public class BlockEventHandler {
    public static void register() {
        String mode = currentMode();

        if ("BREAK".equals(mode)) {
            PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
                if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    FindTheBlockMod.GAME_MANAGER.handleBlockFound(serverPlayer, (ServerLevel) world, state);
                }
            });
        }

        if ("PLACE".equals(mode)) {
            BlockEvents.USE_ITEM_ON.register((stack, state, world, pos, player, hand, hitResult) -> {
                if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer
                        && stack.getItem() instanceof BlockItem blockItem) {
                    FindTheBlockMod.GAME_MANAGER.handleBlockPlaced(serverPlayer, (ServerLevel) world, blockItem.getBlock());
                }
                return InteractionResult.PASS;
            });
        }

        if ("INTERACT".equals(mode)) {
            UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
                if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    FindTheBlockMod.GAME_MANAGER.handleBlockFound(
                            serverPlayer,
                            (ServerLevel) world,
                            world.getBlockState(hitResult.getBlockPos())
                    );
                }
                return InteractionResult.PASS;
            });
        }
    }

    private static String currentMode() {
        if (ConfigManager.CONFIG == null) {
            return "BREAK";
        }
        return ConfigManager.CONFIG.triggerMode == null ? "BREAK" : ConfigManager.CONFIG.triggerMode.toUpperCase();
    }
}
