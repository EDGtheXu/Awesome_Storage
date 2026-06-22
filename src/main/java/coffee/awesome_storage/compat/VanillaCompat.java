package coffee.awesome_storage.compat;

import coffee.awesome_storage.api.adapter.SmithingRecipeAdapter;
import coffee.awesome_storage.api.event.RecipeWorkstation;

/**
 * Default vanilla workstation registrations for the storage system.
 * Discovered automatically via annotation scanning.
 */
@RecipeWorkstation(block = "minecraft:crafting_table", recipeTypes = "minecraft:crafting")
@RecipeWorkstation(block = "minecraft:furnace", recipeTypes = "minecraft:smelting", adapter = SmithingRecipeAdapter.class)
@RecipeWorkstation(block = "minecraft:blast_furnace", recipeTypes = "minecraft:blasting")
@RecipeWorkstation(block = "minecraft:campfire", recipeTypes = "minecraft:campfire_cooking")
@RecipeWorkstation(block = "minecraft:smoker", recipeTypes = "minecraft:smoking")
public class VanillaCompat {}
