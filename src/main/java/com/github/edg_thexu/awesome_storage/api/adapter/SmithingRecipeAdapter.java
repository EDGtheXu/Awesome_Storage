package com.github.edg_thexu.awesome_storage.api.adapter;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.List;

public class SmithingRecipeAdapter<R extends SmithingRecipe> extends AbstractMagicCraftRecipeAdapter<SmithingRecipeInput, R> {


    public SmithingRecipeAdapter(RecipeType<R> recipeType) {
        super(recipeType);
    }

    public SmithingRecipeAdapter(String recipeId){
        super(recipeId);
    }

    @Override
    public ItemStack getClientResult(RecipeHolder<R> recipe, HolderLookup.Provider registries){
        ItemStack res = recipe.value().getResultItem(registries);
        if(recipe.value() instanceof SmithingTransformRecipe transform){
            ItemStack itemstack = transform.base.getItems()[0].transmuteCopy(res.getItem(),res.getCount());
            itemstack.applyComponents(res.getComponentsPatch());
            return itemstack;
        } else if (recipe.value() instanceof SmithingTrimRecipe) {
            return ItemStack.EMPTY;
        }
        return res;
    }

    @Override
    public ItemStack getCraftResult(RecipeHolder<R> recipe, List<ItemStack> consumed, HolderLookup.Provider registries) {
        if (recipe.value() instanceof SmithingTransformRecipe transform) {
            ItemStack res = recipe.value().getResultItem(registries);
            if (res.isEmpty()) return ItemStack.EMPTY;

            // Base stack is the second consumed entry (index 1, after template)
            ItemStack baseStack = consumed.size() > 1 ? consumed.get(1) : ItemStack.EMPTY;
            if (!baseStack.isEmpty()) {
                // Preserve base item's DataComponents (enchantments, name, etc.) via transmuteCopy
                ItemStack result = baseStack.transmuteCopy(res.getItem(), res.getCount());
                result.applyComponents(res.getComponentsPatch());
                return result;
            }
            return res;
        }
        else if (recipe.value() instanceof SmithingTrimRecipe trim) {
            // For trim recipes, assemble the base item with the trim applied
            ItemStack templateStack = consumed.size() > 0 ? consumed.get(0) : ItemStack.EMPTY;
            ItemStack baseStack = consumed.size() > 1 ? consumed.get(1) : ItemStack.EMPTY;
            ItemStack additionStack = consumed.size() > 2 ? consumed.get(2) : ItemStack.EMPTY;

            if (!baseStack.isEmpty()) {
                SmithingRecipeInput input = new SmithingRecipeInput(templateStack, baseStack, additionStack);
                return recipe.value().assemble(input, registries);
            }
            return ItemStack.EMPTY;
        }
        return recipe.value().getResultItem(registries);
    }

    @Override
    public NonNullList<Ingredient> getIngredients(RecipeHolder<R> recipe) {
        var list = NonNullList.withSize(3, Ingredient.EMPTY);
        if(recipe.value() instanceof SmithingTransformRecipe transform){
            list.set(0, transform.template);
            list.set(1, transform.base);
            list.set(2, transform.addition);
            return list;
        }
        else if(recipe.value() instanceof SmithingTrimRecipe trim){
            list.set(0, trim.template);
            list.set(1, trim.base);
            list.set(2, trim.addition);
            return list;
        }
        return list;
    }

}
