package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.item.QueueUpgradeItem;
import com.github.edg_thexu.awesome_storage.core.network.c2s.UpgradePacket;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.layouts.QGridLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class CraftUpgradePage extends QWidget {

    private final QueueSlotWidget[] queueSlots = new QueueSlotWidget[4];
    private final QLabel totalSlotsLabel;

    public CraftUpgradePage() {
        QVBoxLayout vl = new QVBoxLayout(this);
        vl.setSpacing(4);
        vl.setContentsMargins(5, 5, 5, 5);

        QLabel title = new QLabel(Component.translatable("magic_storage_screen.queue_upgrade_title"));
        title.setTextColor(new QColor(0xFFFFAA00));
        vl.addWidget(title);

        QGridLayout grid = new QGridLayout();
        grid.setSpacing(4);
        for (int i = 0; i < 4; i++) {
            queueSlots[i] = new QueueSlotWidget(i);
            queueSlots[i].setFixedSize(24, 24);
            grid.addWidget(queueSlots[i], i / 2, i % 2);
        }
        vl.addLayout(grid);

        totalSlotsLabel = new QLabel();
        totalSlotsLabel.setTextColor(new QColor(0xFF88FF88));
        vl.addWidget(totalSlotsLabel);

        vl.addStretch(1);
    }

    public void refresh() {
        MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
        if (be == null) return;
        for (var ws : queueSlots) ws.update();
        totalSlotsLabel.setText(Component.translatable("magic_storage_screen.queue_upgrade_total", be.getTotalQueueSlots()));
    }

    private static class QueueSlotWidget extends QWidget {
        private final int index;

        QueueSlotWidget(int index) {
            this.index = index;
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter p = event.painter();
            if (p == null) return;
            int s = width();
            p.fillRect(0, 0, s, s, new QColor(0xFF2A2A2A));
            p.setColor(new QColor(0xFF555555));
            p.drawRect(0, 0, s, s);
            MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
            if (be == null) return;
            ItemStack stack = be.getQueueUpgradeSlots()[index];
            if (!stack.isEmpty()) {
                p.renderItemStack(stack, (s - 16) / 2, (s - 16) / 2);
            } else {
                p.setColor(new QColor(0xFF666666));
                p.drawText("+", (s - p.textWidth("+")) / 2, (s - p.textHeight()) / 2);
            }
        }

        @Override
        protected void mousePressEvent(QMouseEvent event) {
            event.accept();
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
            if (be == null) return;
            ItemStack carried = player.containerMenu.getCarried();
            ItemStack current = be.getQueueUpgradeSlots()[index];
            if (current.isEmpty() && !carried.isEmpty() && carried.getItem() instanceof QueueUpgradeItem) {
                PacketDistributor.sendToServer(UpgradePacket.queuePlace(index, carried.copyWithCount(1)));
            } else if (!current.isEmpty() && carried.isEmpty()) {
                PacketDistributor.sendToServer(UpgradePacket.queueTake(index));
            }
        }
    }
}
