package com.github.edg_thexu.awesome_storage.utils;

import com.github.edg_thexu.awesome_storage.mix_util.IPlayer;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;


public class Util {

    public static void setStorageEntity(Player player, BlockEntity entity){
        ((IPlayer) player).awesomeStorage$setContainer(entity);
    }
    public static List<ItemStack> getStorageItems(Player player) {
        var entity = (MagicStorageBlockEntity) ((IPlayer) player).awesomeStorage$getContainer();
        if (entity == null) return null;
        return entity.getStoredItems();
    }

    public static MagicStorageBlockEntity getStorageEntity(Player player) {
        if (((IPlayer) player).awesomeStorage$getContainer() instanceof MagicStorageBlockEntity entity)
            return entity;
        return null;
    }

    public static Map<Item, Integer> stacks2ItemAmountMap(List<ItemStack> have) {
        Map<Item, Integer> haveIngredients = new HashMap<>();
        for (ItemStack stack : have) {
            Item item = stack.getItem();
            int count = stack.getCount();
            haveIngredients.put(item, haveIngredients.getOrDefault(item, 0) + count);
        }
        return haveIngredients;
    }


    public static boolean canCraftSimple(Map<Item, Integer> haveIngredients, NonNullList<Ingredient> ingredients) {
        boolean canCraft = true;
        for (Ingredient ingredient : ingredients) {
            boolean ingredientFound = false;
            if (ingredient.isEmpty()) continue;
            for (ItemStack ingredientStack : ingredient.getItems()) {
                Item ingredientItem = ingredientStack.getItem();
                int requiredCount = ingredientStack.getCount();
                if (haveIngredients.containsKey(ingredientItem) && haveIngredients.get(ingredientItem) >= requiredCount) {
                    haveIngredients.put(ingredientItem, haveIngredients.get(ingredientItem) - requiredCount);
                    ingredientFound = true;
                    break;
                }
            }
            if (!ingredientFound) {
                canCraft = false;
                break;
            }
        }
        return canCraft;
    }

    public static boolean canCraft(List<ItemStack> have, NonNullList<Ingredient> ingredients) {
        return doCraft(have.stream().map(ItemStack::copy).toList(), ingredients);
    }

    public static boolean doCraft(List<ItemStack> have, NonNullList<Ingredient> ingredients) {
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            boolean ingredientFound = false;
            for (ItemStack ingredientStack : ingredient.getItems()) {
                Item ingredientItem = ingredientStack.getItem();
                int requiredCount = ingredientStack.getCount();
                for (int i = 0; i < have.size(); i++) {
                    ItemStack stack = have.get(i);
                    if (stack.isEmpty()) continue;
                    if (stack.getItem() == ingredientItem) {
                        int count = stack.getCount();
                        if (count >= requiredCount) {
                            stack.shrink(requiredCount);
                            ingredientFound = true;
                            break;
                        } else {
                            requiredCount -= count;
                            have.remove(i);
                        }
                    }
                }
            }
            if (!ingredientFound) {
                return false;
            }
        }
        return true;
    }

    public static void unionItemStacks(List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack current = items.get(i);
            if (current.isEmpty()) continue;
            for (int j = i + 1; j < items.size(); j++) {
                ItemStack next = items.get(j);
                if (!next.isEmpty() && current.is(next.getItem()) && current.getCount() < current.getMaxStackSize()) {
                    int transfer = Math.min(next.getCount(), current.getMaxStackSize() - current.getCount());
                    current.grow(transfer);
                    next.shrink(transfer);
                    if (next.isEmpty()) items.set(j, ItemStack.EMPTY);
                }
            }
        }
    }

    public static void tryAddItemStackToItemStacks(ItemStack item, List<ItemStack> items) {
        for (ItemStack current : items) {
            if (!current.isEmpty() && current.is(item.getItem()) && current.getCount() < current.getMaxStackSize()) {
                int transfer = Math.min(item.getCount(), current.getMaxStackSize() - current.getCount());
                current.grow(transfer);
                item.shrink(transfer);
                if (item.isEmpty()) break;
            }
        }
    }

    public static<T extends Enum<T>> Codec<T> createEnumCodec(Class<T> enumClass) {
        return Codec.STRING.xmap(
                name->Enum.valueOf(enumClass, name.toUpperCase()),
                baker-> baker.name().toLowerCase(Locale.ROOT)
        );
    }

}
