package com.github.edg_thexu.awesome_storage.core.menu;

import com.github.edg_thexu.awesome_storage.core.block.StorageArrayBlockEntity;
import com.github.edg_thexu.awesome_storage.core.block.StorageUnitBlock;
import com.github.edg_thexu.awesome_storage.core.registry.ModMenus;
import com.github.edg_thexu.awesome_storage.mix_util.IPlayer;
import com.github.edg_thexu.qtcraft_api.core.layouts.QGridLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.slot.ChestSlotContainer;
import com.github.edg_thexu.qtcraft_api.core.slot.IWidgetSource;
import com.github.edg_thexu.qtcraft_api.core.slot.SlotContainer;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QContainer;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QSlot;
import com.github.edg_thexu.qtcraft_api.menu.QBaseMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StorageArrayMenu extends QBaseMenu {

    private final StorageArrayBlockEntity blockEntity;
    private final ArrayWidgetSource widgetSource;

    public StorageArrayMenu(int containerId, Inventory inventory, StorageArrayBlockEntity blockEntity) {
        super(ModMenus.STORAGE_ARRAY_MENU.get(), containerId, inventory,
                new ArrayWidgetSource(blockEntity, blockEntity.getUnitContainer()));
        this.blockEntity = blockEntity;
        this.widgetSource = (ArrayWidgetSource) getSlotSource();
    }

    public StorageArrayMenu(int containerId, Inventory inventory) {
        super(ModMenus.STORAGE_ARRAY_MENU.get(), containerId, inventory,
                new ArrayWidgetSource((StorageArrayBlockEntity) ((IPlayer) inventory.player).awesomeStorage$getContainer(), new SimpleContainer(8)));
        this.blockEntity = (StorageArrayBlockEntity) ((IPlayer) inventory.player).awesomeStorage$getContainer();
        this.widgetSource = (ArrayWidgetSource) getSlotSource();
    }

    public StorageArrayBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getUsedItemCount() {
        return blockEntity != null ? blockEntity.getTotalItemCount() : 0;
    }

    public int getTotalCapacity() {
        return blockEntity != null ? blockEntity.getTotalCapacity() : 0;
    }

    // ========================================================================
    // IWidgetSource — builds the entire UI widget tree
    // ========================================================================
    public static class ArrayWidgetSource extends QWidget implements IWidgetSource {

        public final UnitSlotContainer unitSlots;
        private final StorageArrayBlockEntity blockEntity;

        public ArrayWidgetSource(StorageArrayBlockEntity blockEntity, Container container) {
            this.blockEntity = blockEntity;
            this.unitSlots = new UnitSlotContainer(container, blockEntity);
        }

        @Override
        public List<SlotContainer> initCraftingSlotModel() {
            return List.of(unitSlots);
        }

        @Override
        public void initWidget(Level level, @Nullable QBaseMenu menu) {
            QVBoxLayout root = new QVBoxLayout(this);
            root.setSpacing(4);
            root.setContentsMargins(8, 6, 8, 10);

            QLabel title = new QLabel(Component.translatable("container.awesome_storage.storage_array"));
            title.setAlignment(QLabel.Alignment.Center);
            title.setTextColor(new QColor(0xFFFFAA00));
            title.setFixedWidth(fontWidth(title.text().getString()) + 2);
            root.addWidget(title, 0, QLayout.ALIGN_HCENTER);

            QWidget unitGrid = new QWidget();
            unitGrid.setStyleSheet("background-color: #44FFFFFF; border-radius: 4px;");
            QGridLayout gl = new QGridLayout();
            gl.setSpacing(2);
            gl.setContentsMargins(4, 4, 4, 4);
            for (int i = 0; i < StorageArrayBlockEntity.UNIT_SLOT_COUNT; i++) {
                QSlot slot = menu != null
                        ? new QSlot(menu, unitSlots.inputStartIndex() + i)
                        : new QSlot(unitSlots.input(i));
                slot.setFixedSize(22, 22);
                gl.addWidget(slot, i / 4, i % 4);
            }
            unitGrid.setLayout(gl);
            root.addWidget(unitGrid, 0, QLayout.ALIGN_HCENTER);

            if (menu != null) {

                QLabel invLabel = new QLabel(Component.translatable("container.inventory"));
                invLabel.setTextColor(new QColor(0xFFFFAA00));
                invLabel.setFixedWidth(fontWidth(invLabel.text().getString()) + 2);
                root.addWidget(invLabel, 0, QLayout.ALIGN_HCENTER);

                QContainer mainInv = new QContainer(menu, 9, 27, 9).setSlotGroup("main");
                root.addWidget(mainInv, 0, QLayout.ALIGN_HCENTER);

                QContainer hotbar = new QContainer(menu, 0, 9, 9).setSlotGroup("hotbar");
                root.addWidget(hotbar, 0, QLayout.ALIGN_HCENTER);

                QContainer block= new QContainer(menu, unitSlots.inputStartIndex(), unitSlots.inputCount(), 9);

                hotbar.transferTo(block, mainInv);
                mainInv.transferTo(block, hotbar);
                block.transferTo(hotbar, mainInv);
            }
        }
    }

    // ========================================================================
    // UnitSlotContainer — wraps the unit container slots with validation
    // ========================================================================
    public static class UnitSlotContainer extends ChestSlotContainer {

        private final StorageArrayBlockEntity blockEntity;

        public UnitSlotContainer(Container container) {
            this(container, null);
        }

        public UnitSlotContainer(Container container, StorageArrayBlockEntity blockEntity) {
            super(container, StorageArrayBlockEntity.UNIT_SLOT_COUNT);
            this.blockEntity = blockEntity;
        }

        @Override
        protected boolean mayPlaceInput(int index, ItemStack stack) {
            return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof StorageUnitBlock;
        }

        @Override
        protected Slot createInputSlot(int index) {
            return new Slot(container, index, -1000, -1000) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return mayPlaceInput(index, stack);
                }
                @Override
                public boolean mayPickup(Player player) {
                    if (blockEntity == null) return true;
                    return blockEntity.canRemoveUnit(index - 41);
                }
                @Override
                public void onTake(Player player, ItemStack stack) {
                    super.onTake(player, stack);
                    if (blockEntity != null) {
                        blockEntity.setUnitSlot(index, ItemStack.EMPTY);
                    }
                }
                @Override
                public int getMaxStackSize() {
                    return 1;
                }
                @Override
                public void setChanged() {
                    super.setChanged();
                    onContainerChanged();
                }
            };
        }
    }
}
