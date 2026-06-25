package com.github.edg_thexu.awesome_storage.client.screen;

import com.github.edg_thexu.awesome_storage.core.menu.StorageArrayMenu;
import com.github.edg_thexu.qtcraft_api.client.painter.ModernDrawDevice;
import com.github.edg_thexu.qtcraft_api.client.screen.QContainerWidgetScreen;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QMainWindow;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public class StorageArrayScreen extends QContainerWidgetScreen<StorageArrayMenu> {

    public StorageArrayScreen(StorageArrayMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected QMainWindow createRootWindow() {
        QMainWindow win = new QMainWindow();
        win.setCentralWidget(menu.getSlotSource().getRoot());
        return win;
    }

    @Override
    protected void init() {
        super.init();
        if (rootWindow() != null) {
            rootWindow().setGeometry((int) (width * 0.2F), 10, (int) (width * 0.6f), height - 20);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        QPainter painter = new QPainter(new ModernDrawDevice(guiGraphics));

        // Draw capacity bar below the widget tree
        if (rootWindow() != null) {
            QWidget central = rootWindow().centralWidget();
            if (central != null) {
                int barY = rootWindow().y() + central.y() + central.height() + 4;
                int barX = rootWindow().x() + 8;
                int barW = central.width() - 16;
                int barH = 14;

                int used = menu.getUsedItemCount();
                int total = menu.getTotalCapacity();

                if (total > 0) {
                    double ratio = Math.min(1.0, (double) used / total);
                    int fillW = (int) (barW * ratio);

                    painter.fillRect(barX, barY, barW, barH, new QColor(0xFF1A1A2E));
                    QColor barColor = ratio < 0.5 ? new QColor(0xFF44AA44)
                            : ratio < 0.8 ? new QColor(0xFFAAAA44)
                            : new QColor(0xFFAA4444);
                    painter.fillRect(barX, barY, fillW, barH, barColor);
                    painter.setColor(new QColor(0xFF555555));
                    painter.drawRect(barX, barY, barW, barH);

                    String text = used + "/" + total + " (" + (int) (ratio * 100) + "%)";
                    painter.setColor(QColor.WHITE);
                    painter.drawText(text, barX + 4, barY + (barH - painter.textHeight()) / 2);
                }
            }
        }

        this.widgetDelegate.paintTooltip(painter, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, width, height, 0xCC000000, 0xCC000000);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) return this.widgetDelegate.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
