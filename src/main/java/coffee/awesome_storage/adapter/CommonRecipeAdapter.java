package coffee.awesome_storage.adapter;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

public class CommonRecipeAdapter<I extends RecipeInput,R extends Recipe<I>> extends  AbstractMagicCraftRecipeAdapter<I,R> {

    public CommonRecipeAdapter(RecipeType<R> recipeType){
        super(recipeType);

    }

    public ItemStack getResult(RecipeHolder<R> recipe){

        ItemStack res = recipe.value().getResultItem(Minecraft.getInstance().level.registryAccess());
        if(!res.isEmpty() && !recipe.value().getIngredients().isEmpty()){
            return res;
        }
        return ItemStack.EMPTY;

    }

    @Override
    public NonNullList<Ingredient> getIngredients(RecipeHolder<R> recipe){
        return recipe.value().getIngredients();
    }

}
