package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicStoragePacket;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.slots.SlotKeyConsumer;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QLineEdit;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

import static com.github.edg_thexu.awesome_storage.utils.Util.getStorageEntity;
import static com.github.edg_thexu.awesome_storage.utils.Util.getStorageItems;

// ========================================================================
// Storage Panel
// ========================================================================
class StoragePanel extends QWidget {
    private final MagicStorageScreen parent;
    private final QLineEdit searchField;
    private final FilterBar filterBar;
    ItemGridWidget itemGrid;
    private final QLabel capacityLabel;
    private final QSmoothScrollArea scrollArea;

    StoragePanel(MagicStorageScreen parent) {
        this.parent = parent;
        QVBoxLayout vl = new QVBoxLayout(this);
        vl.setSpacing(1);
        vl.setContentsMargins(3, 3, 3, 3);

        QHBoxLayout searchRow = new QHBoxLayout();
        searchField = new QLineEdit();
        searchField.setPlaceholderText("Search...");
        searchField.setFixedHeight(16);
        searchField.connect(QLineEdit.TEXT_CHANGED, this, new SlotKeyConsumer<>("ss", (self, v) -> refresh()));
        searchRow.addWidget(searchField, 1);
        vl.addLayout(searchRow);

        QHBoxLayout filterRow = new QHBoxLayout();
        filterRow.setSpacing(2);
        filterBar = new FilterBar(filterRow, this, "s", this::refresh);
        vl.addLayout(filterRow);

        scrollArea = new QSmoothScrollArea();
        itemGrid = new ItemGridWidget();
        itemGrid.setClickHandler(this::onItemClick);
        scrollArea.setWidget(itemGrid);
        scrollArea.setWidgetResizable(true);
        vl.addWidget(scrollArea, 1);

        capacityLabel = new QLabel(Component.literal("Capacity: 0/0"));
        vl.addWidget(capacityLabel);

        refresh();
    }

    void refresh() {
        List<ItemStack> items = getStorageItems(Minecraft.getInstance().player);
        if (items == null) return;
        List<ItemStack> filtered = filterBar.apply(items, searchField.text().toLowerCase());
        itemGrid.setItems(filtered);
        scrollArea.updateLayout();
        scrollArea.markDirty();
        MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
        if (be != null) {
            capacityLabel.setText(Component.literal("容量: " + be.getUsedSlots() + "/" + be.getTotalSlots()));
        }
    }


    void onItemClick(ItemStack stack, int index) {
        if (parent.getMenu().getCarried().isEmpty() && !stack.isEmpty()) {
            int storageIdx = itemGrid.getStorageIndex(index);
            if (storageIdx >= 0) {
                PacketDistributor.sendToServer(new MagicStoragePacket(storageIdx + 10000, new ItemStack(net.minecraft.world.item.Items.WOODEN_AXE)));
                getStorageEntity(Minecraft.getInstance().player).setChanged();
                parent.scheduleRefresh();
            }
        }
    }
}
