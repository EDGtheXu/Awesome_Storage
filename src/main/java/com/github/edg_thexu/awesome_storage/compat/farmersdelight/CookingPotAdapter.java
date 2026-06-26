package com.github.edg_thexu.awesome_storage.compat.farmersdelight;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.event.RecipeWorkstation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import java.util.List;
import java.util.Set;

@RecipeWorkstation(
        blocks = "farmersdelight:cooking_pot",
        recipeTypes = "farmersdelight:cooking",
        adapter = CookingPotAdapter.class
)
public class CookingPotAdapter extends AbstractMagicCraftRecipeAdapter<RecipeInput, Recipe<RecipeInput>> {

    public CookingPotAdapter() {
        super((RecipeType) BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.parse("farmersdelight:cooking")));
    }

    @Override
    public ItemStack getResult(RecipeHolder<Recipe<RecipeInput>> recipe) {
        Recipe<?> raw = recipe.value();
        if (raw instanceof CookingPotRecipe pot) {
            ItemStack result = pot.getResultItem(Minecraft.getInstance().level.registryAccess());
            if (result.isEmpty()) return ItemStack.EMPTY;
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getCraftResult(RecipeHolder<Recipe<RecipeInput>> recipe, List<ItemStack> consumed, HolderLookup.Provider registries) {
        Recipe<?> raw = recipe.value();
        if (raw instanceof CookingPotRecipe pot) {
            return pot.getResultItem(registries).copy();
        }
        return recipe.value().getResultItem(registries);
    }

    @Override
    public NonNullList<Ingredient> getIngredients(RecipeHolder<Recipe<RecipeInput>> recipe) {
        Recipe<?> raw = recipe.value();
        if (raw instanceof CookingPotRecipe pot) {
            return pot.getIngredients();
        }
        return NonNullList.create();
    }

    @Override
    public int getCookTime(RecipeHolder<Recipe<RecipeInput>> recipe) {
        Recipe<?> raw = recipe.value();
        if (raw instanceof CookingPotRecipe pot) {
            return pot.getCookTime();
        }
        return 0;
    }

    @Override
    public float getSpeedMultiplier(RecipeHolder<Recipe<RecipeInput>> recipe, Set<Block> workstations) {
        Recipe<?> raw = recipe.value();
        if (!(raw instanceof CookingPotRecipe)) return 1.0f;
        float mult = 0.5f;
        if (workstations.contains(net.minecraft.world.level.block.Blocks.FIRE)) mult *= 0.5f;
        if (workstations.contains(net.minecraft.world.level.block.Blocks.LAVA_CAULDRON)) mult *= 0.5f;
        if (workstations.contains(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("farmersdelight:cooking_pot")))) {
            mult *= 1.0f;
        }
        return Math.max(mult, 0.25f);
    }
}
