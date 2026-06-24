package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.geometry.QSize;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.util.WidgetTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// ========================================================================
// Item Grid
// ========================================================================
public class ItemGridWidget extends QWidget {

    private List<ItemStack> items = new ArrayList<>();
    private final List<Integer> storageIndices = new ArrayList<>();
    private List<Boolean> overlayFlags = new ArrayList<>();
    private int hoverIndex = -1;
    private int selIndex = -1;

    @Override
    public WidgetTooltip toolTip() {
        if (hoverIndex >= 0 && hoverIndex < items.size() && !items.get(hoverIndex).isEmpty()) {
            return WidgetTooltip.create(items.get(hoverIndex));
        }
        return null;
    }

    private int cols = 8;
    private final int slotSize = 18;
    private java.util.function.BiConsumer<ItemStack, Integer> clickHandler;

    ItemGridWidget() {
        setFocusPolicy(FocusPolicy.NoFocus);
    }

    void setItems(List<ItemStack> items) {
        setItems(items, null);
    }

    void setItems(List<ItemStack> items, List<Boolean> overlays) {
        this.items = items;
        this.overlayFlags = overlays != null ? overlays : new ArrayList<>();
        // Map each display item to its index in the full getStoredItems() list
        storageIndices.clear();
        MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
        List<ItemStack> full = be != null ? be.getStoredItems() : new ArrayList<>();
        if (full != null) {
            for (ItemStack display : items) {
                int idx = -1;
                for (int i = 0; i < full.size(); i++) {
                    if (ItemStack.isSameItemSameComponents(display, full.get(i))) {
                        idx = i;
                        break;
                    }
                }
                storageIndices.add(idx);
            }
        }
        updateCols();
        update();
    }

    int getItemCount() {
        return items.size();
    }

    ItemStack getItemAt(int i) {
        return (i >= 0 && i < items.size()) ? items.get(i) : ItemStack.EMPTY;
    }

    int getCols() {
        return cols;
    }

    int getStorageIndex(int displayIndex) {
        if (displayIndex >= 0 && displayIndex < storageIndices.size()) {
            return storageIndices.get(displayIndex);
        }
        return -1;
    }

    void setClickHandler(java.util.function.BiConsumer<ItemStack, Integer> h) {
        this.clickHandler = h;
    }

    private void updateCols() {
        int newCols = Math.max(1, (width() + 2) / slotSize);
        if (newCols != cols) {
            cols = newCols;
            markDirty();
        }
    }

    @Override
    protected void resizeEvent(com.github.edg_thexu.qtcraft_api.core.events.QResizeEvent event) {
        updateCols();
        super.resizeEvent(event);
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter p = event.painter();
        if (p == null) return;
        updateCols();
        for (int i = 0; i < items.size(); i++) {

            int col = i % cols, row = i / cols;
            boolean over = i < overlayFlags.size() && overlayFlags.get(i);

            MagicStorageScreen.renderSlot(p, col * slotSize, row * slotSize, slotSize, items.get(i), i == hoverIndex, over);
            if(selIndex == i) {
                p.fillRoundRect(col * slotSize, row * slotSize, slotSize, slotSize, 3, new QColor(0x8F0BF8FF));
            }
        }
    }

    @Override
    protected void mousePressEvent(QMouseEvent event) {
        if (event.button() == QMouseEvent.Button.Left) {
            updateCols();
            int col = event.x() / slotSize, row = event.y() / slotSize;
            int idx = row * cols + col;
            selIndex = -1;
            if (idx >= 0 && idx < items.size() && !items.get(idx).isEmpty() && clickHandler != null && col < cols) {
                clickHandler.accept(items.get(idx), idx);
                selIndex = idx;
                event.accept();
            }
        }
    }

    @Override
    protected void mouseMoveEvent(QMouseEvent event) {
        updateCols();
        int col = event.x() / slotSize, row = event.y() / slotSize;

        int idx = row * cols + col;
        int prev = hoverIndex;
        hoverIndex = (idx >= 0 && idx < items.size() && col < cols) ? idx : -1;
        if (prev != hoverIndex) {
            markDirty();
            update();
        }
    }

    @Override
    public QSize sizeHint() {
        int rows = Math.max(1, (items.size() + cols - 1) / cols);
        return new QSize(cols * slotSize, rows * slotSize);
    }
}
