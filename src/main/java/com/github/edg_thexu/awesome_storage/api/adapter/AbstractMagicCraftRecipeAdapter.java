package com.github.edg_thexu.awesome_storage.api.adapter;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMagicCraftRecipeAdapter<I extends RecipeInput, R extends Recipe<I>> {
    RecipeType<R> recipeType;

    public AbstractMagicCraftRecipeAdapter(RecipeType<R> recipeType){
        this.recipeType = recipeType;
    }

    public AbstractMagicCraftRecipeAdapter(String recipeId){
        this((RecipeType) BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.parse(recipeId)));
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

    /**
     * @return cook time in ticks for this recipe. 0 means instant.
     */
    public int getCookTime(RecipeHolder<R> recipe) {
        return 0;
    }

    /**
     * @return speed multiplier based on available workstation blocks.
     * 1.0 = normal speed, 2.0 = twice as fast (half time).
     */
    public float getSpeedMultiplier(RecipeHolder<R> recipe, Set<Block> workstations) {
        return 1.0f;
    }

    /**
     * Called after a recipe is successfully crafted and its result has been stored.
     * Override to grant rewards such as experience, container items, or player effects.
     *
     * @param recipe   the recipe that was crafted
     * @param consumed the actual ItemStacks consumed
     * @param player   the player who crafted (null for queue-based crafting)
     * @param level    the level where crafting occurred
     */
    public void onCraftFinish(RecipeHolder<R> recipe, List<ItemStack> consumed, Player player, Level level) {
    }

    protected void awardExp(Player player, float exp) {
        int xp = this.calExp(exp);
        if(player.level() instanceof ServerLevel serverLevel) {
            ExperienceOrb.award(serverLevel, player.position(), xp);
        }
    }

    /**
     * Called when the recipe info panel builds its UI.
     * Override to add custom widgets (labels, slots, etc.) into the provided container.
     * The container is destroyed and recreated on each recipe change.
     *
     * @param container a QWidget to add child widgets into (use setParent)
     * @param recipe    the recipe being displayed
     */
    public void buildExtraInfo(com.github.edg_thexu.qtcraft_api.core.widget.QWidget container, RecipeHolder<R> recipe) {
    }

    protected int calExp(float exp) {
        int i = Mth.floor(exp);
        float f = Mth.frac(exp);
        if (f != 0.0F && Math.random() < (double)f) {
            i++;
        }
        return i;
    }

}
