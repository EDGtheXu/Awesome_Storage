package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.api.event.RegisterScreenPageEvent;
import com.github.edg_thexu.awesome_storage.core.network.c2s.RenameBlockPacket;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.SignalSlotUtil;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QLineEdit;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ControllerWidget  {

    public static QWidget create(RegisterScreenPageEvent.Storage additional) {
        QSmoothScrollArea scrollArea = new QSmoothScrollArea();
        scrollArea.setWidgetResizable(true);
        QWidget content = new QWidget();
        QVBoxLayout layout = new QVBoxLayout(content);
        layout.setContentsMargins(5, 5, 5, 5);
        QHBoxLayout nameLayout = new QHBoxLayout();

        QLineEdit rename = new QLineEdit();
        rename.setPlaceholderText(Component.translatable("magic_storage_screen.rename").getString());
        rename.setText(Util.getStorageEntity(Minecraft.getInstance().player).displayName.getString());
        nameLayout.addWidget(rename, 1);

        QLabel name = new QLabel();
        name.setText(Component.literal(rename.text()));
        nameLayout.addWidget(name, 1);
        rename.connect(QLineEdit.RETURN_PRESSED, rename, SignalSlotUtil.createSlotRunner("rename", obj ->{
            name.setText(Component.literal(rename.text()));
            if(!rename.text().isEmpty()) {
                rename.clearFocus();
                PacketDistributor.sendToServer(new RenameBlockPacket(name.text(), Util.getStorageEntity(Minecraft.getInstance().player).getBlockPos()));
            }
        }));

        layout.addLayout(nameLayout);
        additional.addButtons(layout, Minecraft.getInstance().player);

        scrollArea.setWidget(content);
        return scrollArea;
    }

}
