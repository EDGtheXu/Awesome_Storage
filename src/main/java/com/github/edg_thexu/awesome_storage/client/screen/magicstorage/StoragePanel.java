package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.client.widget.CapacityBar;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.network.c2s.AutoStockPacket;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicStoragePacket;
import com.github.edg_thexu.awesome_storage.utils.AutoStockSystem;
import com.github.edg_thexu.awesome_storage.utils.FavoriteSystem;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.geometry.QPoint;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.slots.SlotKeyConsumer;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.slots.SlotKeyRunner;
import com.github.edg_thexu.qtcraft_api.core.widget.QAction;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.button.QPushButton;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QMenu;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QToast;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QLineEdit;
import com.github.edg_thexu.qtcraft_api.util.WidgetTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

import static com.github.edg_thexu.awesome_storage.utils.Util.getStorageEntity;
import static com.github.edg_thexu.awesome_storage.utils.Util.getStorageItems;

// ========================================================================
// Storage Panel
// ========================================================================
class StoragePanel extends QWidget {
    private static boolean showFilter = true;
    private static boolean showLeftMenu = true;

    private final MagicStorageScreen parent;
    private final QLineEdit searchField;
    private final FilterBar filterBar;
    ItemGridWidget itemGrid;
    private final CapacityBar capacityBar;
    private final QSmoothScrollArea scrollArea;
    private final QWidget filterBarContainer;

    StoragePanel(MagicStorageScreen parent) {
        this.parent = parent;
        AutoStockSystem.getInstance().load();
        QVBoxLayout vl = new QVBoxLayout(this);
        vl.setSpacing(1);
        vl.setContentsMargins(3, 3, 3, 3);

        filterBarContainer = new QWidget();
        filterBarContainer.setVisible(showFilter);
        QHBoxLayout filterRow = new QHBoxLayout(filterBarContainer);
        filterRow.setSpacing(2);
        filterBar = new FilterBar(filterRow, this, "s", this::refresh);

        QHBoxLayout searchRow = new QHBoxLayout();
        searchRow.setSpacing(2);
        QPushButton menuBtn = new QPushButton(Component.literal("☰"));
        menuBtn.setFixedSize(16, 16);
        QMenu menu = new ForeShowMenu();
        menu.setVisible(false);
        {
            QAction filterAct = new QAction(Component.translatable("awesome_storage.magic_storage_screen.filter").getString());
            filterAct.setCheckable(true);
            filterAct.setChecked(showFilter);
            filterAct.connect(QAction.TRIGGERED, this, new SlotKeyRunner<>("filterAct", (self) -> {
                showFilter = filterAct.isChecked();
                filterBarContainer.setVisible(showFilter);
            }));
            menu.addAction(filterAct);
            QAction leftMenuAct = new QAction(Component.translatable("awesome_storage.magic_storage_screen.leftmenu").getString());
            leftMenuAct.setCheckable(true);
            leftMenuAct.setChecked(showLeftMenu);
            leftMenuAct.connect(QAction.TRIGGERED, this, new SlotKeyRunner<>("leftMenuAct", (self) -> {
                showLeftMenu = leftMenuAct.isChecked();
                parent.storageWin.leftMenu.setVisible(showLeftMenu);
            }));
            menu.addAction(leftMenuAct);
        }
        menuBtn.setOnClick(() -> {
            if(menu.isVisible()) {
                menu.dismiss();
            } else {
                menu.popup(menuBtn.mapToGlobal(QPoint.ZERO).x(), menuBtn.mapToGlobal(QPoint.ZERO).y() + menuBtn.height(), this);
            }
        });
        searchRow.addWidget(menuBtn);
        searchField = new QLineEdit();
        searchField.setPlaceholderText(Component.translatable("awesome_storage.magic_storage_screen.search").getString());
        searchField.setFixedHeight(16);
        searchField.connect(QLineEdit.TEXT_CHANGED, this, new SlotKeyConsumer<>("ss", (self, v) -> refresh()));
        searchRow.addWidget(searchField, 1);
        DepositButton depositBtn = new DepositButton();
        depositBtn.setFixedHeight(16);
        searchRow.addWidget(depositBtn);
        StockButton stockBtn = new StockButton();
        stockBtn.setFixedSize(16, 16);
        searchRow.addWidget(stockBtn);

        vl.addLayout(searchRow);
        vl.addWidget(filterBarContainer);

        scrollArea = new QSmoothScrollArea();
        itemGrid = new ItemGridWidget();
        itemGrid.setClickHandler(this::onItemClick);
        itemGrid.setRightClickHandler(this::onItemRightClick);
        scrollArea.setWidget(itemGrid);
        scrollArea.setWidgetResizable(true);
        vl.addWidget(scrollArea, 1);

        capacityBar = new CapacityBar();
        vl.addWidget(capacityBar);

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
            capacityBar.setSlots(be.getUsedSlots(), be.getTotalSlots());
        }
        if (parent.storageWin != null) {
            parent.storageWin.leftMenu.setVisible(showLeftMenu);
        }
    }


    void onItemClick(ItemStack stack, int index) {
        if (parent.getMenu().getCarried().isEmpty() && !stack.isEmpty()) {
            int storageIdx = itemGrid.getStorageIndex(index);
            if (storageIdx >= 0) {
                PacketDistributor.sendToServer(new MagicStoragePacket(storageIdx + 10000, stack.copy()));
                getStorageEntity(Minecraft.getInstance().player).setChanged();
                parent.scheduleRefresh();
            }
        }else if(!parent.getMenu().getCarried().isEmpty() && !FavoriteSystem.getInstance().clickedSlotWasFav) {
            PacketDistributor.sendToServer(new MagicStoragePacket(0, parent.getMenu().getCarried()));
            getStorageEntity(Minecraft.getInstance().player).setChanged();
            parent.scheduleRefresh();
        }
    }

    void onItemRightClick(ItemStack stack, int index) {
        if (Screen.hasShiftDown()) {
            AutoStockSystem.getInstance().removeTarget(stack);

        } else {
            int target = AutoStockSystem.getInstance().hasTarget(stack)
                    ? 0 : stack.getMaxStackSize();
            AutoStockSystem.getInstance().setTarget(stack, target);
            if(target > 0) {
                QToast.show(this.parent.rootWindow(), "added ", QToast.Type.Warning);
            } else {
                QToast.show(this.parent.rootWindow(), "removed ");
            }
        }
    }

    private class DepositButton extends QWidget {
        private boolean pressed;

        DepositButton() {
            setFocusPolicy(FocusPolicy.NoFocus);
            this.setFixedWidth(4 + fontWidth(Component.translatable("awesome_storage.magic_storage_screen.store_all").getString()));
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter p = event.painter();
            if (p == null) return;
            int w = width(), h = height();
            if (pressed) {
                p.fillRect(0, 0, w, h, new QColor(0xFF555555));
            } else if (isHovered()) {
                p.fillRect(0, 0, w, h, new QColor(0xFF4A4A4A));
            } else {
                p.fillRect(0, 0, w, h, new QColor(0xFF3C3C3C));
            }
            p.setColor(new QColor(0xFF666666));
            p.drawRect(0, 0, w, h);
            p.setColor(new QColor(0xFFFFFFFF));
            p.drawCenteredText(Component.translatable("awesome_storage.magic_storage_screen.store_all").getString(), w / 2, (h - p.textHeight()) / 2 );
        }

        @Override
        protected void mousePressEvent(QMouseEvent event) {
            event.accept();
            pressed = true;
            update();
        }

        @Override
        protected void mouseReleaseEvent(QMouseEvent event) {
            event.accept();
            pressed = false;
            update();
            if (event.button() == QMouseEvent.Button.Left) {
                if (Screen.hasControlDown()) {
                    PacketDistributor.sendToServer(new MagicStoragePacket(3, ItemStack.EMPTY, FavoriteSystem.getInstance().getFavoriteMask()));
                } else {
                    PacketDistributor.sendToServer(new MagicStoragePacket(2, ItemStack.EMPTY, FavoriteSystem.getInstance().getFavoriteMask()));
                }
                getStorageEntity(Minecraft.getInstance().player).setChanged();
                parent.scheduleRefresh();
            } else if (event.button() == QMouseEvent.Button.Right) {
                PacketDistributor.sendToServer(new MagicStoragePacket(4, ItemStack.EMPTY));
                getStorageEntity(Minecraft.getInstance().player).setChanged();
                parent.scheduleRefresh();
            }
        }

        @Override
        public WidgetTooltip toolTip() {
            return WidgetTooltip.create(Component.translatable("awesome_storage.deposit_btn.tooltip"));
        }
    }

    private class StockButton extends QWidget {
        private boolean pressed;

        StockButton() {
            setFocusPolicy(FocusPolicy.NoFocus);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter p = event.painter();
            if (p == null) return;
            int w = width(), h = height();
            if (pressed) {
                p.fillRect(0, 0, w, h, new QColor(0xFF555555));
            } else if (isHovered()) {
                p.fillRect(0, 0, w, h, new QColor(0xFF4A4A4A));
            } else {
                p.fillRect(0, 0, w, h, new QColor(0xFF3C3C3C));
            }
            p.setColor(new QColor(0xFF666666));
            p.drawRect(0, 0, w, h);
            p.setColor(new QColor(0xFFFFAA00));
            p.drawCenteredText(Component.literal("S"), w / 2, (h - p.textHeight()) / 2);
        }

        @Override
        protected void mousePressEvent(QMouseEvent event) {
            event.accept();
            pressed = true;
            update();
        }

        @Override
        protected void mouseReleaseEvent(QMouseEvent event) {
            event.accept();
            pressed = false;
            update();
            if (event.button() == QMouseEvent.Button.Left) {
                var targets = AutoStockSystem.getInstance().getAllTargets();
                if (targets.isEmpty()) return;
                List<ItemStack> list = new ArrayList<>();
                for (var e : targets.entrySet()) {
                    ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                            net.minecraft.resources.ResourceLocation.parse(e.getKey())));
                    if (!stack.isEmpty()) {
                        stack.setCount(e.getValue());
                        list.add(stack);
                    }
                }
                if (!list.isEmpty()) {
                    PacketDistributor.sendToServer(new AutoStockPacket(list));
                    parent.scheduleRefresh();
                }
            }
        }

        @Override
        public WidgetTooltip toolTip() {
            return WidgetTooltip.create(Component.translatable("awesome_storage.auto_stock_btn.tooltip"));
        }
    }
}
