package com.github.edg_thexu.awesome_storage.api.event;

import com.github.edg_thexu.awesome_storage.client.widget.FloatingWindow;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.layouts.QLayout;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.SignalSlotUtil;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.button.QPushButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Add a new page in left menu of storage/craft panel.
 */
public abstract class RegisterScreenPageEvent extends Event implements IModBusEvent {

    FloatingWindow window;
    private final List<Pair<String, QWidget>> pages = new ArrayList<>();

    public RegisterScreenPageEvent(FloatingWindow window) {
        this.window = window;
    }

    public void registerPage(String title, QWidget page) {
        pages.add(Pair.of(title, page));
    }

    public void buildPages() {
        for (Pair<String, QWidget> page : pages) {
            window.addPage(page.getLeft(), page.getRight());
        }
    }

    public static class Storage extends RegisterScreenPageEvent {
        private final List<Pair<Component, BiConsumer<MagicStorageBlockEntity, Player>>> controllerButtons = new ArrayList<>();
        public Storage(FloatingWindow window) {
            super(window);
        }
        public void registerControllerButton(Component title, BiConsumer<MagicStorageBlockEntity, Player> onClick) {
            controllerButtons.add(Pair.of(title, onClick));
        }

        public void addButtons(QLayout layout, Player player) {
            controllerButtons.forEach(pair -> {
                QPushButton button = new QPushButton(pair.getLeft());
                button.connect(QPushButton.CLICKED, button, SignalSlotUtil.createSlotRunner("click", obj -> {
                    pair.getRight().accept(Util.getStorageEntity(player), player);
                }));
                layout.addWidget(button, 0, QLayout.ALIGN_LEFT);
            });
        }
    }

    public static class Craft extends RegisterScreenPageEvent {
        public Craft(FloatingWindow window) {
            super(window);
        }
    }
}
