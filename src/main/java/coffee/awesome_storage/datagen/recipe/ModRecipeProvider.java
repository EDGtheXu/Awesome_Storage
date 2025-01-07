package coffee.awesome_storage.datagen.recipe;


import coffee.awesome_storage.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;


public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> recipeOutput) {

        //光萃台
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MAGIC_STORAGE_BLOCK.get())
                .pattern("AAA")
                .pattern("B B")
                .pattern("CCC")
                .define('A',Items.IRON_INGOT)
                .define('B',Items.GOLD_INGOT)
                .define('C', Blocks.OBSIDIAN)
                .unlockedBy("has_emerald",has(Items.EMERALD))
                .save(recipeOutput);

    }

}
