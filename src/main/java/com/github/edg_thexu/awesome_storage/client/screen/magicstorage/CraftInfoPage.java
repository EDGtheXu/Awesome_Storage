package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.modelview.QAbstractItemModel;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.button.QPushButton;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QToast;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QCompleter;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QLineEdit;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class CraftInfoPage extends QWidget {

    private final QSmoothScrollArea scroll;
    private QLineEdit recipeTypeInput;
    private QLineEdit blockInput;
    private QWidget content;

    public CraftInfoPage() {
        QVBoxLayout vl = new QVBoxLayout(this);
        vl.setSpacing(4);
        vl.setContentsMargins(5, 5, 5, 5);

        scroll = new QSmoothScrollArea();
        scroll.setWidgetResizable(true);
        vl.addWidget(scroll, 1);

        // Add form
        QWidget form = createAddForm();
        vl.addWidget(form);
    }

    private QWidget createAddForm() {
        QWidget form = new QWidget();
        QVBoxLayout layout = new QVBoxLayout(form);
        layout.setSpacing(4);
        layout.setContentsMargins(0, 4, 0, 0);


        QHBoxLayout row = new QHBoxLayout();
        QLabel addTitle = new QLabel(Component.literal("Add New Recipe"));
        addTitle.setTextColor(new QColor(0xFF00AAFF));
        row.addWidget(addTitle);
        QPushButton addBtn = new QPushButton(Component.literal("+"));
        addBtn.setFixedSize(16, 16);
        addBtn.setOnClick(this::onAddRecipe);
        row.addWidget(addBtn);
        layout.addLayout(row);

//        QHBoxLayout inputRow = new QHBoxLayout();
//        inputRow.setSpacing(4);

        recipeTypeInput = new QLineEdit();
        recipeTypeInput.setPlaceholderText("recipe type (e.g. minecraft:smelting)");
        QCompleter typeCompleter = new QCompleter();
        List<String> recipeTypes = new ArrayList<>();
        for (RecipeType<?> rt : BuiltInRegistries.RECIPE_TYPE) {
            ResourceLocation key = BuiltInRegistries.RECIPE_TYPE.getKey(rt);
            if (key != null) recipeTypes.add(key.toString());
        }
        typeCompleter.setCompletions(recipeTypes);
        typeCompleter.setCaseSensitive(false);
        typeCompleter.setMaxVisibleItems(20);
        recipeTypeInput.setCompleter(typeCompleter);
        layout.addWidget(recipeTypeInput, 1);

        blockInput = new QLineEdit();
        blockInput.setPlaceholderText("block (e.g. minecraft:furnace)");
        QCompleter blockCompleter = new QCompleter();
        List<String> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key != null) blocks.add(key.toString());
        }
        blockCompleter.setCompletions(blocks);
        blockCompleter.setCaseSensitive(false);
        blockCompleter.setMaxVisibleItems(20);

        blockInput.setCompleter(blockCompleter);
        layout.addWidget(blockInput, 1);

        return form;
    }

    private void onAddRecipe() {
        String recipeTypeId = recipeTypeInput.text().trim();
        String blockId = blockInput.text().trim();

        if (recipeTypeId.isEmpty() || blockId.isEmpty()) {
            showFeedback("Input cannot be empty", true);
            return;
        }

        boolean success = false;
        try {
            success = CraftConfig.INSTANCE().addRecipeBlock(recipeTypeId, blockId);
        } catch (Exception e) {
            showFeedback("Error: " + e.getMessage(), true);
            return;
        }

        if (success) {
            showFeedback("Added successfully!", false);
            recipeTypeInput.clear();
            blockInput.clear();
            refresh();
        } else {
            showFeedback("Invalid recipe type or block ID", true);
        }
    }

    private void showFeedback(String text, boolean isError) {
        QToast.show(this, text, isError ? QToast.Type.Error : QToast.Type.Success);
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
            QLabel empty = new QLabel(Component.translatable("awesome_storage.magic_storage_screen.craft_info_empty"));
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
            QLabel sep = new QLabel(Component.translatable("awesome_storage.magic_storage_screen.craft_info_unsupported"));
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
