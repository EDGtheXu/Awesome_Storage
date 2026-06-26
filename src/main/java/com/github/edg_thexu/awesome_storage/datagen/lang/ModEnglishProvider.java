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
        add("container.awesome_storage.storage_array", "Storage Array");

        add("creativetab.awesome_storage.cards", "Awesome Storage");

        add("awesome_storage.missing_ingredient", "Missing Ingredient");
        add("awesome_storage.can_craft", "Click to craft");
        add("awesome_storage.magic_storage_screen.storage", "Storage");
        add("awesome_storage.magic_storage_screen.craft", "Craft");

        add("awesome_storage.message.no_component", "This item has no component: ");
        add("awesome_storage.message.too_far", "Container is Too Far!");

        add("awesome_storage.tooltip.controller_range", "Communication Distance: ");
        add("awesome_storage.tooltip.distance", "Distance: ");
        add("awesome_storage.tooltip.block_pos", "Your Position: ");
        add("awesome_storage.tooltip.error_level", "No Signal");

        add("awesome_storage.deposit_btn.tooltip", "Deposit All - LClick: Store All, Ctrl+LClick: Quick Stack, RClick: Refill");

        add("awesome_storage.magic_storage_screen.search", "Search...");
        add("awesome_storage.magic_storage_screen.store_all", "全部存入");
        add("awesome_storage.magic_storage_screen.capacity_format", "Capacity: %s/%s");
        add("awesome_storage.magic_storage_screen.craftable", "Craftable");
        add("awesome_storage.magic_storage_screen.all", "All");
        add("awesome_storage.magic_storage_screen.favorites_only", "Favorites Only");
        add("awesome_storage.auto_stock_btn.tooltip", "Auto Stock - Right-click items to set stock target");
        add("awesome_storage.magic_storage_screen.auto_stock_title", "Stock Settings");
        add("awesome_storage.magic_storage_screen.auto_stock_desc", "Set Count: Left-1 Right-10 Middle-64");
        add("awesome_storage.magic_storage_screen.auto_stock_empty", "No stock targets set");
        add("awesome_storage.magic_storage_screen.max", "Max");
        add("awesome_storage.magic_storage_screen.reset", "Reset");
        add("awesome_storage.magic_storage_screen.select_item", "Select an item");
        add("awesome_storage.magic_storage_screen.output", "Output");
        add("awesome_storage.magic_storage_screen.ingredients", "Ingredients");
        add("awesome_storage.magic_storage_screen.stations", "Stations");
        add("awesome_storage.magic_storage_screen.in_storage", "In Storage");
        add("awesome_storage.magic_storage_screen.controller", "Controller");
        add("awesome_storage.magic_storage_screen.rename", "Rename");
        add("awesome_storage.magic_storage_screen.default", "Default");
        add("awesome_storage.magic_storage_screen.stackable", "Stackable");
        add("awesome_storage.magic_storage_screen.non_stackable", "Non-stackable");
        add("awesome_storage.magic_storage_screen.all_mods", "All Mods");
        add("awesome_storage.magic_storage_screen.display", "Display");
        add("awesome_storage.magic_storage_screen.filter", "Filters");
        add("awesome_storage.magic_storage_screen.leftmenu", "Left Menu");

        add("awesome_storage.magic_storage_screen.queue", "Queue");
        add("awesome_storage.magic_storage_screen.queue_title", "Crafting Queue");
        add("awesome_storage.magic_storage_screen.queue_idle", "Idle");
        add("awesome_storage.magic_storage_screen.queue_pending", "Pending");
        add("awesome_storage.magic_storage_screen.queue_empty", "No items in queue");
        add("awesome_storage.magic_storage_screen.queue_status", "Queue Slots: %s");
        add("awesome_storage.magic_storage_screen.queue_clear_all", "Clear All");
        add("awesome_storage.magic_storage_screen.queue_pause_all", "Pause All");
        add("awesome_storage.magic_storage_screen.queue_resume_all", "Resume All");

        add("awesome_storage.magic_storage_screen.stats_title", "Statistics");
        add("awesome_storage.magic_storage_screen.stats_overview", "Overview");
        add("awesome_storage.magic_storage_screen.stats_unique_types", "Item Types:");
        add("awesome_storage.magic_storage_screen.stats_total_count", "Total Items:");
        add("awesome_storage.magic_storage_screen.stats_top_items", "Top Items");
        add("awesome_storage.magic_storage_screen.stats_empty", "No items stored");
        add("awesome_storage.magic_storage_screen.stats_mods", "Mod Distribution");

        add("awesome_storage.magic_storage_screen.upgrade_title", "Upgrades");
        add("awesome_storage.magic_storage_screen.upgrade_slot", "Network Card");
        add("awesome_storage.magic_storage_screen.upgrade_range", "Range: %s");
        add("awesome_storage.magic_storage_screen.upgrade_frequency", "Frequency");
        add("awesome_storage.magic_storage_screen.upgrade_freq_hint", "Enter frequency...");
        add("awesome_storage.magic_storage_screen.upgrade_set", "Set");
        add("awesome_storage.magic_storage_screen.upgrade_connected", "Connected Cores");
        add("awesome_storage.magic_storage_screen.upgrade_no_cores", "No cores found");

        add("awesome_storage.tooltip.network_range", "Range: %s");
        add("awesome_storage.magic_storage_screen.craft_info_title", "Recipe Info");
        add("awesome_storage.magic_storage_screen.craft_info_empty", "No recipes configured");
        add("awesome_storage.magic_storage_screen.craft_info_unsupported", "Unsupported Recipe Types");
        add("awesome_storage.tooltip.queue_upgrade", "Adds %s queue slot(s)");

        add("awesome_storage.magic_storage_screen.queue_upgrade_title", "Queue Upgrades");
        add("awesome_storage.magic_storage_screen.queue_upgrade_total", "Total queue slots: %s");

        add("config.jade.plugin_awesome_storage.magic_block", "Magic Storage");
        englishProviders.forEach(a->a.accept(this));
    }

}