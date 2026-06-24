package com.github.edg_thexu.awesome_storage.api.filter;

import com.github.edg_thexu.awesome_storage.api.event.RegisterCategoryRuleEvent;
import com.github.edg_thexu.awesome_storage.api.event.RegisterSortRuleEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.fml.ModLoader;

import java.util.*;

public class FilterRuleRegistry {
    private static final List<SortRule> sortRules = new ArrayList<>();
    private static final List<CategoryRule> categoryRules = new ArrayList<>();
    private static boolean initialized = false;

    public static void registerSortRule(SortRule rule) {
        sortRules.add(rule);
    }

    public static void registerCategoryRule(CategoryRule rule) {
        categoryRules.add(rule);
    }

    public static List<SortRule> getSortRules() {
        return Collections.unmodifiableList(sortRules);
    }

    public static List<CategoryRule> getCategoryRules() {
        return Collections.unmodifiableList(categoryRules);
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Built-in defaults
        sortRules.add(new SortRule("By ID", Comparator.comparing(a -> BuiltInRegistries.ITEM.getKey(a.getItem()).toString())));
        sortRules.add(new SortRule("By Name", Comparator.comparing(a -> a.getDisplayName().getString())));
        sortRules.add(new SortRule("By Count", Comparator.comparingInt(ItemStack::getCount).reversed()));
        sortRules.add(new SortRule("By Rarity", Comparator.comparingInt(a -> a.getRarity().ordinal())));
        sortRules.add(new SortRule("By Mod", Comparator.comparing(a -> BuiltInRegistries.ITEM.getKey(a.getItem()).getNamespace())));
        sortRules.add(new SortRule("By Enchantments", (a, b) -> {
            int ea = EnchantmentHelper.getEnchantmentsForCrafting(a).size();
            int eb = EnchantmentHelper.getEnchantmentsForCrafting(b).size();
            if (ea != eb) return Integer.compare(ea, eb);
            return Integer.compare(a.getCount(), b.getCount());
        }));
        sortRules.add(new SortRule("By Damage", Comparator.comparingInt(a -> a.getDamageValue())));

        categoryRules.add(new CategoryRule("Weapon", s -> {
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
            return id.contains("sword") || id.contains("bow") || id.contains("crossbow") || id.contains("trident");
        }));
        categoryRules.add(new CategoryRule("Tool", s -> {
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
            return id.contains("pickaxe") || id.contains("axe") || id.contains("shovel") || id.contains("hoe");
        }));
        categoryRules.add(new CategoryRule("Block", s -> s.getItem() instanceof BlockItem));
        categoryRules.add(new CategoryRule("Material", s -> !(s.getItem() instanceof BlockItem)));

        // Allow other mods to add custom rules
        ModLoader.postEvent(new RegisterSortRuleEvent());
        ModLoader.postEvent(new RegisterCategoryRuleEvent());
    }
}
