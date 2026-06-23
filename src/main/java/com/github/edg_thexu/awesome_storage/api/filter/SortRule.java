package com.github.edg_thexu.awesome_storage.api.filter;

import net.minecraft.world.item.ItemStack;

import java.util.Comparator;

public record SortRule(String name, Comparator<ItemStack> comparator) {
}
