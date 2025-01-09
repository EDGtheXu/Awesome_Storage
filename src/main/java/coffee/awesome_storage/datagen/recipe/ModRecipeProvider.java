package coffee.awesome_storage.datagen.recipe;


import coffee.awesome_storage.registry.ModBlocks;
import coffee.awesome_storage.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;


import java.util.concurrent.CompletableFuture;

import static coffee.awesome_storage.Awesome_storage.MODID;


public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {


        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MAGIC_STORAGE_BLOCK.get())
                .pattern("AAA")
                .pattern("B B")
                .pattern("CCC")
                .define('A',Items.IRON_INGOT)
                .define('B',Items.GOLD_INGOT)
                .define('C', Blocks.OBSIDIAN)
                .unlockedBy("has_obsidian",has(Items.EMERALD))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.REMOTE_CONTROLLER_1000.get())
                .pattern(" AA")
                .pattern("AAA")
                .pattern("AA ")
                .define('A',Items.COPPER_INGOT)
                .unlockedBy("has_cpopper",has(Items.COPPER_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.REMOTE_CONTROLLER_5000.get())
                .pattern(" AA")
                .pattern("AAA")
                .pattern("AA ")
                .define('A',Items.IRON_INGOT)
                .unlockedBy("has_iron",has(Items.IRON_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.REMOTE_CONTROLLER_10000.get())
                .pattern(" AA")
                .pattern("AAA")
                .pattern("AA ")
                .define('A',Items.GOLD_INGOT)
                .unlockedBy("has_goal",has(Items.GOLD_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.REMOTE_CONTROLLER_INF.get())
                .pattern(" AA")
                .pattern("AAA")
                .pattern("AA ")
                .define('A',Items.DIAMOND)
                .unlockedBy("has_diamond",has(Items.DIAMOND))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.REMOTE_CONTROLLER_THROUGH_CROSS_LEVEL.get())
                .pattern(" AA")
                .pattern("AAA")
                .pattern("AA ")
                .define('A',Items.NETHERITE_INGOT)
                .unlockedBy("has_netherite",has(Items.NETHERITE_INGOT))
                .save(recipeOutput);

    }

    protected static <T extends AbstractCookingRecipe> void cookRecipes(
            RecipeOutput recipeOutput, String cookingMethod, RecipeSerializer<T> cookingSerializer, AbstractCookingRecipe.Factory<T> recipeFactory, int cookingTime
    ) {
//        simpleCookingRecipe(recipeOutput, cookingMethod, cookingSerializer, recipeFactory, cookingTime, ModItems.BREAD_SWORD, ModItems.BREAD_SWORD_HOT, 0.35F);
//        simpleCookingRecipe(recipeOutput, cookingMethod, cookingSerializer, recipeFactory, cookingTime, ModItems.BREAD_SWORD_HOT, ModItems.BREAD_SWORD_VERY_HOT, 0.35F);
//

    }

    protected static <T extends AbstractCookingRecipe> void simpleCookingRecipe(
            RecipeOutput recipeOutput,
            String cookingMethod,
            RecipeSerializer<T> cookingSerializer,
            AbstractCookingRecipe.Factory<T> recipeFactory,
            int cookingTime,
            ItemLike material,
            ItemLike result,
            float experience
    ) {
        SimpleCookingRecipeBuilder.generic(Ingredient.of(material), RecipeCategory.FOOD, result, experience, cookingTime, cookingSerializer, recipeFactory)
                .unlockedBy(getHasName(material), has(material))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(MODID,getItemName(result) + "_from_" + cookingMethod));
    }
}
