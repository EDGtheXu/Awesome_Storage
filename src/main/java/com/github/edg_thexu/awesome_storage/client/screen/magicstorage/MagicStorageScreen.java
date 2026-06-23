package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.client.widget.FloatingWindow;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.block.StorageCoreBlock;
import com.github.edg_thexu.awesome_storage.core.menu.MagicStorageMenu;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicStoragePacket;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.client.screen.QContainerWidgetScreen;
import com.github.edg_thexu.qtcraft_api.core.geometry.QSize;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QMainWindow;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static com.github.edg_thexu.awesome_storage.utils.Util.getStorageEntity;


public class MagicStorageScreen extends QContainerWidgetScreen<MagicStorageMenu> {

    private FloatingWindow storageWin;
    private FloatingWindow craftWin;
    private boolean storageOnly;
    private StoragePanel storagePanel;
    private CraftPanel craftPanel;
    private long nextRefresh;
    private long lastPeriodicRefresh;
    List<String> lastAccessors = new ArrayList<>();

    private static int storageX = 200, storageY = 30, storageW = 220, storageH = 220;
    private static int craftX = 200, craftY = 30, craftW = 220, craftH = 220;

    public MagicStorageScreen(MagicStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected QMainWindow createRootWindow() {
        QMainWindow win = new QMainWindow();
        win.setGeometry(0, 0, width, height);

        // Player inventory widget from slot source, positioned at top-left
        QWidget invWidget = menu.getSlotSource().getRoot();
        if (invWidget != null) {
            invWidget.setParent(win);
            QSize hint = invWidget.sizeHint();
            invWidget.setGeometry(5, 5, Math.max(hint.width(), 170), hint.height());
        }

        // Determine mode
        MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
        storageOnly = be != null && be.getBlockState().getBlock() instanceof StorageCoreBlock;

        if (!storageOnly) {
            // Craft window
            craftPanel = new CraftPanel(this);
            craftWin = new FloatingWindow("Crafting");
            craftWin.setWidget(craftPanel);
            craftWin.setGeometry(craftX, craftY, craftW, craftH);
            craftWin.setParent(win);
            craftPanel.updateLayout();
            craftPanel.refresh();

        }else{
            // Storage window
            storagePanel = new StoragePanel(this);
            storageWin = new FloatingWindow("Storage");
            storageWin.setWidget(storagePanel);
            storageWin.setGeometry(storageX, storageY, storageW, storageH);
            storageWin.setParent(win);
            storagePanel.updateLayout();
        }

        return win;
    }

    @Override
    protected void init() {
        super.init();
        if (rootWindow() != null) {
            rootWindow().setGeometry(0, 0, width, height);
        }
    }

    static void renderSlot(QPainter p, int x, int y, int size, ItemStack stack, boolean highlight) {
        renderSlot(p, x, y, size, stack, highlight, false);
    }

    static void renderSlot(QPainter p, int x, int y, int size, ItemStack stack, boolean highlight, boolean overlay) {
        p.fillRect(x, y, size, size, new QColor(0xFF333333));
        if (overlay) p.fillRect(x, y, size, size, new QColor(0x55FF0000));
        if (highlight) p.fillRect(x, y, size, size, new QColor(0x55FFFFFF));
        if (!stack.isEmpty()) {
            p.renderItemStack(stack, x + (size - 16) / 2, y + (size - 16) / 2);
            p.renderItemDecorations(stack, x + (size - 16) / 2, y + (size - 16) / 2);
        }
    }


    // ========================================================================
    // Screen Rendering
    // ========================================================================
    void scheduleRefresh() {
        nextRefresh = System.currentTimeMillis() + 150;
    }

    private void scheduleReloadRecipes() {
        lastAccessors = new ArrayList<>();
        scheduleRefresh();
    }


    private void refreshPanels() {
        if (storagePanel != null) storagePanel.refresh();
        if (craftPanel != null) {
            // Detect block_accessors changes (server sync via onDataPacket)
            MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
            if (be != null) {
                List<String> current = be.getBlock_accessors();
                if (!current.equals(lastAccessors)) {
                    lastAccessors = new ArrayList<>(current);
                    try { craftPanel.reloadRecipes(); } catch (Exception e) { e.printStackTrace(); }
                }
            }
            craftPanel.refresh();
            craftPanel.infoPanel.markDirty();
            craftPanel.infoPanel.update();
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        if (nextRefresh != 0 && now > nextRefresh) {
            nextRefresh = 0;
            refreshPanels();
        }
        if (now - lastPeriodicRefresh > 300) {
            lastPeriodicRefresh = now;
            refreshPanels();
            if (craftPanel != null) craftPanel.infoPanel.tickCycles();
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        if (storageWin != null) {
            storageX = storageWin.x(); storageY = storageWin.y();
            storageW = storageWin.width(); storageH = storageWin.height();
        }
        if (craftWin != null) {
            craftX = craftWin.x(); craftY = craftWin.y();
            craftW = craftWin.width(); craftH = craftWin.height();
        }
        super.removed();
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, width, height, 0xCC000000, 0xCC000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hasShiftDown()) scheduleRefresh();
        // Let QTCraft widgets handle clicks first (grid, stations row, etc.)
        if (widgetDelegate.mouseClicked(mouseX, mouseY, button)) return true;
        // Store action into storage window (carrying item, click anywhere on storage window)
        if (!menu.getCarried().isEmpty() && storageWin != null && storageWin.isVisible()
                && mouseX >= storageWin.x() && mouseX <= storageWin.x() + storageWin.width()
                && mouseY >= storageWin.y() && mouseY <= storageWin.y() + storageWin.height()) {
            PacketDistributor.sendToServer(new MagicStoragePacket(0, menu.getCarried()));
            getStorageEntity(minecraft.player).setChanged();
            scheduleRefresh();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {}
}
