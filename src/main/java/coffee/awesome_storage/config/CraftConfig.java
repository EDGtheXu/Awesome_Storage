package coffee.awesome_storage.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static coffee.awesome_storage.Awesome_storage.MODID;

public class CraftConfig {
    public static Map<Block, RecipeType<Recipe<RecipeInput>>> ENABLED_RECIPES = new HashMap<>();

    public static void loadCraftConfig(){
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

                json.addProperty("minecraft:crafting_table","minecraft:crafting");
                json.addProperty("minecraft:furnace","minecraft:smelting");
                json.addProperty("minecraft:blast_furnace","minecraft:blasting");
                json.addProperty("minecraft:campfire","minecraft:campfire_cooking");
                json.addProperty("minecraft:smithing_table","minecraft:smithing");
                json.addProperty("minecraft:smoker","minecraft:smoking");


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
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
