package com.github.edg_thexu.awesome_storage.api.event;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.adapter.CommonRecipeAdapter;

import java.lang.annotation.*;

/**
 * Annotation to register a block-recipe type mapping with adapter.
 * Place on any class in your mod. The class is discovered automatically
 * via {@link RecipeWorkstationScanner#scanAll()} at startup.
 * <p>
 * Example:
 * <pre>
 * &#64;RecipeWorkstation(blocks = {"minecraft:furnace", "minecraft:blast_furnace"}, recipeTypes = "minecraft:smelting")
 * &#64;RecipeWorkstation(blocks = "minecraft:crafting_table", recipeTypes = "minecraft:crafting")
 * public class MyWorkstations {}
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RecipeWorkstations.class)
public @interface RecipeWorkstation {
    String[] blocks();
    String[] recipeTypes();
    Class<? extends AbstractMagicCraftRecipeAdapter> adapter() default CommonRecipeAdapter.class;
}
