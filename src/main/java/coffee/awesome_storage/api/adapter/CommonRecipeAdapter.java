package coffee.awesome_storage.api.adapter;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

public class CommonRecipeAdapter<C extends Container,R extends Recipe<C>> extends  AbstractMagicCraftRecipeAdapter<C,R> {

    public CommonRecipeAdapter(RecipeType<R> recipeType){
        super(recipeType);

    }

    public ItemStack getResult(Recipe<C> recipe){

        ItemStack res = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        if(!res.isEmpty() && !recipe.getIngredients().isEmpty()){
            return res;
        }
        return ItemStack.EMPTY;

    }

    @Override
    public NonNullList<Ingredient> getIngredients(Recipe<C> recipe){
        return recipe.getIngredients();
    }

}
