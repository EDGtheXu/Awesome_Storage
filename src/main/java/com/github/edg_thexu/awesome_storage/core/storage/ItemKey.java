package com.github.edg_thexu.awesome_storage.core.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public record ItemKey(Item item, DataComponentPatch components) {

    public static ItemKey from(ItemStack stack) {
        return new ItemKey(stack.getItem(), stack.getComponentsPatch());
    }

    public ItemStack toStack() {
        ItemStack stack = new ItemStack(item);
        stack.applyComponents(components);
        return stack;
    }

    public ItemStack toStack(int count) {
        ItemStack stack = toStack();
        stack.setCount(count);
        return stack;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        return (CompoundTag)toStack(1).saveOptional(registries);
    }

    public static ItemKey load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parseOptional(registries, tag);
        if (stack.isEmpty()) return new ItemKey(net.minecraft.world.item.Items.AIR, DataComponentPatch.EMPTY);
        return from(stack);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemKey itemKey)) return false;
        return item == itemKey.item && Objects.equals(components, itemKey.components);
    }

    @Override
    public int hashCode() {
        int result = item.hashCode();
        result = 31 * result + (components != null ? components.hashCode() : 0);
        return result;
    }
}
