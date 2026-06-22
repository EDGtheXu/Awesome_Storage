package coffee.awesome_storage.api.event;

import coffee.awesome_storage.config.CraftConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.fml.event.IModBusEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Event fired during mod initialization to register block-recipe type mappings.
 * Subscribe using {@link net.neoforged.bus.api.SubscribeEvent}.
 * <p>
 * Example usage in your mod:
 * <pre>
 * &#64;SubscribeEvent
 * public static void onRegisterWorkstations(RegisterWorkstationEvent event) {
 *     event.register("minecraft:furnace", "minecraft:smelting");
 *     event.register("minecraft:blast_furnace", "minecraft:blasting");
 * }
 * </pre>
 */
public class RegisterWorkstationEvent extends Event implements IModBusEvent {

    @SuppressWarnings("unchecked")
    public void register(Block block, RecipeType<?>... recipeTypes) {
        for (RecipeType<?> type : recipeTypes) {
            var key = (RecipeType<net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.RecipeInput>>) (Object) type;
            List<Block> existing = CraftConfig.ENABLED_RECIPES.get(key);
            if (existing != null && !existing.contains(block)) {
                existing.add(block);
            } else if (existing == null) {
                CraftConfig.ENABLED_RECIPES.put(key, new java.util.ArrayList<>(List.of(block)));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void register(String blockId, String... recipeTypeIds) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
        if (block == null) return;
        for (String typeId : recipeTypeIds) {
            var type = (RecipeType<net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.RecipeInput>>) (Object) BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.parse(typeId));
            if (type != null) {
                List<Block> existing = CraftConfig.ENABLED_RECIPES.get(type);
                if (existing != null && !existing.contains(block)) {
                    existing.add(block);
                } else if (existing == null) {
                    CraftConfig.ENABLED_RECIPES.put(type, new java.util.ArrayList<>(List.of(block)));
                }
            }
        }
    }

    public static void scanAnnotations(String scanPackage) {
        try {
            var cls = Class.forName(scanPackage);
            // Simple per-class scanning - caller should pass annotated class
        } catch (ClassNotFoundException ignored) {}
    }
}
