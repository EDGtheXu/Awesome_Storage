package com.github.edg_thexu.awesome_storage.compat.farmersdelight;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.event.RecipeWorkstation;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.util.WidgetTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;

@RecipeWorkstation(
        blocks = "farmersdelight:cutting_board",
        recipeTypes = "farmersdelight:cutting",
        adapter = CuttingBoardAdapter.class
)
public class CuttingBoardAdapter extends AbstractMagicCraftRecipeAdapter {

    public CuttingBoardAdapter() {
        super("farmersdelight:cutting");
    }

    @Override
    public ItemStack getResult(RecipeHolder recipe) {
        if (recipe.value() instanceof CuttingBoardRecipe cut) {
            return cut.getResultItem(Minecraft.getInstance().level.registryAccess());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients(RecipeHolder recipe) {
        if (recipe.value() instanceof CuttingBoardRecipe cut) {
            return cut.getIngredients();
        }
        return NonNullList.create();
    }

    @Override
    public void buildExtraInfo(QWidget container, RecipeHolder recipe) {
        if (!(recipe.value() instanceof CuttingBoardRecipe cut)) return;
        Ingredient tool = cut.getTool();
        if (tool == null || tool.getItems().length == 0) return;
        ItemStack toolStack = tool.getItems()[0];

        QLabel label = new QLabel(Component.translatable("awesome_storage.craft_info.tool").append(":"));
        label.setTextColor(new QColor(0xFFFFAA00));
        label.setParent(container);
        label.setGeometry(0, 0, container.width(), 12);

        QWidget slot = new QWidget() {
            @Override
            protected void paintEvent(QPaintEvent event) {
                QPainter p = event.painter();
                if (p == null) return;
                p.fillRect(0, 0, width(), height(), new QColor(0xFF333333));
                p.drawRect(0, 0, width(), height(), new QColor(0xFF555555));
                p.renderItemStack(toolStack, (width() - 16) / 2, (height() - 16) / 2);
            }
            @Override
            public WidgetTooltip toolTip() { return WidgetTooltip.create(toolStack); }
        };
        slot.setFixedSize(18, 18);
        slot.setParent(container);
        slot.move(0, 14);
    }
}
