package coffee.awesome_storage.api.adapter;

import coffee.awesome_storage.api.event.RegisterAdapterEvent;
import net.minecraft.world.item.crafting.*;
import net.neoforged.fml.ModLoader;

import java.util.HashMap;
import java.util.Map;

import static coffee.awesome_storage.config.CraftConfig.ENABLED_RECIPES;

public class AdapterManager {
    public static Map<RecipeType<?>, AbstractMagicCraftRecipeAdapter<RecipeInput, Recipe<RecipeInput>>> Adapters = new HashMap<>();
    public static AbstractMagicCraftRecipeAdapter<CraftingInput, CraftingRecipe> defaultAdapter;

    public static void registerAdapters(RecipeType<?> recipeType, AbstractMagicCraftRecipeAdapter adapter) {
        Adapters.put(recipeType, adapter);
    }

    public static void init(){
        defaultAdapter = new CommonRecipeAdapter<>((RecipeType.CRAFTING));

        ModLoader.postEvent(new RegisterAdapterEvent());

        for(var recipeType : ENABLED_RECIPES.values()){
            if(!Adapters.containsKey(recipeType))
                Adapters.put(recipeType, new CommonRecipeAdapter<>(recipeType));
        }
    }
}
