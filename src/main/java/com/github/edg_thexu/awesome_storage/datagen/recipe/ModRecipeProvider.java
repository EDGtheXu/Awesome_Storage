package com.github.edg_thexu.awesome_storage.datagen.recipe;

import com.github.edg_thexu.awesome_storage.core.registry.ModBlocks;
import com.github.edg_thexu.awesome_storage.core.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STORAGE_CORE_BLOCK.get())
                .pattern("AAA")
                .pattern("BDB")
                .pattern("CCC")
                .define('A',Items.IRON_INGOT)
                .define('B',Items.DIAMOND)
                .define('C', Blocks.OBSIDIAN)
                .define('D',Items.REDSTONE)
                .unlockedBy("has_diamond", has(Items.OBSIDIAN))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRAFTING_UNIT_BLOCK.get())
                .pattern("AAA")
                .pattern("BDB")
                .pattern("CCC")
                .define('A',Items.COPPER_INGOT)
                .define('B',Items.GOLD_INGOT)
                .define('C', Blocks.OBSIDIAN)
                .define('D',Items.REDSTONE)
                .unlockedBy("has_obsidian", has(Items.OBSIDIAN))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.BASE_REMOTE_CONTROLLER.get())
                .pattern(" AC")
                .pattern("ABA")
                .pattern("CA ")
                .define('A',Items.GLASS)
                .define('B',Items.REDSTONE)
                .define('C',Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot",has(Items.IRON_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.ADVANCE_REMOTE_CONTROLLER.get())
                .pattern(" AC")
                .pattern("ABA")
                .pattern("CA ")
                .define('A',Items.GLASS)
                .define('B',Items.REDSTONE)
                .define('C',Items.DIAMOND)
                .unlockedBy("has_diamond",has(Items.DIAMOND))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.FINAL_REMOTE_CONTROLLER.get())
                .pattern(" AC")
                .pattern("ABA")
                .pattern("CA ")
                .define('A',Items.GLASS)
                .define('B',Items.REDSTONE)
                .define('C',Items.NETHERITE_INGOT)
                .unlockedBy("has_netherite",has(Items.NETHERITE_INGOT))
                .save(recipeOutput);
    }

}
