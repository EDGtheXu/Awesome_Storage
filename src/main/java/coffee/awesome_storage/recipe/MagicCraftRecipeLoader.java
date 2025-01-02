package coffee.awesome_storage.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static coffee.awesome_storage.Awesome_storage.MODID;

public class MagicCraftRecipeLoader<T extends RecipeInput> {
    RecipeType<? extends Recipe<T>> recipeType;
    public MagicCraftRecipeLoader(RecipeType<? extends Recipe<T>> recipeType){
        this.recipeType = recipeType;
    }

    public RecipeType<? extends Recipe<T>> getRecipe(){
        return recipeType;
    }

    public void loadResults(RecipeHolder<? extends Recipe<?>> recipe, List<ItemStack> results, Map<ItemStack,RecipeHolder<?>> recipeMap){
        var value = recipe.value();
        Optional.of(value.getResultItem(null)).filter(f -> !f.isEmpty() && !value.getIngredients().isEmpty()).ifPresent(r->{
            results.add(r);
            recipeMap.put(r, recipe);
        });
    }

    public static Map<Block, RecipeType<Recipe<RecipeInput>>> ENABLED_RECIPES = new HashMap<>();

    public static void readFromJson(){
        Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(MODID);
        Path configFile = CONFIG_PATH.resolve("magic_craft_config.json");
        File file = configFile.toFile();
        try {
            Reader reader;
            JsonObject json;
            if (!file.exists()) {
                CONFIG_PATH.toFile().mkdirs();
                file.createNewFile();
                reader = new java.io.FileReader(file);
                json = new JsonObject();

                json.addProperty("minecraft:crafter","minecraft:crafting");

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Writer writer = new java.io.FileWriter(file);
                writer.write(gson.toJson(json));
                writer.close();
                reader.close();
            }else{
                reader = new java.io.FileReader(file);
                json = GsonHelper.parse(reader);
            }

            var map = json.asMap();
            for (var entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().getAsString();
                ENABLED_RECIPES.put(
                        BuiltInRegistries.BLOCK.get(ResourceLocation.parse(key)),
                        (RecipeType<Recipe<RecipeInput>>)BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.parse(value))
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
