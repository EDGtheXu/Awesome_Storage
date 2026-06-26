package com.github.edg_thexu.awesome_storage.api.adapter;

import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;
import java.util.*;

public class CommonRecipeAdapter<I extends RecipeInput,R extends Recipe<I>> extends  AbstractMagicCraftRecipeAdapter<I, R> {

    public CommonRecipeAdapter(RecipeType<R> recipeType){
        super(recipeType);
    }

    public CommonRecipeAdapter(String recipeId){
        super(recipeId);
    }

    public ItemStack getClientResult(RecipeHolder<R> recipe, HolderLookup.Provider registries){
        ItemStack res = recipe.value().getResultItem(registries);
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
        if (player == null) return;
        try {
            this.awardExp(player, getExperienceFromRecipe(recipe.value()));
        } catch (Exception ignored) {
        }
    }

    private float getExperienceFromRecipe(Object recipe) throws Exception {
        Class<?> clazz = recipe.getClass();
        if (CACHE_HAS_EXPERIENCE.containsKey(clazz)) {
            Optional<Field> field = CACHE_HAS_EXPERIENCE.get(clazz);
            if (field.isPresent()) {
                return field.get().getFloat(recipe);
            }
            return 0f;
        }
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField("experience");
                field.setAccessible(true);
                CACHE_HAS_EXPERIENCE.put(clazz, Optional.of(field));
                return field.getFloat(recipe);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        CACHE_HAS_EXPERIENCE.put(recipe.getClass(), Optional.empty());
        return 0f;
    }

    static final Map<Class<?>, Optional<Field>> CACHE_HAS_EXPERIENCE = new HashMap<>();

    @Override
    public void buildExtraInfo(QWidget container, RecipeHolder<R> recipe) {
        if (!(recipe.value() instanceof AbstractCookingRecipe)) return;
        try {
            float xp = getExperienceFromRecipe(recipe.value());
            if (xp > 0) {
                QLabel xpLabel = new QLabel(Component.literal("Experience: " + String.format("%.1f", xp) + " XP"));
                xpLabel.setTextColor(new QColor(0xFF88FF88));
                xpLabel.setParent(container);
                xpLabel.setGeometry(0, 0, container.width(), 12);
            }
        } catch (Exception ignored) {
        }
    }
}
