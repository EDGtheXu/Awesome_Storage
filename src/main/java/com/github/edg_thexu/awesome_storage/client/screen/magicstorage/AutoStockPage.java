package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.utils.AutoStockSystem;
import com.github.edg_thexu.qtcraft_api.core.QTheme;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoStockPage extends QWidget {

    private final QSmoothScrollArea scroll;
    private QWidget content;

    public AutoStockPage() {
        QVBoxLayout vl = new QVBoxLayout(this);
        vl.setSpacing(4);
        vl.setContentsMargins(5, 5, 5, 5);

        QLabel title = new QLabel(Component.translatable("awesome_storage.magic_storage_screen.auto_stock_desc"));
        title.setTextColor(QTheme.TEXT.secondary);
        vl.addWidget(title);

        scroll = new QSmoothScrollArea();
        scroll.setWidgetResizable(true);
        vl.addWidget(scroll, 1);
    }

    @Override
    public void setVisible(boolean visible){
        if(this.isVisible() != visible && visible) {
            this.refresh();
        }
        super.setVisible(visible);
    }

    public void refresh() {
        content = new QWidget();
        QVBoxLayout layout = new QVBoxLayout(content);
        layout.setSpacing(2);
        layout.setContentsMargins(5, 5, 5, 5);

        Map<String, Integer> targets = AutoStockSystem.getInstance().getAllTargets();

        if (targets.isEmpty()) {
            QLabel empty = new QLabel(Component.translatable("awesome_storage.magic_storage_screen.auto_stock_empty"));
            empty.setTextColor(QColor.GRAY);
            layout.addWidget(empty);
        } else {
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(targets.entrySet());
            sorted.sort(Map.Entry.comparingByKey());

            for (var entry : sorted) {
                String itemId = entry.getKey();
                int count = entry.getValue();
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId)));
                if (stack.isEmpty()) continue;

                QWidget row = new QWidget();
                row.setStyleSheet("background-color: #33FFFFFF;");
                QHBoxLayout rowLayout = new QHBoxLayout(row);
                rowLayout.setSpacing(4);
                rowLayout.setContentsMargins(4, 2, 4, 2);

                // Item icon
                QWidget icon = new QWidget() {
                    @Override
                    protected void paintEvent(QPaintEvent event) {
                        QPainter p = event.painter();
                        if (p == null) return;
                        p.renderItemStack(stack, 1, 1);
                    }
                };
                icon.setFixedSize(18, 18);
                rowLayout.addWidget(icon);

                // Item name
                String name = stack.getHoverName().getString();
                if (name.length() > 22) name = name.substring(0, 22) + "...";
                QLabel nameLabel = new QLabel(Component.literal(name));
                nameLabel.setTextColor(QColor.WHITE);
                rowLayout.addWidget(nameLabel, 1);

                // Current count
                QLabel countLabel = new QLabel(Component.literal("x" + count));
                countLabel.setTextColor(new QColor(0xFFFFAA00));
                rowLayout.addWidget(countLabel);

                // -1 button
                QWidget decBtn = new QWidget() {
                    @Override
                    protected void paintEvent(QPaintEvent event) {
                        QPainter p = event.painter();
                        if (p == null) return;
                        p.fillRect(0, 0, width(), height(), isHovered() ? new QColor(0xFF555555) : new QColor(0xFF333333));
                        p.setColor(QColor.WHITE);
                        p.drawText("-", (width() - p.textWidth("-")) / 2, (height() - p.textHeight()) / 2);
                    }
                    @Override
                    protected void mousePressEvent(QMouseEvent e) {
                        e.accept();
                        int newCount = count;
                        if(e.button() == QMouseEvent.Button.Right) {
                            newCount -= 10;
                        } else if(e.button() == QMouseEvent.Button.Left) {
                            newCount -= 1;
                        }else if(e.button() == QMouseEvent.Button.Middle) {
                            newCount -= 64;
                        }
                        newCount = Math.max(newCount, 1);
                        AutoStockSystem.getInstance().setTarget(stack, newCount);
                        refresh();
                    }
                };
                decBtn.setFixedSize(16, 14);
                rowLayout.addWidget(decBtn);

                // +1 button
                QWidget incBtn = new QWidget() {
                    @Override
                    protected void paintEvent(QPaintEvent event) {
                        QPainter p = event.painter();
                        if (p == null) return;
                        p.fillRect(0, 0, width(), height(), isHovered() ? new QColor(0xFF555555) : new QColor(0xFF333333));
                        p.setColor(QColor.WHITE);
                        p.drawText("+", (width() - p.textWidth("+")) / 2, (height() - p.textHeight()) / 2);
                    }
                    @Override
                    protected void mousePressEvent(QMouseEvent e) {
                        e.accept();
                        int newCount = count;
                        if(e.button() == QMouseEvent.Button.Right) {
                            newCount += 10;
                        } else if(e.button() == QMouseEvent.Button.Left) {
                            newCount += 1;
                        }else if(e.button() == QMouseEvent.Button.Middle) {
                            newCount += 64;
                        }
                        AutoStockSystem.getInstance().setTarget(stack, newCount);
                        refresh();
                    }

                };
                incBtn.setFixedSize(16, 14);
                rowLayout.addWidget(incBtn);

                // Delete button
                QWidget delBtn = new QWidget() {
                    @Override
                    protected void paintEvent(QPaintEvent event) {
                        QPainter p = event.painter();
                        if (p == null) return;
                        p.fillRect(0, 0, width(), height(), isHovered() ? new QColor(0xFFCC4444) : new QColor(0xFF662222));
                        p.setColor(QColor.WHITE);
                        p.drawText("X", (width() - p.textWidth("X")) / 2, (height() - p.textHeight()) / 2);
                    }
                    @Override
                    protected void mousePressEvent(QMouseEvent e) {
                        e.accept();
                        AutoStockSystem.getInstance().removeTarget(stack);
                        refresh();
                    }
                };
                delBtn.setFixedSize(16, 14);
                rowLayout.addWidget(delBtn);

                layout.addWidget(row);
                row.setFixedHeight(22);
            }
        }

        content.setLayout(layout);
        scroll.setWidget(content);
    }
}
