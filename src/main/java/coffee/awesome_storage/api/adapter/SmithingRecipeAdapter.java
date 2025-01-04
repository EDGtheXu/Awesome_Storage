package coffee.awesome_storage.api.adapter;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

public class SmithingRecipeAdapter<R extends SmithingRecipe> extends AbstractMagicCraftRecipeAdapter<SmithingRecipeInput,R> {

    public SmithingRecipeAdapter(RecipeType<R> recipeType) {
        super(recipeType);
    }

    @Override
    public RecipeType<SmithingRecipe> getRecipe() {
        return RecipeType.SMITHING;
    }

    @Override
    public ItemStack getResult(RecipeHolder<R> recipe){
        ItemStack res = recipe.value().getResultItem(Minecraft.getInstance().level.registryAccess());

        if(!res.isEmpty()){
            return res;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients(RecipeHolder<R> recipe) {
        var list = NonNullList.withSize(3, Ingredient.EMPTY);
        if(recipe.value() instanceof SmithingTransformRecipe transform){
            list.set(0, transform.template);
            list.set(2, transform.base);
            list.set(1, transform.addition);
            return list;
        }
        else if(recipe.value() instanceof SmithingTrimRecipe trim){
            list.set(0, trim.template);
            list.set(1, trim.base);
            list.set(2, trim.addition);
            return list;
        }
        return list;
    }

}
