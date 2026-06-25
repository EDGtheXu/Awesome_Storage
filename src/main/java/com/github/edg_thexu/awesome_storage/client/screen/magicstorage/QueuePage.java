package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.manager.CraftingQueueManager;
import com.github.edg_thexu.awesome_storage.core.manager.QueuedRecipe;
import com.github.edg_thexu.awesome_storage.core.network.c2s.QueueActionPacket;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.layouts.QGridLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QGridLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.button.QPushButton;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class QueuePage extends QWidget {

    private final MagicStorageScreen screen;
    private final QSmoothScrollArea scroll;
    private QWidget content;

    public QueuePage(MagicStorageScreen screen) {
        this.screen = screen;
        QVBoxLayout vl = new QVBoxLayout(this);
        vl.setSpacing(4);
        vl.setContentsMargins(5, 5, 5, 5);

        QLabel title = new QLabel(Component.translatable("magic_storage_screen.queue_title"));
        title.setTextColor(new QColor(0xFFFFAA00));
        vl.addWidget(title);

        scroll = new QSmoothScrollArea();
        scroll.setWidgetResizable(true);
        vl.addWidget(scroll, 1);
    }

    public void refresh() {
        MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
        if (be == null) return;

        content = new QWidget();
        QVBoxLayout layout = new QVBoxLayout(content);
        layout.setSpacing(6);
        layout.setContentsMargins(5, 5, 5, 5);

        // Pending items grid
        QLabel pendingTitle = new QLabel(Component.translatable("magic_storage_screen.queue_pending"));
        pendingTitle.setTextColor(new QColor(0xFFFFAA00));
        layout.addWidget(pendingTitle);

        var pending = be.getQueueManager().getPendingQueue();
        if (pending.isEmpty()) {
            QLabel empty = new QLabel(Component.translatable("magic_storage_screen.queue_empty"));
            empty.setTextColor(QColor.GRAY);
            layout.addWidget(empty);
        } else {
            int cols = 8;
            QWidget pendingGrid = new QWidget();
            QGridLayout gridLayout = new QGridLayout();
            gridLayout.setSpacing(2);
            int idx = 0;
            for (QueuedRecipe recipe : pending) {
                int fi = idx;
                PendingItemWidget w = new PendingItemWidget(recipe, fi);
                w.setFixedSize(22, 22);
                gridLayout.addWidget(w, idx / cols, idx % cols);
                if (idx > 200) break; // safety cap
                idx++;
            }
            pendingGrid.setLayout(gridLayout);
            layout.addWidget(pendingGrid);
        }

        // Divider
        QLabel statusLabel = new QLabel(Component.translatable("magic_storage_screen.queue_status",
                be.getQueueManager().getSlots().size()));
        statusLabel.setTextColor(new QColor(0xFF8888FF));
        layout.addWidget(statusLabel);

        // Queue slots
        var slots = be.getQueueManager().getSlots();
        for (int si = 0; si < slots.size(); si++) {
            layout.addWidget(new QueueSlotWidget(si, slots.get(si)));
        }

        // Controls
        QHBoxLayout ctrlRow = new QHBoxLayout();
        QPushButton clearPendingBtn = new QPushButton(Component.translatable("magic_storage_screen.queue_clear_all"));
        clearPendingBtn.setFixedHeight(14);
        clearPendingBtn.setOnClick(() -> PacketDistributor.sendToServer(QueueActionPacket.clearAll()));
        ctrlRow.addWidget(clearPendingBtn);
        ctrlRow.addStretch(1);
        layout.addLayout(ctrlRow);

        layout.addStretch(1);
        scroll.setWidget(content);
    }

    private static final Map<net.minecraft.resources.ResourceLocation, ItemStack> resultCache = new HashMap<>();

    private static ItemStack resolveResult(net.minecraft.resources.ResourceLocation recipeId, net.minecraft.resources.ResourceLocation recipeTypeId) {
        var cached = resultCache.get(recipeId);
        if (cached != null) return cached;
        var level = Minecraft.getInstance().level;
        if (level == null) return ItemStack.EMPTY;
        var optRecipe = level.getRecipeManager().byKey(recipeId);
        if (optRecipe.isEmpty()) return ItemStack.EMPTY;
        RecipeType<?> rt = BuiltInRegistries.RECIPE_TYPE.get(recipeTypeId);
        if (rt == null) return ItemStack.EMPTY;
        AbstractMagicCraftRecipeAdapter<?, ?> adapter = AdapterManager.Adapters.get(rt);
        if (adapter == null) return ItemStack.EMPTY;
        ItemStack result = adapter.getResult((RecipeHolder) optRecipe.get());
        if (!result.isEmpty()) resultCache.put(recipeId, result);
        return result;
    }

    private static class PendingItemWidget extends QWidget {
        private final QueuedRecipe recipe;
        private final int index;

        PendingItemWidget(QueuedRecipe recipe, int index) {
            this.recipe = recipe;
            this.index = index;
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter p = event.painter();
            if (p == null) return;
            int s = width();
            ItemStack result = resolveResult(recipe.recipeId, recipe.recipeTypeId);
            ItemStack temp = result.copy();
            temp.setCount(recipe.quantity);
            MagicStorageScreen.renderSlot(p, 0, 0, s, temp, false);
            p.setColor(new QColor(0x44FF0000));
            p.drawRect(0, 0, s, s);
        }

        @Override
        protected void mousePressEvent(QMouseEvent event) {
            event.accept();
            PacketDistributor.sendToServer(QueueActionPacket.remove(0, index));
        }
    }

    private static class QueueSlotWidget extends QWidget {
        private final int slotIndex;
        private final CraftingQueueManager.QueueSlot slot;

        QueueSlotWidget(int slotIndex, CraftingQueueManager.QueueSlot slot) {
            this.slotIndex = slotIndex;
            this.slot = slot;
            setFixedHeight(70);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter p = event.painter();
            if (p == null) return;
            int w = width(), h = height();

            // Background
            p.fillRect(0, 0, w, h, new QColor(0xCC1E1E1E));
            p.setColor(new QColor(0xFF555555));
            p.drawRect(0, 0, w, h);

            // Slot label
            p.setColor(new QColor(0xFF888888));
            p.drawText("Slot " + (slotIndex + 1) + (slot.paused ? " [Paused]" : ""), 4, 12);

            // Pause/Play button (always visible)
            String pauseIcon = slot.paused ? "▶" : "⏸";
            p.setColor(QColor.WHITE);
            p.drawText(pauseIcon, w - 18, 8);

            // Clear X button (always visible)
            p.setColor(new QColor(0xFFFF4444));
            p.drawText("X", w - 18, h - 8);

            if (slot.isIdle()) {
                p.setColor(QColor.GRAY);
                p.drawText(Component.translatable("magic_storage_screen.queue_idle").getString(), 4, 40);
            } else {
                QueuedRecipe current = slot.current();
                if (current == null) return;

                // Item icon
                ItemStack result = resolveResult(current.recipeId, current.recipeTypeId);
                p.translate(0, 5);
                MagicStorageScreen.renderSlot(p, 4, 18, 18, result, false);

                // Progress bar
                int barX = 28, barY = 22, barW = w - barX - 60, barH = 10;
                float pct = current.getProgressRatio();
                p.fillRect(barX, barY, barW, barH, new QColor(0xFF1A1A2E));
                p.fillRect(barX, barY, (int) (barW * pct), barH, new QColor(0xFF44AA44));
                p.setColor(new QColor(0xFF555555));
                p.drawRect(barX, barY, barW, barH);

                // Percentage
                p.setColor(new QColor(0xFFFFAA00));
                p.drawText((int) (pct * 100) + "%", barX + barW + 4, barY + barH - 2);
            }
        }

        @Override
        protected void mousePressEvent(QMouseEvent event) {
            event.accept();
            int mx = event.x(), my = event.y(), w = width(), h = height();
            if (mx >= w - 20 && my <= 14) {
                PacketDistributor.sendToServer(QueueActionPacket.togglePause(slotIndex));
            } else if (mx >= w - 20 && my >= h - 14) {
                PacketDistributor.sendToServer(QueueActionPacket.clearSlot(slotIndex));
            }
        }
    }
}
