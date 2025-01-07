package coffee.awesome_storage.api.adapter;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Map;

public abstract class AbstractMagicCraftRecipeAdapter<C extends Container, R extends Recipe<C>> {
    RecipeType<R> recipeType;

    public AbstractMagicCraftRecipeAdapter(RecipeType<R> recipeType){
        this.recipeType = recipeType;
    }

    public void loadRecipe(Recipe<C> recipe , List<ItemStack> results, Map<Recipe ,AbstractMagicCraftRecipeAdapter> recipeMap)
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

    public RecipeType<? extends Recipe<C>> getRecipe(){
        return recipeType;
    }

    /**
     * @return null / EMPTY : not add to recipeMap
     */
    public abstract ItemStack getResult(Recipe<C> recipe);


    public abstract NonNullList<Ingredient> getIngredients(Recipe<C> recipe);

}
