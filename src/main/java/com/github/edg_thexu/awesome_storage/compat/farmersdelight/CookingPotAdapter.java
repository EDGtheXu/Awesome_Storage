package com.github.edg_thexu.awesome_storage.compat.farmersdelight;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.event.RecipeWorkstation;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import java.util.List;
import java.util.Set;

@RecipeWorkstation(
        blocks = "farmersdelight:cooking_pot",
        recipeTypes = "farmersdelight:cooking",
        adapter = CookingPotAdapter.class
)
public class CookingPotAdapter extends AbstractMagicCraftRecipeAdapter<RecipeWrapper, CookingPotRecipe> {

    public CookingPotAdapter() {
        super("farmersdelight:cooking");
    }

    @Override
    public ItemStack getClientResult(RecipeHolder<CookingPotRecipe> recipe, HolderLookup.Provider registries) {
        CookingPotRecipe pot = recipe.value();
        ItemStack result = pot.getResultItem(registries);
        if (result.isEmpty()) return ItemStack.EMPTY;
        return result;
    }

    @Override
    public ItemStack getCraftResult(RecipeHolder<CookingPotRecipe> recipe, List<ItemStack> consumed, HolderLookup.Provider registries) {
        CookingPotRecipe pot = recipe.value();
        return pot.getResultItem(registries).copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients(RecipeHolder<CookingPotRecipe> recipe) {
        CookingPotRecipe pot = recipe.value();
        return pot.getIngredients();
    }

    @Override
    public int getCookTime(RecipeHolder<CookingPotRecipe> recipe) {
        CookingPotRecipe pot = recipe.value();
        return pot.getCookTime();
    }

    @Override
    public float getSpeedMultiplier(RecipeHolder<CookingPotRecipe> recipe, Set<Block> workstations) {
        return 1.0f;
    }

    @Override
    public void onCraftFinish(RecipeHolder<CookingPotRecipe> recipe, List<ItemStack> consumed, Player player, Level level) {
        CookingPotRecipe pot = recipe.value();

        // Award experience
        float xp = pot.getExperience();
        if (xp > 0 && player != null) {
            this.awardExp(player, xp);
        }
        // Drop container item (bowl/plate) if any
        ItemStack container = pot.getOutputContainer();
        if (!container.isEmpty() && player != null) {
            if (!player.getInventory().add(container.copy())) {
                player.drop(container.copy(), false);
            }
        }
    }

    @Override
    public void buildExtraInfo(QWidget container, RecipeHolder<CookingPotRecipe> recipe) {
        CookingPotRecipe pot = recipe.value();
        float xp = pot.getExperience();
        if (xp > 0) {
            QLabel xpLabel = new QLabel(Component.literal("Experience: " + String.format("%.1f", xp) + " XP"));
            xpLabel.setTextColor(new QColor(0xFF88FF88));
            xpLabel.setParent(container);
            xpLabel.setGeometry(0, 0, container.width(), 12);
        }
    }
}
