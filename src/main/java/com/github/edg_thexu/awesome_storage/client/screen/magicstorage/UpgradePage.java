package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.item.WirelessNetworkCard;
import com.github.edg_thexu.awesome_storage.core.network.c2s.UpgradePacket;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.QTheme;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.button.QPushButton;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QLineEdit;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class UpgradePage extends QWidget {

    private final UpgradeSlotWidget slotWidget;
    private final QLabel rangeLabel;
    private final QLineEdit freqInput;
    private final QSmoothScrollArea coreScroll;
    private QWidget coreContent;

    public UpgradePage() {
        QVBoxLayout vl = new QVBoxLayout(this);
        vl.setSpacing(4);
        vl.setContentsMargins(5, 5, 5, 5);

        QLabel slotTitle = new QLabel(Component.translatable("awesome_storage.magic_storage_screen.upgrade_slot"));
        slotTitle.setTextColor(QTheme.TEXT.title);
        vl.addWidget(slotTitle);

        slotWidget = new UpgradeSlotWidget();
        slotWidget.setFixedSize(24, 24);
        vl.addWidget(slotWidget);

        rangeLabel = new QLabel();
        rangeLabel.setTextColor(new QColor(0xFF88FF88));
        vl.addWidget(rangeLabel);

        QLabel freqTitle = new QLabel(Component.translatable("awesome_storage.magic_storage_screen.upgrade_frequency"));
        freqTitle.setTextColor(QTheme.TEXT.secondary);
        vl.addWidget(freqTitle);

        QHBoxLayout freqRow = new QHBoxLayout();
        freqInput = new QLineEdit();
        freqInput.setPlaceholderText(Component.translatable("awesome_storage.magic_storage_screen.upgrade_freq_hint").getString());
        freqInput.setFixedHeight(16);
        freqRow.addWidget(freqInput, 1);

        QPushButton setFreqBtn = new QPushButton(Component.translatable("awesome_storage.magic_storage_screen.upgrade_set"));
        setFreqBtn.setFixedHeight(14);
        setFreqBtn.setOnClick(() -> {
            try {
                int freq = Integer.parseInt(freqInput.text());
                PacketDistributor.sendToServer(new UpgradePacket(UpgradePacket.ACTION_SET_FREQ, ItemStack.EMPTY, freq));
            } catch (NumberFormatException ignored) {}
        });
        freqRow.addWidget(setFreqBtn);
        vl.addLayout(freqRow);

        QLabel connTitle = new QLabel(Component.translatable("awesome_storage.magic_storage_screen.upgrade_connected"));
        connTitle.setTextColor(QTheme.TEXT.secondary);
        vl.addWidget(connTitle);

        coreScroll = new QSmoothScrollArea();
        coreScroll.setWidgetResizable(true);
        coreScroll.setFixedHeight(100);
        coreContent = new QWidget();
        coreScroll.setWidget(coreContent);
        vl.addWidget(coreScroll, 1);
    }

    public void refresh() {
        MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
        if (be == null) return;

        slotWidget.update();

        if (be.getUpgradeSlot().getItem() instanceof WirelessNetworkCard card) {
            rangeLabel.setText(Component.translatable("awesome_storage.magic_storage_screen.upgrade_range", card.getRange()));
            rangeLabel.setVisible(true);
        } else {
            rangeLabel.setVisible(false);
        }

        coreContent = new QWidget();
        QVBoxLayout cl = new QVBoxLayout(coreContent);
        cl.setSpacing(2);

        if (be.getUpgradeSlot().getItem() instanceof WirelessNetworkCard && be.getFrequency() != 0) {
            var cores = com.github.edg_thexu.awesome_storage.core.manager.WirelessNetworkManager.getInstance()
                    .findConnectedCores(be.getBlockPos(), Minecraft.getInstance().level.dimension(),
                            be.getFrequency(), be.getWirelessRange());
            if (cores.isEmpty()) {
                QLabel none = new QLabel(Component.translatable("awesome_storage.magic_storage_screen.upgrade_no_cores"));
                none.setTextColor(QColor.GRAY);
                cl.addWidget(none);
            } else {
                for (var pos : cores) {
                    Component appendName = Component.empty();
                    if(Minecraft.getInstance().level.getBlockEntity(pos) instanceof MagicStorageBlockEntity mbe) {
                        appendName = Component.literal("   #").append(mbe.displayName);
                    }
                    QLabel coreLabel = new QLabel(Component.literal(
                            "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]").append(appendName));
                    coreLabel.setTextColor(new QColor(0xFF88FF88));
                    cl.addWidget(coreLabel);
                }
            }
        } else {
            QLabel none = new QLabel(Component.translatable("awesome_storage.magic_storage_screen.upgrade_no_cores"));
            none.setTextColor(QColor.GRAY);
            cl.addWidget(none);
        }

        coreScroll.setWidget(coreContent);
    }

    private class UpgradeSlotWidget extends QWidget {
        UpgradeSlotWidget() {}

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter p = event.painter();
            if (p == null) return;
            int s = width();
            p.fillRect(0, 0, s, s, new QColor(0xFF333333));
            p.setColor(new QColor(0xFF555555));
            p.drawRect(0, 0, s, s);
            MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
            if (be == null) return;
            ItemStack stack = be.getUpgradeSlot();
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
            if (be.getUpgradeSlot().isEmpty() && !carried.isEmpty() && carried.getItem() instanceof WirelessNetworkCard) {
                ItemStack toSlot = carried.copy();
                toSlot.setCount(1);
                be.setUpgradeSlot(toSlot);
                carried.shrink(1);
                if (carried.isEmpty()) player.containerMenu.setCarried(ItemStack.EMPTY);
                PacketDistributor.sendToServer(new UpgradePacket(UpgradePacket.ACTION_PLACE, toSlot, be.getFrequency()));
            } else if (!be.getUpgradeSlot().isEmpty() && carried.isEmpty()) {
                ItemStack taken = be.getUpgradeSlot().copy();
                be.setUpgradeSlot(ItemStack.EMPTY);
                player.containerMenu.setCarried(taken);
                PacketDistributor.sendToServer(new UpgradePacket(UpgradePacket.ACTION_TAKE, ItemStack.EMPTY, be.getFrequency()));
            }
            update();
        }
    }
}
