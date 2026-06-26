package com.github.edg_thexu.awesome_storage.compat.farmersdelight;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.event.RecipeWorkstation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipeInput;

import java.util.ArrayList;
import java.util.List;

@RecipeWorkstation(
        blocks = "farmersdelight:cutting_board",
        recipeTypes = "farmersdelight:cutting",
        adapter = CuttingBoardAdapter.class
)
public class CuttingBoardAdapter extends AbstractMagicCraftRecipeAdapter<CuttingBoardRecipeInput, CuttingBoardRecipe> {

    public CuttingBoardAdapter() {
        super("farmersdelight:cutting");
    }

    @Override
    public ItemStack getResult(RecipeHolder<CuttingBoardRecipe> recipe) {
        CuttingBoardRecipe cut = recipe.value();
        ItemStack result = cut.getResultItem(Minecraft.getInstance().level.registryAccess());
        if (result.isEmpty()) return ItemStack.EMPTY;
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients(RecipeHolder<CuttingBoardRecipe> recipe) {
        CuttingBoardRecipe cut = recipe.value();
        return cut.getIngredients();
    }

    @Override
    public List<ItemStack> getExtraInfoItems(RecipeHolder<CuttingBoardRecipe> recipe) {
        CuttingBoardRecipe cut = recipe.value();
        List<ItemStack> tools = new ArrayList<>();
        Ingredient tool = cut.getTool();
        if (tool != null) {
            for (ItemStack stack : tool.getItems()) {
                if (!stack.isEmpty()) {
                    tools.add(stack);
                    break;
                }
            }
        }
        return tools;
    }

    @Override
    public String getExtraInfoLabel(RecipeHolder<CuttingBoardRecipe> recipe) {
        return "awesome_storage.craft_info.tool";
    }
}
