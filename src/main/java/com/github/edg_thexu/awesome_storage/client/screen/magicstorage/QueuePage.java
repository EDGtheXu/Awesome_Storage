package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.manager.CraftingQueueManager;
import com.github.edg_thexu.awesome_storage.core.network.c2s.QueueActionPacket;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.layouts.QGridLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.button.QPushButton;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class QueuePage extends QWidget {

    private final MagicStorageScreen screen;
    private QGridLayout grid;

    public QueuePage(MagicStorageScreen screen) {
        this.screen = screen;
        QVBoxLayout vl = new QVBoxLayout(this);
        vl.setSpacing(4);
        vl.setContentsMargins(5, 5, 5, 5);

        QLabel title = new QLabel(Component.translatable("magic_storage_screen.queue_title"));
        title.setTextColor(new QColor(0xFFFFAA00));
        vl.addWidget(title);

        QSmoothScrollArea scroll = new QSmoothScrollArea();
        scroll.setWidgetResizable(true);
        QWidget content = new QWidget();
        grid = new QGridLayout();
        grid.setSpacing(6);
        content.setLayout(grid);
        scroll.setWidget(content);
        vl.addWidget(scroll, 1);

        QHBoxLayout btnRow = new QHBoxLayout();
        QPushButton clearAllBtn = new QPushButton(Component.translatable("magic_storage_screen.queue_clear_all"));
        clearAllBtn.setFixedHeight(14);
        clearAllBtn.setOnClick(() -> {
            PacketDistributor.sendToServer(QueueActionPacket.clearAll());
        });
        btnRow.addWidget(clearAllBtn);
        btnRow.addStretch(1);
        vl.addLayout(btnRow);
    }

    public void refresh() {
        grid.clear();
        MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
        if (be == null || be.getQueueManager() == null) return;
        var slots = be.getQueueManager().getSlots();
        for (int si = 0; si < slots.size(); si++) {
            grid.addWidget(new QueueSlotWidget(si, slots.get(si)), si, 0);
        }
    }

    private class QueueSlotWidget extends QWidget {
        private final int slotIndex;
        private final CraftingQueueManager.QueueSlot slot;

        QueueSlotWidget(int slotIndex, CraftingQueueManager.QueueSlot slot) {
            this.slotIndex = slotIndex;
            this.slot = slot;
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
                    QHBoxLayout row = new QHBoxLayout();
                    row.addWidget(new QLabel(Component.literal(current.recipeId.toString())));
                    row.addStretch(1);
                    float pct = current.getProgressRatio();
                    QLabel prog = new QLabel(Component.literal((int) (pct * 100) + "%"));
                    prog.setTextColor(pct >= 1 ? new QColor(0xFF44FF44) : new QColor(0xFFFFAA00));
                    row.addWidget(prog);
                    vl.addLayout(row);

                    // Progress bar
                    QWidget bar = new QWidget() {
                        @Override
                        protected void paintEvent(com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent event) {
                            var p = event.painter();
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
                QPushButton pauseBtn = new QPushButton(Component.literal(slot.paused ? "\u25B6" : "\u23F8"));
                pauseBtn.setFixedSize(20, 14);
                int fsi = slotIndex;
                pauseBtn.setOnClick(() ->
                        PacketDistributor.sendToServer(QueueActionPacket.togglePause(fsi)));
                ctrlRow.addWidget(pauseBtn);

                QPushButton clearBtn = new QPushButton(Component.literal("X"));
                clearBtn.setFixedSize(20, 14);
                clearBtn.setOnClick(() ->
                        PacketDistributor.sendToServer(QueueActionPacket.clearSlot(fsi)));
                ctrlRow.addWidget(clearBtn);
                ctrlRow.addStretch(1);
                vl.addLayout(ctrlRow);
            }
        }
    }
}
