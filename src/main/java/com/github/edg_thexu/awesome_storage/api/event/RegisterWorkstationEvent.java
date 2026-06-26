package com.github.edg_thexu.awesome_storage.api.event;

import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Event fired during mod initialization to register block-recipe type mappings.
 * Subscribe using {@link net.neoforged.bus.api.SubscribeEvent}.
 * <p>
 * Example usage in your mod:
 * <pre>
 * &#64;SubscribeEvent
 * public static void onRegisterWorkstations(RegisterWorkstationEvent event) {
 *     // Multiple blocks required to unlock one recipe
 *     event.register(RecipeType.SMELTING, Blocks.FURNACE, Blocks.BLAST_FURNACE);
 *     event.registerBlocks("minecraft:crafting", "minecraft:crafting_table");
 * }
 * </pre>
 */
public class RegisterWorkstationEvent extends Event implements IModBusEvent {

    private void put(RecipeType<Recipe<RecipeInput>> type, Block block) {
        Set<Block> existing = CraftConfig.ENABLED_RECIPES.get(type);
        if (existing != null && !existing.contains(block)) {
            existing.add(block);
        } else if (existing == null) {
            CraftConfig.ENABLED_RECIPES.put(type, new HashSet<>(List.of(block)));
        }
    }

    /** One recipe type requires ALL of the given blocks to be present. */
    @SuppressWarnings("unchecked")
    public void register(RecipeType<?> recipeType, Block... blocks) {
        var key = (RecipeType<Recipe<RecipeInput>>) recipeType;
        for (Block block : blocks) {
            put(key, block);
        }
    }

    /** One recipe type (by id) requires ALL of the given block ids to be present. */
    public void registerBlocks(String recipeTypeId, String... blockIds) {
        var type = (RecipeType<Recipe<RecipeInput>>) BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.parse(recipeTypeId));
        if (type == null) return;
        for (String blockId : blockIds) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
            if (block == null) continue;
            put(type, block);
        }
    }

}
