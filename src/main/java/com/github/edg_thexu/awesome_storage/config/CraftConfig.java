package com.github.edg_thexu.awesome_storage.config;

import com.github.edg_thexu.awesome_storage.utils.Util;
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

import java.util.*;
import java.util.stream.Collectors;

public class CraftConfig extends AbstractJsonConfig{

    public static Map<RecipeType<?>, Set<Block>> ENABLED_RECIPES = new HashMap<>();
    private static final Set<Block> ENABLED_BLOCKS = new HashSet<>();
    public static boolean isEnabledBlock(Block block){
        return ENABLED_BLOCKS.contains(block);
    }

    public static void registerWorkstationBlock(String blockId, String... recipeTypeIds) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
        if (block == null || block == Blocks.AIR) return;
        for (String typeId : recipeTypeIds) {
            registerRecipeBlocks(typeId, blockId);
        }
    }

    public static void registerRecipeBlocks(String recipeTypeId, String... blockIds) {
        RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.parse(recipeTypeId));
        if (type == null) return;
        @SuppressWarnings("unchecked")
        RecipeType<Recipe<RecipeInput>> key = (RecipeType<Recipe<RecipeInput>>) type;
        for (String blockId : blockIds) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
            if (block == null || block == Blocks.AIR) continue;
            ENABLED_RECIPES.computeIfAbsent(key, k -> new HashSet<>()).add(block);
            if (!ENABLED_BLOCKS.contains(block)) ENABLED_BLOCKS.add(block);
        }
    }
    public boolean addRecipeBlock(String recipeTypeId, String blockId) {
        ResourceLocation rtId = ResourceLocation.parse(recipeTypeId);
        RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.get(rtId);
        if (type == null) return false;

        ResourceLocation bId = ResourceLocation.parse(blockId);
        Block block = BuiltInRegistries.BLOCK.get(bId);
        if (block == null || block == Blocks.AIR) return false;

        ENABLED_RECIPES.computeIfAbsent(type, k -> new HashSet<>()).add(block);
        if (!ENABLED_BLOCKS.contains(block)) ENABLED_BLOCKS.add(block);

        saveConfig();
        return true;
    }

    public void saveConfig() {
        List<RecipeAccess> list = new ArrayList<>();
        for (var entry : ENABLED_RECIPES.entrySet()) {
            RecipeType<?> rt = entry.getKey();
            ResourceLocation rtId = BuiltInRegistries.RECIPE_TYPE.getKey(rt);
            if (rtId == null) continue;
            List<ResourceLocation> blockIds = entry.getValue().stream()
                    .map(BuiltInRegistries.BLOCK::getKey)
                    .toList();
            list.add(new RecipeAccess(blockIds, rtId, Operation.ADD));
        }
        json.add("enabled_recipes", RecipeAccess.LIST_CODEC.encodeStart(JsonOps.INSTANCE, list).result().get());
        save();
    }

    private static final CraftConfig instance = new CraftConfig("magic_craft_config");
    public static CraftConfig INSTANCE() {return instance;}

    protected CraftConfig(String name) {
        super(name);
    }
    public CraftConfig(JsonObject config){
        super(config);
    }

    public void loadFrom(CraftConfig other) {
        var map = RecipeAccess.LIST_CODEC.decode(JsonOps.INSTANCE, other.rawConfig().get("enabled_recipes")).result().get();
        map.getFirst().stream().forEach(access ->{
            Operation operation = access.operation;
            RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(access.recipeType);
            switch (operation){
                case ADD -> {
                    if(!ENABLED_RECIPES.containsKey(recipeType)) {
                        ENABLED_RECIPES.put(recipeType, new HashSet<>());
                    }
                    Set<Block> set = ENABLED_RECIPES.get(recipeType);
                    Set<Block> added = access.blocks.stream().map(BuiltInRegistries.BLOCK::get).collect(Collectors.toSet());
                    set.addAll(added);
                    ENABLED_BLOCKS.addAll(added);
                }
                case REMOVE -> {
                    ENABLED_RECIPES.remove(access.recipeType);
                }
            }

        });
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
//        list.add(new RecipeAccess(List.of(BuiltInRegistries.BLOCK.getKey(Blocks.SMITHING_TABLE)), BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.SMITHING)));
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
                        new HashSet<>(access.blocks.stream().map(BuiltInRegistries.BLOCK::get).toList())));
        ENABLED_BLOCKS.addAll(ENABLED_RECIPES.values().stream().flatMap(Set::stream).toList());

    }

    public record RecipeAccess(List<ResourceLocation> blocks, ResourceLocation recipeType, Operation operation) {
        public RecipeAccess(List<ResourceLocation> blocks, ResourceLocation recipeType) {
            this(blocks, recipeType, Operation.ADD);
        }

        public static final Codec<RecipeAccess> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(RecipeAccess::blocks),
                ResourceLocation.CODEC.fieldOf("recipe_type").forGetter(RecipeAccess::recipeType),
                Operation.CODEC.optionalFieldOf("operation", Operation.ADD).forGetter(RecipeAccess::operation)
        ).apply(instance, RecipeAccess::new));

        public static final Codec<List<RecipeAccess>> LIST_CODEC = CODEC.listOf();

    }

    public enum Operation {
        ADD,
        REMOVE,
        REPLACE;

        public static final Codec<Operation> CODEC = Util.createEnumCodec(Operation.class);
    }

}
