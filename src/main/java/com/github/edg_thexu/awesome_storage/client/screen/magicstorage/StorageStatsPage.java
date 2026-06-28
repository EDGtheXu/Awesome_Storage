package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class StorageStatsPage extends QWidget {

    private final QSmoothScrollArea scroll;
    private QWidget content;

    public StorageStatsPage() {
        QVBoxLayout vl = new QVBoxLayout(this);
        vl.setSpacing(4);
        vl.setContentsMargins(5, 5, 5, 5);

        scroll = new QSmoothScrollArea();
        scroll.setWidgetResizable(true);
        vl.addWidget(scroll, 1);
    }

    public void refresh() {
        MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
        if (be == null) return;

        content = new QWidget();
        QVBoxLayout layout = new QVBoxLayout(content);
        layout.setSpacing(8);
        layout.setContentsMargins(5, 5, 5, 5);

        List<ItemStack> items = be.getStoredItems();
        int usedSlots = be.getUsedSlots();
        int totalSlots = be.getTotalSlots();
        int uniqueTypes = items.size();
        int totalCount = 0;
        for (ItemStack s : items) totalCount += s.getCount();

        // === Overview Section ===
        section(layout, "awesome_storage.magic_storage_screen.stats_overview");

        addStatRow(layout, "awesome_storage.magic_storage_screen.stats_unique_types", String.valueOf(uniqueTypes));
        addStatRow(layout, "awesome_storage.magic_storage_screen.stats_total_count", String.valueOf(totalCount));

        // Capacity bar
        QWidget capBar = new CapacityStatBar(usedSlots, totalSlots);
        capBar.setFixedHeight(20);
        layout.addWidget(capBar);

        // === Top Items Section ===
        section(layout, "awesome_storage.magic_storage_screen.stats_top_items");

        List<ItemStack> sorted = items.stream()
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .limit(10)
                .collect(Collectors.toList());

        for (ItemStack stack : sorted) {
            QHBoxLayout itemRow = new QHBoxLayout();
            itemRow.setSpacing(4);

            QWidget icon = new QWidget() {
                @Override
                protected void paintEvent(QPaintEvent event) {
                    QPainter p = event.painter();
                    if (p == null) return;
                    MagicStorageScreen.renderSlot(p, 0, 0, 18, stack, false);
                }
            };
            icon.setFixedSize(18, 18);
            itemRow.addWidget(icon);

            String name = stack.getHoverName().getString();
            if (name.length() > 20) name = name.substring(0, 20) + "...";
            QLabel nameLabel = new QLabel(Component.literal(name));
            nameLabel.setTextColor(QColor.WHITE);
            itemRow.addWidget(nameLabel, 1);

            QLabel countLabel = new QLabel(Component.literal("x" + MagicStorageScreen.formatCount(stack.getCount())));
            countLabel.setTextColor(new QColor(0xFFFFAA00));
            itemRow.addWidget(countLabel);

            layout.addLayout(itemRow);
        }

        if (sorted.isEmpty()) {
            QLabel empty = new QLabel(Component.translatable("awesome_storage.magic_storage_screen.stats_empty"));
            empty.setTextColor(QColor.GRAY);
            layout.addWidget(empty);
        }

        // === Mod Distribution Section ===
        section(layout, "awesome_storage.magic_storage_screen.stats_mods");

        Map<String, Integer> modCounts = new HashMap<>();
        for (ItemStack s : items) {
            String mod = BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace();
            modCounts.merge(mod, s.getCount(), Integer::sum);
        }
        List<Map.Entry<String, Integer>> modSorted = new ArrayList<>(modCounts.entrySet());
        modSorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        for (Map.Entry<String, Integer> e : modSorted) {
            QHBoxLayout modRow = new QHBoxLayout();
            QLabel modLabel = new QLabel(Component.literal(e.getKey()));
            modLabel.setTextColor(QColor.WHITE);
            modRow.addWidget(modLabel, 1);
            QLabel modCount = new QLabel(Component.literal("x" + MagicStorageScreen.formatCount(e.getValue())));
            modCount.setTextColor(new QColor(0xFF8888FF));
            modRow.addWidget(modCount);
            layout.addLayout(modRow);
        }

        content.setLayout(layout);
        scroll.setWidget(content);
    }

    private void section(QVBoxLayout layout, String key) {
        QLabel sec = new QLabel(Component.translatable(key));
        sec.setTextColor(new QColor(0xFFFFAA00));
        layout.addWidget(sec);
    }

    private void addStatRow(QVBoxLayout layout, String key, String value) {
        QHBoxLayout row = new QHBoxLayout();
        QLabel label = new QLabel(Component.translatable(key));
        label.setTextColor(QColor.WHITE);
        row.addWidget(label, 1);
        QLabel val = new QLabel(Component.literal(value));
        val.setTextColor(new QColor(0xFF88FF88));
        row.addWidget(val);
        layout.addLayout(row);
    }

    private static class CapacityStatBar extends QWidget {
        private final int used;
        private final int total;

        CapacityStatBar(int used, int total) {
            this.used = used;
            this.total = Math.max(total, 1);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter p = event.painter();
            if (p == null) return;
            int w = width(), h = height();
            double ratio = Math.min(1.0, (double) used / total);
            int barW = (int) (w * ratio);

            p.fillRect(0, 0, w, h, new QColor(0xFF1A1A2E));
            QColor color;
            if (ratio < 0.5) color = new QColor(0xFF44AA44);
            else if (ratio < 0.8) color = new QColor(0xFFAAAA44);
            else color = new QColor(0xFFAA4444);
            p.fillRect(0, 0, barW, h, color);
            p.setColor(new QColor(0xFF555555));
            p.drawRect(0, 0, w, h);
            String text = used + "/" + total + " (" + (int) (ratio * 100) + "%)";
            p.setColor(QColor.WHITE);
            p.drawText(text, 4, (h - p.textHeight()) / 2);
        }
    }
}
