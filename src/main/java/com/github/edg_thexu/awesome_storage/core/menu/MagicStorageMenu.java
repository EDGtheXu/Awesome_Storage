package com.github.edg_thexu.awesome_storage.core.menu;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.registry.ModMenus;
import com.github.edg_thexu.qtcraft_api.core.layouts.QLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.slot.IWidgetSource;
import com.github.edg_thexu.qtcraft_api.core.slot.SlotContainer;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QContainer;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.menu.QBaseMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;

import javax.annotation.Nullable;
import java.util.List;

public class MagicStorageMenu extends QBaseMenu {
    public final ContainerData access;
    private boolean dirty;

    public boolean isDirty() { return dirty; }
    public void setDirty(boolean v) { dirty = v; }

    public MagicStorageMenu(int pContainerId, Inventory inventory) {
        this(pContainerId, inventory, new SimpleContainerData(2));
    }

    public MagicStorageMenu(int pContainerId, Inventory pPlayerInventory, ContainerData pAccess) {
        super(ModMenus.MAGIC_STORAGE_MENU.get(), pContainerId, pPlayerInventory, new StorageWidgetSource());
        this.access = pAccess;
        addDataSlots(pAccess);
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemStack = slots.get(slotIndex).getItem();
        if (!itemStack.isEmpty()) {
            // Try to store directly into adjacent containers via the storage entity
            MagicStorageBlockEntity be = com.github.edg_thexu.awesome_storage.utils.Util.getStorageEntity(player);
            if (be != null) {
                ItemStack toStore = itemStack.copy();
                int remaining = be.storeItem(toStore);
                if (remaining < itemStack.getCount()) {
                    // At least some items were stored
                    itemStack.shrink(itemStack.getCount() - remaining);
                    if (itemStack.isEmpty()) slots.get(slotIndex).set(ItemStack.EMPTY);
                    be.syncToClient(player);
                    this.broadcastChanges();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    // ========================================================================
    // IWidgetSource for player inventory at top-left
    // ========================================================================
    public static class StorageWidgetSource extends QWidget implements IWidgetSource {

        @Override
        public List<SlotContainer> initCraftingSlotModel() {
            return List.of();
        }

        @Override
        public void initWidget(net.minecraft.world.level.Level level, @Nullable QBaseMenu menu) {
            if (menu == null) return;

            QVBoxLayout root = new QVBoxLayout(this);
            root.setSpacing(2);

            QLabel invLabel = new QLabel(Component.literal("Inventory"));
            invLabel.setTextColor(new QColor(0xFFFFAA00));
            root.addWidget(invLabel, 0, QLayout.ALIGN_LEFT);

            QContainer mainInv = new QContainer(menu, 9, 27, 9);
            mainInv.setSlotGroup("main");
            root.addWidget(mainInv, 0, QLayout.ALIGN_LEFT);

            QContainer hotbar = new QContainer(menu, 0, 9, 9);
            hotbar.setSlotGroup("hotbar");
            root.addWidget(hotbar, 0, QLayout.ALIGN_LEFT);

            hotbar.transferTo(mainInv);
            mainInv.transferTo(hotbar);
        }
    }
}
