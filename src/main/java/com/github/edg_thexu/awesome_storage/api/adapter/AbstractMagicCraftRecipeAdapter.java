package com.github.edg_thexu.awesome_storage.api.adapter;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.List;
import java.util.Map;

public abstract class AbstractMagicCraftRecipeAdapter<I extends RecipeInput, R extends Recipe<I>> {
    RecipeType<R> recipeType;

    public AbstractMagicCraftRecipeAdapter(RecipeType<R> recipeType){
        this.recipeType = recipeType;
    }

    public void loadRecipe(RecipeHolder recipe, List<ItemStack> results, Map<RecipeHolder<?>, AbstractMagicCraftRecipeAdapter> recipeMap) {
        try {
            ItemStack result = getResult(recipe);
            if(result !=null && !result.isEmpty()){
                results.add(result.copy());
                recipeMap.put(recipe, this);
            }
        }catch (Exception e1){
            e1.printStackTrace();
        }
    }

    public RecipeType<? extends Recipe<I>> getRecipe(){
        return recipeType;
    }

    /**
     * @return null / EMPTY : not add to recipeMap
     */
    public abstract ItemStack getResult(RecipeHolder<R> recipe);

    /**
     * Compute the actual craft result using the real consumed ItemStacks.
     * Default implementation uses the recipe's default result.
     * Override this in adapters that need component inheritance (e.g. SmithingRecipeAdapter).
     *
     * @param recipe   the recipe being crafted
     * @param consumed the actual ItemStacks consumed (with their DataComponents intact)
     * @param registries registry lookup for the current level
     * @return the crafted ItemStack
     */
    public ItemStack getCraftResult(RecipeHolder<R> recipe, List<ItemStack> consumed, HolderLookup.Provider registries) {
        return recipe.value().getResultItem(registries);
    }

    public abstract NonNullList<Ingredient> getIngredients(RecipeHolder<R> recipe);

}
