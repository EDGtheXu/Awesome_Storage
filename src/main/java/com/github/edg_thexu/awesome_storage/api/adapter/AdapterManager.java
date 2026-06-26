package com.github.edg_thexu.awesome_storage.api.adapter;

import com.github.edg_thexu.awesome_storage.api.event.RegisterAdapterEvent;
import net.minecraft.world.item.crafting.*;
import net.neoforged.fml.ModLoader;

import java.util.HashMap;
import java.util.Map;

import static com.github.edg_thexu.awesome_storage.config.CraftConfig.ENABLED_RECIPES;

public class AdapterManager {
    public static Map<RecipeType<?>, AbstractMagicCraftRecipeAdapter<RecipeInput, Recipe<RecipeInput>>> Adapters = new HashMap<>();

    public static void registerAdapters(RecipeType<?> recipeType, AbstractMagicCraftRecipeAdapter adapter) {
        Adapters.put(recipeType, adapter);
    }

    public static void init(){

        ModLoader.postEvent(new RegisterAdapterEvent());

        for(var recipeType : ENABLED_RECIPES.keySet()){
            if(!Adapters.containsKey(recipeType))
                Adapters.put(recipeType, new CommonRecipeAdapter(recipeType));
        }
    }
}
