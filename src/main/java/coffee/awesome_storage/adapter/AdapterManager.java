package coffee.awesome_storage.adapter;

import net.minecraft.world.item.crafting.*;

import java.util.HashMap;
import java.util.Map;

public class AdapterManager {
    public static Map<RecipeType<?>, AbstractMagicCraftRecipeAdapter<RecipeInput, Recipe<RecipeInput>>> Adapters = new HashMap<>();
    public static AbstractMagicCraftRecipeAdapter<CraftingInput, CraftingRecipe> defaultAdapter;

    public static void registerAdapters(RecipeType<?> recipeType, AbstractMagicCraftRecipeAdapter adapter) {
        Adapters.put(recipeType, adapter);

    }

    public static void init(){
        defaultAdapter = new CommonRecipeAdapter<>((RecipeType.CRAFTING));
        registerAdapters(RecipeType.CRAFTING, defaultAdapter);
        registerAdapters(RecipeType.SMITHING, new SmithingRecipeAdapter<>(RecipeType.SMITHING));
    }

}
