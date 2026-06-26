package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QMenu;

class ForeShowMenu extends QMenu {

    public ForeShowMenu() {
        super();
    }

    public ForeShowMenu(String title) {
        super(title);
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        event.painter().push();
        event.painter().translate(0, 0, 300);
        super.paintEvent(event);
        event.painter().pop();
    }

}
