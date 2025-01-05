package coffee.awesome_storage.config;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CraftConfig extends AbstractJsonConfig{

    public static Map<RecipeType<Recipe<RecipeInput>>,List<Block>> ENABLED_RECIPES = new HashMap<>();
    private static final List<Block> ENABLED_BLOCKS = new ArrayList<>();
    public static boolean isEnabledBlock(Block block){
        return ENABLED_BLOCKS.contains(block);
    }
    private static final CraftConfig instance = new CraftConfig("magic_craft_config");
    public static CraftConfig INSTANCE() {return instance;}

    protected CraftConfig(String name) {
        super(name);
    }
    public CraftConfig(JsonObject config){
        super(config);
    }

    @Override
    protected JsonObject defaultConfig() {
        JsonObject json = new JsonObject();
        List<RecipeAccess> list = new ArrayList<>();
        list.add(new RecipeAccess(List.of(
                BuiltInRegistries.BLOCK.getKey(Blocks.CRAFTING_TABLE)
        ),
                BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.CRAFTING)));
        list.add(new RecipeAccess(List.of(BuiltInRegistries.BLOCK.getKey(Blocks.FURNACE)), BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.SMELTING)));
        list.add(new RecipeAccess(List.of(BuiltInRegistries.BLOCK.getKey(Blocks.BLAST_FURNACE)), BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.BLASTING)));
        list.add(new RecipeAccess(List.of(BuiltInRegistries.BLOCK.getKey(Blocks.CAMPFIRE)), BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.CAMPFIRE_COOKING)));
        list.add(new RecipeAccess(List.of(BuiltInRegistries.BLOCK.getKey(Blocks.SMITHING_TABLE)), BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.SMITHING)));
        list.add(new RecipeAccess(List.of(BuiltInRegistries.BLOCK.getKey(Blocks.SMOKER)), BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.SMOKING)));
        json.add("enabled_recipes", RecipeAccess.LIST_CODEC.encodeStart(JsonOps.INSTANCE,list).result().get());
        return json;
    }

    @Override
    protected void initConfig(JsonObject json) {
        var map = RecipeAccess.LIST_CODEC.decode(JsonOps.INSTANCE, json.get("enabled_recipes")).result().get();
        map.getFirst().stream().forEach(access ->
                ENABLED_RECIPES.put(
                        (RecipeType<Recipe<RecipeInput>>) BuiltInRegistries.RECIPE_TYPE.get(access.recipeType),
                        access.blocks.stream().map(BuiltInRegistries.BLOCK::get).toList()));
        ENABLED_BLOCKS.addAll(ENABLED_RECIPES.values().stream().flatMap(List::stream).toList());

    }

    public record RecipeAccess(List<ResourceLocation> blocks, ResourceLocation recipeType){
        public static final Codec<RecipeAccess> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.listOf().fieldOf("blocks").forGetter(RecipeAccess::blocks),
                ResourceLocation.CODEC.fieldOf("recipe_type").forGetter(RecipeAccess::recipeType)
        ).apply(instance, RecipeAccess::new));

        public static final Codec<List<RecipeAccess>> LIST_CODEC = CODEC.listOf();

    }

}
