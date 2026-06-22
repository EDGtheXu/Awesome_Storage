package coffee.awesome_storage.client.screen;

import com.github.edg_thexu.qtcraft_api.core.QTheme;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.geometry.QPoint;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QCursor;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QDockWidget;

public class FloatingWindow extends QDockWidget {
    private static final int RESIZE_BORDER = 5;
    private static final int TITLE_H = 22;
    private static final int MIN_W = 250;
    private static final int MIN_H = 200;

    private int resizeEdge;
    private int dragStartX, dragStartY;
    private int dragStartWinX, dragStartWinY;
    private int dragStartWinW, dragStartWinH;

    public FloatingWindow(String title) {
        super(title);
        setMinimumSize(MIN_W, MIN_H);
    }

    private int getResizeEdge(int mx, int my) {
        int w = width(), h = height();
        boolean left = mx < RESIZE_BORDER, right = mx > w - RESIZE_BORDER;
        boolean top = my < RESIZE_BORDER, bottom = my > h - RESIZE_BORDER;
        if (top && left) return 5;
        if (top && right) return 6;
        if (bottom && left) return 7;
        if (bottom && right) return 8;
        if (left) return 1;
        if (right) return 2;
        if (top) return 3;
        if (bottom) return 4;
        return 0;
    }

    private boolean isInTitleBar(int mx, int my) {
        return my >= RESIZE_BORDER && my < TITLE_H && mx >= RESIZE_BORDER && mx < width() - RESIZE_BORDER - 20;
    }

    private boolean isInClose(int mx, int my) {
        return my >= RESIZE_BORDER && my < TITLE_H && mx >= width() - RESIZE_BORDER - 20 && mx < width() - RESIZE_BORDER;
    }

    @Override
    protected void mousePressEvent(QMouseEvent event) {
        if (event.button() == QMouseEvent.Button.Left) {
            resizeEdge = getResizeEdge(event.x(), event.y());
            if (resizeEdge > 0) {
                QPoint gp = mapToGlobal(event.pos());
                dragStartX = gp.x(); dragStartY = gp.y();
                dragStartWinX = x(); dragStartWinY = y();
                dragStartWinW = width(); dragStartWinH = height();
                QWidget.grabMouse(this);
                event.accept();
                return;
            }
            if (isInClose(event.x(), event.y())) { close(); event.accept(); return; }
            if (isInTitleBar(event.x(), event.y())) {
                QPoint gp = mapToGlobal(event.pos());
                dragStartX = gp.x(); dragStartY = gp.y();
                dragStartWinX = x(); dragStartWinY = y();
                resizeEdge = -1;
                QWidget.grabMouse(this);
                raise();
                event.accept();
                return;
            }
        }
        super.mousePressEvent(event);
    }

    @Override
    protected void mouseMoveEvent(QMouseEvent event) {
        if (resizeEdge != 0) {
            QPoint gp = mapToGlobal(event.pos());
            int dx = gp.x() - dragStartX, dy = gp.y() - dragStartY;
            int newX = dragStartWinX, newY = dragStartWinY, newW = dragStartWinW, newH = dragStartWinH;

            if (resizeEdge > 0) {
                switch (resizeEdge) {
                    case 1 -> { newX = dragStartWinX + dx; newW = dragStartWinW - dx; }
                    case 2 -> { newW = dragStartWinW + dx; }
                    case 3 -> { newY = dragStartWinY + dy; newH = dragStartWinH - dy; }
                    case 4 -> { newH = dragStartWinH + dy; }
                    case 5 -> { newX = dragStartWinX + dx; newW = dragStartWinW - dx; newY = dragStartWinY + dy; newH = dragStartWinH - dy; }
                    case 6 -> { newW = dragStartWinW + dx; newY = dragStartWinY + dy; newH = dragStartWinH - dy; }
                    case 7 -> { newX = dragStartWinX + dx; newW = dragStartWinW - dx; newH = dragStartWinH + dy; }
                    case 8 -> { newW = dragStartWinW + dx; newH = dragStartWinH + dy; }
                }
                if (newW < MIN_W) { if (resizeEdge == 1 || resizeEdge == 5 || resizeEdge == 7) newX = dragStartWinX + dragStartWinW - MIN_W; newW = MIN_W; }
                if (newH < MIN_H) { if (resizeEdge == 3 || resizeEdge == 5 || resizeEdge == 6) newY = dragStartWinY + dragStartWinH - MIN_H; newH = MIN_H; }
                setGeometry(newX, newY, newW, newH);
            } else {
                move(dragStartWinX + dx, dragStartWinY + dy);
            }
            event.accept();
            return;
        }

        int edge = getResizeEdge(event.x(), event.y());
        if (edge > 0) {
            setCursor(new QCursor(
                edge == 1 || edge == 2 ? QCursor.Shape.ResizeHorizontal :
                edge == 3 || edge == 4 ? QCursor.Shape.ResizeVertical :
                QCursor.Shape.ResizeDiagonal
            ));
        } else {
            setCursor(null);
        }
        super.mouseMoveEvent(event);
    }

    @Override
    protected void mouseReleaseEvent(QMouseEvent event) {
        if (resizeEdge != 0) {
            resizeEdge = 0;
            QWidget.releaseMouse();
            event.accept();
            return;
        }
        super.mouseReleaseEvent(event);
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter p = event.painter();
        if (p == null) return;

        p.fillRect(0, 0, width(), height(), new QColor(0xCC1E1E1E));
        p.setColor(new QColor(0xFF555555));
        p.drawRect(0, 0, width(), height());

        p.fillRect(1, 1, width() - 2, TITLE_H, new QColor(0xFF2C2C2C));
        p.setColor(QColor.WHITE);
        String title = windowTitle().isEmpty() ? "Window" : windowTitle();
        p.drawText(title, 6, (TITLE_H - p.textHeight()) / 2 + 1);
        p.setColor(new QColor(0xFFFF4444));
        p.drawText("X", width() - 16, (TITLE_H - p.textHeight()) / 2 + 1);
    }
}
