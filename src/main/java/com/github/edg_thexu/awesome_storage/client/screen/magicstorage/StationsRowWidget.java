package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicStoragePacket;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QWheelEvent;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

// ========================================================================
// Stations Row
// ========================================================================
class StationsRowWidget extends QWidget {
    private final MagicStorageScreen parent;
    private final List<ItemStack> stations = new ArrayList<>();
    private int scrollOffset;

    public StationsRowWidget(MagicStorageScreen parent) {
        this.parent = parent;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int getStationCount() {
        return stations.size();
    }

    void refresh() {
        stations.clear();
        MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
        if (be != null) {
            for (String s : be.getBlock_accessors()) {
                Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s));
                if (b != null) stations.add(new ItemStack(b));
            }
        }
        stations.add(ItemStack.EMPTY);
        update();
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter p = event.painter();
        if (p == null) return;
        p.fillRect(0, 0, width(), height(), new QColor(0xCC333333));
        int slot = 14, gap = 1;
        p.enableClip(0, 0, width(), height());
        p.push();
        p.translate(-scrollOffset, 0);
        for (int i = 0; i < stations.size(); i++) {
            int x = i * (slot + gap);
            MagicStorageScreen.renderSlot(p, x, 2, slot, stations.get(i), false);
            if (stations.get(i).isEmpty()) {
                p.setColor(new QColor(0xFF888888));
                p.drawText("+", x + slot / 2 - 3, 2 + slot / 2 - 4);
            }
        }
        p.pop();
        p.disableClip();
    }

    @Override
    protected void mousePressEvent(QMouseEvent event) {
        if (event.button() == QMouseEvent.Button.Left) {
            int slot = 14, gap = 1;
            int idx = (event.x() + scrollOffset) / (slot + gap);
            if (idx >= 0 && idx < stations.size()) {
                if (idx < stations.size() - 1) {
                    PacketDistributor.sendToServer(new MagicStoragePacket(20000 + idx, ItemStack.EMPTY));
                } else {
                    ItemStack held = parent.getMenu().getCarried();
                    if (!held.isEmpty() && held.getItem() instanceof BlockItem bi && CraftConfig.isEnabledBlock(bi.getBlock())) {
                        PacketDistributor.sendToServer(new MagicStoragePacket(1, held));
                    }
                }
                var be = Util.getStorageEntity(Minecraft.getInstance().player);
                if (be != null) be.setChanged();
                // Schedule recipe reload — it will trigger after server syncs accessors
                parent.scheduleRefresh();
            }
            event.accept();
        }
    }

    @Override
    protected void wheelEvent(QWheelEvent event) {
        int max = Math.max(0, stations.size() * 34 - width());
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) (event.delta() * 0.8)));
        update();
        event.accept();
    }
}
