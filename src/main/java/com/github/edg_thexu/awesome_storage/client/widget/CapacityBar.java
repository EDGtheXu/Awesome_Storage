package com.github.edg_thexu.awesome_storage.client.widget;

import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import net.minecraft.network.chat.Component;

public class CapacityBar extends QWidget {

    private int usedSlots;
    private int totalSlots;

    public CapacityBar() {
        setFixedHeight(16);
    }

    public void setSlots(int used, int total) {
        this.usedSlots = used;
        this.totalSlots = Math.max(total, 1);
        markDirty();
        update();
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter p = event.painter();
        if (p == null) return;

        int w = width(), h = height();
        double ratio = Math.min(1.0, (double) usedSlots / totalSlots);
        int barW = (int) (w * ratio);

        p.fillRect(0, 0, w, h, new QColor(0xFF1A1A2E));

        QColor barColor;
        if (ratio < 0.5) barColor = new QColor(0xFF44AA44);
        else if (ratio < 0.8) barColor = new QColor(0xFFAAAA44);
        else barColor = new QColor(0xFFAA4444);
        p.fillRect(0, 0, barW, h, barColor);

        p.setColor(new QColor(0xFF555555));
        p.drawRect(0, 0, w, h);

        String text = usedSlots + "/" + totalSlots + " (" + (int) (ratio * 100) + "%)";
        p.setColor(QColor.WHITE);
        p.drawText(text, 4, (h - p.textHeight()) / 2);
    }
}
