package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.manager.CraftingQueueManager;
import com.github.edg_thexu.awesome_storage.core.network.c2s.QueueActionPacket;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
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

import java.util.HashMap;
import java.util.Map;

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

        QHBoxLayout btnRow = new QHBoxLayout();
        QPushButton clearAllBtn = new QPushButton(Component.translatable("magic_storage_screen.queue_clear_all"));
        clearAllBtn.setFixedHeight(14);
        clearAllBtn.setOnClick(() -> PacketDistributor.sendToServer(QueueActionPacket.clearAll()));
        btnRow.addWidget(clearAllBtn);
        btnRow.addStretch(1);
        vl.addLayout(btnRow);
    }

    public void refresh() {
        content = new QWidget();
        QGridLayout grid = new QGridLayout();
        grid.setSpacing(6);

        MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
        if (be != null && be.getQueueManager() != null) {
            var slots = be.getQueueManager().getSlots();
            for (int si = 0; si < slots.size(); si++) {
                grid.addWidget(new QueueSlotWidget(si, slots.get(si)), si, 0);
            }
        }

        content.setLayout(grid);
        scroll.setWidget(content);
    }

    private static class QueueSlotWidget extends QWidget {
        QueueSlotWidget(int slotIndex, CraftingQueueManager.QueueSlot slot) {
            setFixedHeight(80);
            QVBoxLayout vl = new QVBoxLayout(this);
            vl.setSpacing(2);

            QLabel slotLabel = new QLabel(Component.literal("Slot " + (slotIndex + 1)));
            slotLabel.setTextColor(new QColor(0xFF888888));
            vl.addWidget(slotLabel);

            if (slot.isIdle()) {
                QLabel idle = new QLabel(Component.translatable("magic_storage_screen.queue_idle"));
                idle.setTextColor(QColor.GRAY);
                vl.addWidget(idle);
            } else {
                var current = slot.current();
                if (current != null) {
                    ItemStack result = resolveResult(current.recipeId, current.recipeTypeId);

                    QHBoxLayout row = new QHBoxLayout();
                    // Render item + name
                    if (!result.isEmpty()) {
                        QWidget itemWidget = new QWidget() {
                            @Override
                            protected void paintEvent(QPaintEvent event) {
                                QPainter p = event.painter();
                                if (p == null) return;
                                MagicStorageScreen.renderSlot(p, 0, 0, 18, result, false);
                            }
                        };
                        itemWidget.setFixedSize(18, 18);
                        row.addWidget(itemWidget);
                        row.addWidget(new QLabel(Component.literal(result.getHoverName().getString())));
                    } else {
                        row.addWidget(new QLabel(Component.literal(current.recipeId.toString())));
                    }
                    row.addStretch(1);
                    float pct = current.getProgressRatio();
                    QLabel prog = new QLabel(Component.literal((int) (pct * 100) + "%"));
                    prog.setTextColor(pct >= 1 ? new QColor(0xFF44FF44) : new QColor(0xFFFFAA00));
                    row.addWidget(prog);
                    vl.addLayout(row);

                    // Progress bar
                    QWidget bar = new QWidget() {
                        @Override
                        protected void paintEvent(QPaintEvent event) {
                            QPainter p = event.painter();
                            if (p == null) return;
                            int w = width(), h = height();
                            p.fillRect(0, 0, w, h, new QColor(0xFF1A1A2E));
                            int fw = (int) (w * pct);
                            p.fillRect(0, 0, fw, h, new QColor(0xFF44AA44));
                            p.setColor(new QColor(0xFF555555));
                            p.drawRect(0, 0, w, h);
                        }
                    };
                    bar.setFixedHeight(8);
                    vl.addWidget(bar);
                }

                int pending = slot.queue.size() - 1;
                if (pending > 0) {
                    QLabel pendingLabel = new QLabel(Component.literal("+" + pending + " pending"));
                    pendingLabel.setTextColor(QColor.GRAY);
                    vl.addWidget(pendingLabel);
                }

                QHBoxLayout ctrlRow = new QHBoxLayout();
                ctrlRow.setSpacing(4);
                QPushButton pauseBtn = new QPushButton(Component.literal(slot.paused ? "▶" : "⏸"));
                pauseBtn.setFixedSize(20, 14);
                pauseBtn.setOnClick(() ->
                        PacketDistributor.sendToServer(QueueActionPacket.togglePause(slotIndex)));
                ctrlRow.addWidget(pauseBtn);

                QPushButton clearBtn = new QPushButton(Component.literal("X"));
                clearBtn.setFixedSize(20, 14);
                clearBtn.setOnClick(() ->
                        PacketDistributor.sendToServer(QueueActionPacket.clearSlot(slotIndex)));
                ctrlRow.addWidget(clearBtn);
                ctrlRow.addStretch(1);
                vl.addLayout(ctrlRow);
            }
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
            if (!result.isEmpty()) {
                resultCache.put(recipeId, result);
            }
            return result;
        }
    }
}
