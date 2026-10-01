package com.findtheblock.config;

import com.findtheblock.FindTheBlockMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockConfig {
    public List<String> blocks = Arrays.asList(
            "minecraft:oak_log",
            "minecraft:stone",
            "minecraft:coal_ore",
            "minecraft:iron_ore",
            "minecraft:gold_ore",
            "minecraft:redstone_ore",
            "minecraft:lapis_ore",
            "minecraft:diamond_ore",
            "minecraft:emerald_ore",
            "minecraft:ancient_debris"
    );

    public List<String> invalidBlocks = new ArrayList<>();

    public void validate() {
        invalidBlocks.clear();
        for (String identifier : blocks) {
            try {
                Identifier id = Identifier.tryParse(identifier);
                if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
                    invalidBlocks.add(identifier);
                }
            } catch (Exception ex) {
                invalidBlocks.add(identifier);
            }
        }
        if (!invalidBlocks.isEmpty()) {
            for (String invalid : invalidBlocks) {
                FindTheBlockMod.LOGGER.warn("WARNUNG: Ungültiger Block in blocks.json: {}", invalid);
            }
        }
    }

    public List<Identifier> getValidBlockIds() {
        List<Identifier> valid = new ArrayList<>();
        for (String identifier : blocks) {
            try {
                Identifier id = Identifier.tryParse(identifier);
                if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                    valid.add(id);
                }
            } catch (Exception ignored) {
            }
        }
        return valid;
    }

    public static BlockConfig createDefault() {
        return new BlockConfig();
    }
}
