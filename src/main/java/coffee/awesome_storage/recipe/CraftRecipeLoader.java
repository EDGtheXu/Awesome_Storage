package coffee.awesome_storage.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CraftRecipeLoader extends MagicCraftRecipeLoader<CraftingInput> {


    public CraftRecipeLoader(RecipeType<? extends Recipe<CraftingInput>> recipeType) {
        super(recipeType);
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipe() {
        return RecipeType.CRAFTING;
    }

    @Override
    public void loadResults(RecipeHolder<? extends Recipe<?>> recipe, List<ItemStack> results, Map<ItemStack,RecipeHolder<?>> recipeMap) {
        var value = recipe.value();

        Optional.of(value.getResultItem(null)).filter(f -> !f.isEmpty() && !value.getIngredients().isEmpty()).ifPresent(r->{
            results.add(r);
            recipeMap.put(r, recipe);
        });

    }



}
