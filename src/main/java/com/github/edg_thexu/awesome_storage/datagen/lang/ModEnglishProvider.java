package com.github.edg_thexu.awesome_storage.datagen.lang;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.MODID;
import static com.github.edg_thexu.awesome_storage.AwesomeStorage.englishProviders;


public class ModEnglishProvider extends LanguageProvider {

    public ModEnglishProvider(PackOutput output) {
        super(output, MODID, "en_us");
    }
    public static String toTitleCase(String raw) {
        return Arrays.stream(raw
                        .split("[_/]"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
    @Override
    protected void addTranslations() {
        add("container.awesome_storage.magic_storage", "Magic Storage");
        add("creativetab.magic_storage.cards", "Magic Storage");

        add("magic_storage.missing_ingredient", "Missing Ingredient");
        add("magic_storage.can_craft", "Click to craft");
        add("magic_storage_screen.storage", "Storage");
        add("magic_storage_screen.craft", "Craft");

        add("magic_craft.no_access", "Push Work Table to Access Crafting");

        add("magic_storage.message.no_component", "This item has no component: ");
        add("magic_storage.message.too_far", "Container is Too Far!");

        add("magic_storage.tooltip.controller_range", "Communication Distance: ");
        add("magic_storage.tooltip.distance", "Distance: ");
        add("magic_storage.tooltip.block_pos", "Your Position: ");
        add("magic_storage.tooltip.error_level", "No Signal");

        add("magic_storage.deposit_btn.tooltip", "Deposit All - LClick: Store All, Ctrl+LClick: Quick Stack, RClick: Refill");

        add("config.jade.plugin_awesome_storage.magic_block", "Magic Storage");

        add("magic_storage_screen.search", "Search...");
        add("magic_storage_screen.save", "Save");
        add("magic_storage_screen.capacity_format", "Capacity: %s/%s");
        add("magic_storage_screen.craftable", "Craftable");
        add("magic_storage_screen.all", "All");
        add("magic_storage_screen.max", "Max");
        add("magic_storage_screen.reset", "Reset");
        add("magic_storage_screen.select_item", "Select an item");
        add("magic_storage_screen.output", "Output:");
        add("magic_storage_screen.ingredients", "Ingredients:");
        add("magic_storage_screen.stations", "Stations:");
        add("magic_storage_screen.in_storage", "In Storage:");
        add("magic_storage_screen.controller", "Controller");
        add("magic_storage_screen.rename", "Rename");
        add("magic_storage_screen.default", "Default");
        add("magic_storage_screen.stackable", "Stackable");
        add("magic_storage_screen.non_stackable", "Non-stackable");
        add("magic_storage_screen.all_mods", "All Mods");

        add("magic_storage_screen.queue", "Queue");
        add("magic_storage_screen.queue_title", "Crafting Queue");
        add("magic_storage_screen.queue_idle", "Idle");
        add("magic_storage_screen.queue_clear_all", "Clear All");

        add("magic_storage_screen.stats_title", "Statistics");
        add("magic_storage_screen.stats_overview", "Overview");
        add("magic_storage_screen.stats_unique_types", "Item Types:");
        add("magic_storage_screen.stats_total_count", "Total Items:");
        add("magic_storage_screen.stats_top_items", "Top Items");
        add("magic_storage_screen.stats_empty", "No items stored");
        add("magic_storage_screen.stats_mods", "Mod Distribution");

        englishProviders.forEach(a->a.accept(this));
    }

}