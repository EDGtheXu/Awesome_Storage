package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.api.event.RegisterScreenPageEvent;
import com.github.edg_thexu.awesome_storage.client.widget.FloatingWindow;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.block.StorageCoreBlock;
import com.github.edg_thexu.awesome_storage.core.menu.MagicStorageMenu;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicStoragePacket;
import com.github.edg_thexu.awesome_storage.utils.FavoriteSystem;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.client.painter.ModernDrawDevice;
import com.github.edg_thexu.qtcraft_api.client.screen.QContainerWidgetScreen;
import com.github.edg_thexu.qtcraft_api.client.screen.WidgetScreenDelegate;
import com.github.edg_thexu.qtcraft_api.core.QTheme;
import com.github.edg_thexu.qtcraft_api.core.geometry.QPoint;
import com.github.edg_thexu.qtcraft_api.core.geometry.QSize;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.SignalSlotUtil;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QContainer;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QMainWindow;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QSlot;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static com.github.edg_thexu.awesome_storage.utils.Util.getStorageEntity;


public class MagicStorageScreen extends QContainerWidgetScreen<MagicStorageMenu> {

    private FloatingWindow storageWin;
    private FloatingWindow craftWin;
    private boolean storageOnly;
    private StoragePanel storagePanel;
    private CraftPanel craftPanel;
    private QueuePage queuePage;
    private StorageStatsPage statsPage;
    private UpgradePage upgradePage;
    private CraftUpgradePage craftUpgradePage;
    private CraftInfoPage craftInfoPage;
    private long nextRefresh;
    private long lastPeriodicRefresh;
    private List<String> lastAccessors = new ArrayList<>();

    private static int storageX = 200, storageY = 30, storageW = 220, storageH = 220;
    private static int craftX = 200, craftY = 30, craftW = 220, craftH = 220;

    private boolean handleFavoriteClick(double mouseX, double mouseY) {
        QWidget root = rootWindow();
        if (root == null) return false;
        QWidget target = root.childAt((int) mouseX, (int) mouseY);
        if (target instanceof QSlot qslot) {
            int slotIndex = qslot.slotIndex();
            if (slotIndex >= 0 && slotIndex < 36) {
                ItemStack stack = qslot.itemStack();
                if (!stack.isEmpty()) {
                    FavoriteSystem.getInstance().toggleFavorite(slotIndex);
                    FavoriteSystem.getInstance().saveFavorites();
                    return true;
                }
            }
        }
        return false;
    }

    private QPoint getWidgetScreenPos(QWidget widget) {
        int x = 0, y = 0;
        QWidget w = widget;
        while (w != null) {
            x += w.x();
            y += w.y();
            var p = w.parent();
            w = (p instanceof QWidget qw) ? qw : null;
        }
        return new QPoint(x, y);
    }

    private void drawFavoriteBorders(net.minecraft.client.gui.GuiGraphics guiGraphics) {
        if (FavoriteSystem.getInstance().isEmpty()) return;
        QWidget invWidget = menu.getSlotSource().getRoot();
        if (invWidget == null) return;
        QPainter painter = new QPainter(new ModernDrawDevice(guiGraphics));
        for (var child : invWidget.children()) {
            if (child instanceof QContainer container) {
                for (QSlot slot : container.slots()) {
                    int slotIndex = slot.slotIndex();
                    if (slotIndex >= 0 && slotIndex < 36 && FavoriteSystem.getInstance().isFavorited(slotIndex)) {
                        ItemStack stack = slot.itemStack();
//                        if (!stack.isEmpty()) {
                            QPoint pos = getWidgetScreenPos(slot);
                            int s = 20;
                            this.drawFavoriteBorders(painter, pos.x() - 1, pos.y() - 1, s);
                    }
                }
            }
        }
    }

    private void drawFavoriteBorders(QPainter painter, int x, int y, int s) {
        painter.drawRoundRect(x, y, s, s, 3, 1.5f, new QColor(0xFF4444FF));

    }

    public MagicStorageScreen(MagicStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.widgetDelegate = new WidgetScreenDelegate(this) {
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                if (this.rootWindow != null) {
                    QPainter painter = new QPainter(new ModernDrawDevice(guiGraphics));
                    this.renderWidgetTree(painter, mouseX, mouseY, partialTick);
                }
            }
        };
    }

    @Override
    protected QMainWindow createRootWindow() {
        QMainWindow win = new QMainWindow();
        win.setGeometry(0, 0, width, height);

        FavoriteSystem.getInstance().loadFavorites();

        // Player inventory widget from slot source, positioned at top-left
        QWidget invWidget = menu.getSlotSource().getRoot();
        if (invWidget != null) {
            invWidget.setParent(win);
            QSize hint = invWidget.sizeHint();
            invWidget.setGeometry(5, 5, Math.max(hint.width(), 170), hint.height());
        }

        FavoriteSystem.getInstance().validate(menu.getItems());

        // Determine mode
        MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
        storageOnly = be != null && be.getBlockState().getBlock() instanceof StorageCoreBlock;

        if (!storageOnly) {
            // Craft window
            craftPanel = new CraftPanel(this);
            craftWin = new FloatingWindow(Component.translatable("magic_storage_screen.craft").getString());
            craftWin.setParent(win);

            craftWin.addPage(craftWin.windowTitle(), craftPanel);
            ModLoader.postEventWithReturn(new RegisterScreenPageEvent.Craft(craftWin)).buildPages();

            // Queue page
            queuePage = new QueuePage(this);
            craftWin.addPage(Component.translatable("magic_storage_screen.queue_title").getString(), queuePage);

            craftUpgradePage = new CraftUpgradePage();
            craftWin.addPage(Component.translatable("magic_storage_screen.queue_upgrade_title").getString(), craftUpgradePage);

            craftInfoPage = new CraftInfoPage();
            craftWin.addPage(Component.translatable("magic_storage_screen.craft_info_title").getString(), craftInfoPage);

            craftWin.connectPages();

            craftWin.setWidget(craftPanel);
            craftWin.setGeometry(craftX, craftY, craftW, craftH);
            craftPanel.updateLayout();
            craftWin.connect(FloatingWindow.ON_CLOSE, craftWin, SignalSlotUtil.createSlotRunner("close", (obj)-> onClose()));


        }else{
            // Storage window
            storagePanel = new StoragePanel(this);
            storageWin = new FloatingWindow(Component.translatable("magic_storage_screen.storage").getString());
            storageWin.setParent(win);

            storageWin.addPage(storageWin.windowTitle(), storagePanel);
            var event = ModLoader.postEventWithReturn(new RegisterScreenPageEvent.Storage(craftWin));
            storageWin.addPage(Component.translatable("magic_storage_screen.controller").getString(), ControllerWidget.create(event));
            event.buildPages();
            statsPage = new StorageStatsPage();
            storageWin.addPage(Component.translatable("magic_storage_screen.stats_title").getString(), statsPage);
            upgradePage = new UpgradePage();
            QSmoothScrollArea area = new QSmoothScrollArea();
            area.setWidget(upgradePage);
            area.setWidgetResizable(true);
            storageWin.addPage(Component.translatable("magic_storage_screen.upgrade_title").getString(), area);
            storageWin.connectPages();

            storageWin.setWidget(storagePanel);
            storageWin.setGeometry(storageX, storageY, storageW, storageH);
            storagePanel.updateLayout();
            storageWin.connect(FloatingWindow.ON_CLOSE, storageWin, SignalSlotUtil.createSlotRunner("close", (obj)-> onClose()));

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
        p.push();
        p.translate(x, y);
        x = 0;
        y = 0;
        p.fillRect(x, y, size, size, new QColor(0xFF333333));
        if (overlay) p.fillRect(x, y, size, size, new QColor(0x55FF0000));
        if (highlight) p.fillRoundRect(x, y, size, size, 3, new QColor(0x55FFFFFF));
        if (!stack.isEmpty()) {
            p.renderItemStack(stack, x + (size - 16) / 2, y + (size - 16) / 2);
            int count = stack.getCount();
            if (stack.isBarVisible()) {
                p.push();
                p.translate(0, 0, 190);
                int l = stack.getBarWidth();
                int i = stack.getBarColor();
                int j = x + 2;
                int k = y + 13;
                p.fillRect(j, k, 13, 2, new QColor(-16777216));
                p.fillRect(j, k, l, 1, new QColor(i | -16777216));
                p.pop();
            }
            if (count > 1) {
                p.push();
                p.translate(size, size, 200);
                p.scale(0.7f, 0.7f);
                String countStr = formatCount(count);
                int tw = p.textWidth(countStr) + 2;

                if(count > 999) {
                    p.setColor(QTheme.TEXT.title);
                }else{
                    p.setColor(QColor.WHITE);
                }
                p.drawText(countStr, x - tw, y - p.textHeight());

                p.pop();
            }
        }
        p.pop();
    }


    static String formatCount(int count) {
        if (count >= 1000000) return String.format("%.1fM", count / 1000000.0);
        if (count >= 1000) return String.format("%.1fk", count / 1000.0);
        return String.valueOf(count);
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
        if (queuePage != null) queuePage.refresh();
        if (statsPage != null) statsPage.refresh();
        if (upgradePage != null) upgradePage.refresh();
        if (craftUpgradePage != null) craftUpgradePage.refresh();
        if (craftInfoPage != null) craftInfoPage.refresh();
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
//        updateFavoriteTracking();
        guiGraphics.pose().pushPose();
        drawFavoriteBorders(guiGraphics);
        QPainter painter = new QPainter(new ModernDrawDevice(guiGraphics));
        if(FavoriteSystem.getInstance().clickedSlotWasFav && !menu.getCarried().isEmpty()) {
            painter.translate(0, 0, 350);
            painter.fillRoundRect(mouseX - 10, mouseY - 10, 20, 20, 6, new QColor(0xFF4444FF));
        }
        guiGraphics.pose().popPose();

        this.widgetDelegate.paintTooltip(painter, mouseX, mouseY);
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch(); // jei render conflict
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
        // Alt+Left Click on player inventory slots: toggle favorite
        if (hasAltDown() && button == 0) {
            if (handleFavoriteClick(mouseX, mouseY)) return true;
        }
        // Record clicked slot for favorite tracking
        if (button == 0) {
            QWidget root = rootWindow();
            if (root != null) {
                QWidget target = root.childAt((int) mouseX, (int) mouseY);
                if (target instanceof QSlot qslot) {
                    int idx = qslot.slotIndex();
                    if (idx >= 0 && idx < 36) {
                        if(FavoriteSystem.getInstance().isFavorited(idx) && hasShiftDown()) {
                            return true;
                        }
                        if(ItemStack.isSameItemSameComponents(qslot.itemStack(), menu.getCarried())
                                && menu.getCarried().getMaxStackSize() > 1) {
                            if(FavoriteSystem.getInstance().clickedSlotWasFav) {
                                FavoriteSystem.getInstance().toggleFavorite(idx, true);
                                FavoriteSystem.getInstance().clickedSlotWasFav = false;
                            }
                        }else{
                            FavoriteSystem.getInstance().exchange(idx, !qslot.itemStack().isEmpty());
                        }
                    }
                }
            }
        }
        // Let QTCraft widgets handle clicks first (grid, stations row, etc.)
//        if (widgetDelegate.mouseClicked(mouseX, mouseY, button)) return true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if(slotId == -999) {
            // forbidden drop favorited item
            if(FavoriteSystem.getInstance().clickedSlotWasFav) {
                return;
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    public void onClose() {
        super.onClose();
        FavoriteSystem.getInstance().saveFavorites();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        FavoriteSystem.getInstance().saveFavorites();
        super.resize(minecraft, width, height);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) {
            // avoid closing when press E
            return this.widgetDelegate.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
//        return false;
    }
}
