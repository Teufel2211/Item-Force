package com.findtheblock.event;

import com.findtheblock.FindTheBlockMod;
import com.findtheblock.config.ConfigManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BlockEventHandler {
    public static void register() {
        // BREAK: always registered, mode checked inside so /reload works (C1)
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world == null || world.isClientSide()) return;
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!(world instanceof ServerLevel serverLevel)) return;
            if (!isMode("BREAK")) return;
            if (FindTheBlockMod.GAME_MANAGER == null) return;
            try {
                FindTheBlockMod.GAME_MANAGER.handleBlockFound(serverPlayer, serverLevel, state);
            } catch (Exception e) {
                FindTheBlockMod.LOGGER.warn("Fehler in BREAK-Handler", e);
            }
        });

        // PLACE + INTERACT via UseBlockCallback (C2: BlockEvents.USE_ITEM_ON existiert so nicht)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world == null || world.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(world instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
            if (FindTheBlockMod.GAME_MANAGER == null) return InteractionResult.PASS;
            if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
            if (!(hitResult instanceof BlockHitResult blockHit)) return InteractionResult.PASS;

            try {
                if (isMode("INTERACT")) {
                    FindTheBlockMod.GAME_MANAGER.handleBlockFound(serverPlayer, serverLevel, world.getBlockState(blockHit.getBlockPos()));
                } else if (isMode("PLACE")) {
                    var stack = player.getItemInHand(hand);
                    if (stack != null && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() != null) {
                        FindTheBlockMod.GAME_MANAGER.handleBlockPlaced(serverPlayer, serverLevel, blockItem.getBlock());
                    }
                }
            } catch (Exception e) {
                FindTheBlockMod.LOGGER.warn("Fehler in USE-Handler", e);
            }
            return InteractionResult.PASS;
        });
    }

    private static boolean isMode(String mode) {
        return mode.equals(currentMode());
    }

    public static String currentMode() {
        try {
            if (ConfigManager.CONFIG == null || ConfigManager.CONFIG.triggerMode == null) {
                return "BREAK";
            }
            return ConfigManager.CONFIG.triggerMode.toUpperCase();
        } catch (Exception e) {
            return "BREAK";
        }
    }
}
