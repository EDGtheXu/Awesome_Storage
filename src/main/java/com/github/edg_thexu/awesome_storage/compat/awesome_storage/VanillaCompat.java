package com.github.edg_thexu.awesome_storage.compat.awesome_storage;

import com.github.edg_thexu.awesome_storage.api.adapter.SmithingRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.event.RecipeWorkstation;
import com.github.edg_thexu.awesome_storage.api.event.RecipeWorkstations;

/**
 * Default vanilla workstation registrations for the storage system.
 * Discovered automatically via annotation scanning.
 */
@RecipeWorkstations(value = {
        @RecipeWorkstation(blocks = "minecraft:crafting_table", recipeTypes = "minecraft:crafting"),
        @RecipeWorkstation(blocks = "minecraft:furnace", recipeTypes = "minecraft:smelting"),
        @RecipeWorkstation(blocks = "minecraft:blast_furnace", recipeTypes = "minecraft:blasting"),
        @RecipeWorkstation(blocks = "minecraft:campfire", recipeTypes = "minecraft:campfire_cooking"),
        @RecipeWorkstation(blocks = "minecraft:smoker", recipeTypes = "minecraft:smoking"),
        @RecipeWorkstation(blocks = "minecraft:smithing_table", recipeTypes = "minecraft:smithing", adapter = SmithingRecipeAdapter.class)
})
public class VanillaCompat {}
