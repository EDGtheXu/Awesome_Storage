package coffee.awesome_storage.api.adapter;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.List;
import java.util.Map;

public abstract class AbstractMagicCraftRecipeAdapter<I extends RecipeInput, R extends Recipe<I>> {
    RecipeType<R> recipeType;

    public AbstractMagicCraftRecipeAdapter(RecipeType<R> recipeType){
        this.recipeType = recipeType;
    }

    public void loadRecipe(RecipeHolder recipe , List<ItemStack> results, Map<RecipeHolder<?>,AbstractMagicCraftRecipeAdapter> recipeMap)
    {
        try {
            ItemStack result = getResult(recipe);
            if(result!=null && !result.isEmpty()){
                results.add(result);
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


    public abstract NonNullList<Ingredient> getIngredients(RecipeHolder<R> recipe);

}
