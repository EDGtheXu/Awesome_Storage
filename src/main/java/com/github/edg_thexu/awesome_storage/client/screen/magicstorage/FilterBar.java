package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.api.filter.FilterRuleRegistry;
import com.github.edg_thexu.qtcraft_api.core.QSizePolicy;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.slots.SlotKeyConsumer;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QComboBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// ========================================================================
// Shared filter bar (sort / category / stack / mod + filter + sort logic)
// ========================================================================
class FilterBar {
    final QComboBox sortCombo;
    final QComboBox categoryCombo;
    final QComboBox stackCombo;
    final QComboBox modCombo;

    FilterBar(QHBoxLayout row, Object owner, String keyPrefix, Runnable onRefresh) {
        sortCombo = new QComboBox();
        sortCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
        sortCombo.addItem(Component.translatable("awesome_storage.magic_storage_screen.default").getString());
        for (var rule : FilterRuleRegistry.getSortRules()) sortCombo.addItem(rule.name());
        sortCombo.setFixedHeight(16);
        sortCombo.connect(QComboBox.CURRENT_INDEX_CHANGED, owner, new SlotKeyConsumer<>(keyPrefix + "s", (self, idx) -> onRefresh.run()));
        row.addWidget(sortCombo, 1);

        categoryCombo = new QComboBox();
        categoryCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
        categoryCombo.addItem(Component.translatable("awesome_storage.magic_storage_screen.all").getString());
        for (var rule : FilterRuleRegistry.getCategoryRules()) categoryCombo.addItem(rule.name());
        categoryCombo.setFixedHeight(16);
        categoryCombo.connect(QComboBox.CURRENT_INDEX_CHANGED, owner, new SlotKeyConsumer<>(keyPrefix + "c", (self, idx) -> onRefresh.run()));
        row.addWidget(categoryCombo, 1);

        stackCombo = new QComboBox();
        stackCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
        stackCombo.addItem(Component.translatable("awesome_storage.magic_storage_screen.all").getString());
        stackCombo.addItem(Component.translatable("awesome_storage.magic_storage_screen.stackable").getString());
        stackCombo.addItem(Component.translatable("awesome_storage.magic_storage_screen.non_stackable").getString());
        stackCombo.setFixedHeight(16);
        stackCombo.connect(QComboBox.CURRENT_INDEX_CHANGED, owner, new SlotKeyConsumer<>(keyPrefix + "t", (self, idx) -> onRefresh.run()));
        row.addWidget(stackCombo, 1);

        modCombo = new QComboBox();
        modCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
        modCombo.addItem(Component.translatable("awesome_storage.magic_storage_screen.all_mods").getString());
        java.util.TreeSet<String> allMods = new java.util.TreeSet<>();
        for (var item : BuiltInRegistries.ITEM) {
            allMods.add(BuiltInRegistries.ITEM.getKey(item).getNamespace());
        }
        for (String mod : allMods) modCombo.addItem(mod);
        modCombo.setFixedHeight(16);
        modCombo.connect(QComboBox.CURRENT_INDEX_CHANGED, owner, new SlotKeyConsumer<>(keyPrefix + "m", (self, idx) -> onRefresh.run()));
        row.addWidget(modCombo, 1);
    }

    List<ItemStack> apply(List<ItemStack> items, String search) {
        String catFilter = categoryCombo.currentText();
        String sortText = sortCombo.currentText();
        String stackText = stackCombo.currentText();
        String modFilter = modCombo.currentText();

        // Parse search prefix: @modname, #itemid, otherwise name search
        String searchLower = search.toLowerCase();
        String modSearch = null;
        String idSearch = null;
        String nameSearch = null;
        if (searchLower.startsWith("@")) {
            modSearch = searchLower.substring(1);
        } else if (searchLower.startsWith("#")) {
            idSearch = searchLower.substring(1);
        } else if (!searchLower.isEmpty()) {
            nameSearch = searchLower;
        }

        List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack s : items) {
            if (s.isEmpty()) continue;
            var key = BuiltInRegistries.ITEM.getKey(s.getItem());
            if (nameSearch != null && !s.getDisplayName().getString().toLowerCase().contains(nameSearch)) continue;
            if (modSearch != null && !key.getNamespace().contains(modSearch)) continue;
            if (idSearch != null && !key.getPath().contains(idSearch)) continue;
            // Category filter (registered rules)
            if (!catFilter.equals(Component.translatable("awesome_storage.magic_storage_screen.all").getString())) {
                boolean match = false;
                for (var rule : FilterRuleRegistry.getCategoryRules()) {
                    if (catFilter.equals(rule.name()) && rule.predicate().test(s)) {
                        match = true;
                        break;
                    }
                }
                if (!match) continue;
            }
            if (stackText.equals(Component.translatable("awesome_storage.magic_storage_screen.stackable").getString()) && !s.isStackable()) continue;
            if (stackText.equals(Component.translatable("awesome_storage.magic_storage_screen.non_stackable").getString()) && s.isStackable()) continue;
            if (!modFilter.equals(Component.translatable("awesome_storage.magic_storage_screen.all_mods").getString()) && !key.getNamespace().equals(modFilter))
                continue;
            filtered.add(s);
        }
        // Sort by registered rule
        if (!sortText.equals(Component.translatable("awesome_storage.magic_storage_screen.default").getString())) {
            for (var rule : FilterRuleRegistry.getSortRules()) {
                if (sortText.equals(rule.name())) {
                    filtered.sort(rule.comparator());
                    break;
                }
            }
        }
        return filtered;
    }
}
