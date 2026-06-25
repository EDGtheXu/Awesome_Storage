package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class CraftInfoPage extends QWidget {

    private final QSmoothScrollArea scroll;
    private QWidget content;

    public CraftInfoPage() {
        QVBoxLayout vl = new QVBoxLayout(this);
        vl.setSpacing(4);
        vl.setContentsMargins(5, 5, 5, 5);

        QLabel title = new QLabel(Component.translatable("magic_storage_screen.craft_info_title"));
        title.setTextColor(new QColor(0xFFFFAA00));
        vl.addWidget(title);

        scroll = new QSmoothScrollArea();
        scroll.setWidgetResizable(true);
        vl.addWidget(scroll, 1);
    }

    public void refresh() {
        content = new QWidget();
        QVBoxLayout layout = new QVBoxLayout(content);
        layout.setSpacing(6);
        layout.setContentsMargins(5, 5, 5, 5);

        // Supported recipes
        for (var entry : CraftConfig.ENABLED_RECIPES.entrySet()) {
            RecipeType<?> recipeType = entry.getKey();
            var blocks = entry.getValue();

            String typeName = BuiltInRegistries.RECIPE_TYPE.getKey(recipeType).toString();
            QLabel sectionLabel = new QLabel(Component.literal(typeName));
            sectionLabel.setTextColor(new QColor(0xFFFFAA00));
            layout.addWidget(sectionLabel);

            for (Block block : blocks) {
                QHBoxLayout blockRow = new QHBoxLayout();
                blockRow.setSpacing(4);

                ItemStack stack = new ItemStack(block);
                QWidget icon = new QWidget() {
                    @Override
                    protected void paintEvent(QPaintEvent event) {
                        QPainter p = event.painter();
                        if (p == null) return;
                        MagicStorageScreen.renderSlot(p, 0, 0, 18, stack, false);
                    }
                };
                icon.setFixedSize(18, 18);
                blockRow.addWidget(icon);

                String blockName = stack.getHoverName().getString();
                QLabel nameLabel = new QLabel(Component.literal(blockName));
                nameLabel.setTextColor(QColor.WHITE);
                nameLabel.setFixedWidth(fontWidth(blockName) + 2);
                blockRow.addWidget(nameLabel);
                blockRow.addStretch(1);

                layout.addLayout(blockRow);
            }
        }

        if (CraftConfig.ENABLED_RECIPES.isEmpty()) {
            QLabel empty = new QLabel(Component.translatable("magic_storage_screen.craft_info_empty"));
            empty.setTextColor(QColor.GRAY);
            layout.addWidget(empty);
        }

        // Unsupported recipes
        List<RecipeType<?>> unsupported = new ArrayList<>();
        for (RecipeType<?> rt : BuiltInRegistries.RECIPE_TYPE) {
            if (!CraftConfig.ENABLED_RECIPES.containsKey(rt)) {
                unsupported.add(rt);
            }
        }

        if (!unsupported.isEmpty()) {
            QLabel sep = new QLabel(Component.translatable("magic_storage_screen.craft_info_unsupported"));
            sep.setTextColor(new QColor(0xFFAA4444));
            layout.addWidget(sep);

            for (RecipeType<?> rt : unsupported) {
                String name = BuiltInRegistries.RECIPE_TYPE.getKey(rt).toString();
                QLabel item = new QLabel(Component.literal(name));
                item.setTextColor(QColor.GRAY);
                layout.addWidget(item);
            }
        }

        layout.addStretch(1);
        scroll.setWidget(content);
    }
}
