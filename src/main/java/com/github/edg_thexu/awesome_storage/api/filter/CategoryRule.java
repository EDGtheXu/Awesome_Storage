package com.github.edg_thexu.awesome_storage.api.filter;

import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public record CategoryRule(String name, Predicate<ItemStack> predicate) {
}
