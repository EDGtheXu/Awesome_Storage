package com.github.edg_thexu.awesome_storage.api.adapter;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Set;

public class CommonRecipeAdapter<I extends RecipeInput,R extends Recipe<I>> extends  AbstractMagicCraftRecipeAdapter<I,R> {

    public CommonRecipeAdapter(RecipeType<R> recipeType){
        super(recipeType);
    }

    public ItemStack getResult(RecipeHolder<R> recipe){
        ItemStack res = recipe.value().getResultItem(Minecraft.getInstance().level.registryAccess());
        if(!res.isEmpty() && !recipe.value().getIngredients().isEmpty()){
            return res;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients(RecipeHolder<R> recipe){
        return recipe.value().getIngredients();
    }

    @Override
    public int getCookTime(RecipeHolder<R> recipe) {
        if (recipe.value() instanceof AbstractCookingRecipe cooking) {
            return cooking.cookingTime;
        }
        return 0;
    }

    @Override
    public float getSpeedMultiplier(RecipeHolder<R> recipe, Set<Block> workstations) {
        if (!(recipe.value() instanceof AbstractCookingRecipe)) {
            return 1.0f;
        }
        float mult = 1.0f;
        // Blast Furnace accelerates smelting recipes by 2x
        if (recipe.value().getType() == RecipeType.SMELTING && workstations.contains(Blocks.BLAST_FURNACE)) {
            mult *= 2.0f;
        }
        // Smoker accelerates smoking recipes by 2x
        if (recipe.value().getType() == RecipeType.SMOKING && workstations.contains(Blocks.SMOKER)) {
            mult *= 2.0f;
        }
        return mult;
    }

    @Override
    public void onCraftFinish(RecipeHolder<R> recipe, List<ItemStack> consumed, Player player, Level level) {
        if (player != null) {
            try {
                var f = recipe.value().getClass().getField("experience");
                float xp = f.getFloat(recipe.value());
                if (xp > 0) player.giveExperiencePoints(Math.round(xp));
            } catch (Exception ignored) {
            }
        }
    }

}
