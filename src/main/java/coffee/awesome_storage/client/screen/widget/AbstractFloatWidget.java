package coffee.awesome_storage.client.screen.widget;

import coffee.awesome_storage.client.screen.MagicStorageScreen;
import coffee.awesome_storage.menu.MagicStorageMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import static coffee.awesome_storage.utils.Util.*;
import static net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.renderSlotHighlight;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractFloatWidget extends AbstractContainerWidget {

    protected final MagicStorageMenu menu;
    protected final MagicStorageScreen screen;
    protected int left;
    protected int top;

    protected int internal = 18;
    protected int rowCount = 11;
    protected int page = 1;

    protected int lineCount;
    protected int pageCount;
    private int maxPage;
    public ItemStack hoverIt;
    protected int hoverIndex;
    private int count;

    private List<AbstractWidget> children = new ArrayList<>();
    protected AbstractButton nextBt;
    protected AbstractButton lastBt;
    private boolean addButton = true;


    public AbstractFloatWidget(MagicStorageScreen screen, int x, int y, int width, int height, Component message) {
        super(x, y, width/18*18, height, message);
        this.menu = screen.getMenu();
        this.screen = screen;
        this.left = x;
        this.top = y;

        if(this.addButton){
            lastBt = new AbstractButton(this.getX(),this.getY()-15,15,15,Component.literal("<")) {
                @Override
                public void onPress() {
                    page--;
                    if(page==0) page = maxPage;
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                }
            };

            nextBt = new AbstractButton(this.getX()+this.width-15,this.getY()-15,15,15,Component.literal(">")) {
                @Override
                public void onPress() {
                    if(maxPage > 0)
                        page = page % maxPage+1;
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
            };
            children.add(lastBt);
            children.add(nextBt);
        }

    }

    public AbstractFloatWidget setInternal(int internal){
        this.internal = internal;
        return this;
    }

    public AbstractFloatWidget setRowCount(int rowCount){
        this.rowCount = rowCount;
        return this;
    }

    public AbstractFloatWidget setNoRenderButton(){
        this.addButton = false;
        return this;
    }


    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if(!this.isHovered){
            return super.mouseClicked(pMouseX,pMouseY, pButton);
        }
        return false;
    }


    protected abstract List<ItemStack> getItems();

    protected int getNonEmptyItemsCount(){
        return count;
    }

    protected BiPredicate<ItemStack, Integer> overlay(){
        return (it,i)->false;
    }

    protected void appendHoverItemTooltip(List<Component> tooltip){

    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

        List<ItemStack> items = getItems();

        lineCount = width / internal;
        pageCount = lineCount * rowCount;
        maxPage = (items.size() + pageCount + 1) / pageCount;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.left, this.top, 0.0F);

        //渲染物品
        count = 0;
        boolean flag = true;
        for(int j=0;j < rowCount  && flag;j++){
            for(int i=0;i<lineCount;i++){
                int index = i + j * lineCount + (page -1)* pageCount;

                if(index >= items.size()){
                    flag = false;
                    break;
                }
                if(!items.get(index).isEmpty()){
                    renderItemStack(guiGraphics,items.get(index),i * internal,j * internal, overlay().test(items.get(index),index));
                    count++;
                }
            }
        }

        guiGraphics.pose().popPose();

        //渲染物品信息
        if (this.isHovered) {
            int resi = (mouseX - getX()) / internal;
            int resj = (mouseY - getY()) / internal;

            // 计算每页的物品数量
            int pageCount = lineCount * rowCount;

            // 计算总页数
            int totalPages = (items.size() + pageCount - 1) / pageCount;

            // 边界检查，确保 resi 和 resj 在有效范围内，并且 page 在有效范围内
            if (resi >= 0 && resi < lineCount && resj >= 0 && resj < rowCount && page >= 1 && page <= totalPages) {
                int index = resi + resj * lineCount + (page - 1) * pageCount;
                hoverIndex = index;
                if (index >= 0 && index < items.size()) { // 增加 index 的边界检查
                    int row = index / lineCount;
                    int col = index % lineCount;
                    int x = col * internal + getX();
                    int y = row%rowCount * internal + getY();
                    hoverIt = items.get(index);
                    if (!hoverIt.isEmpty()) {
                        renderSlotHighlight(guiGraphics, x, y, internal);
//                        guiGraphics.renderTooltip(Minecraft.getInstance().font, hoverIt, mouseX, mouseY);

                        List<Component> tooltip = hoverIt.getTooltipLines(Item.TooltipContext.of(Minecraft.getInstance().level), Minecraft.getInstance().player, TooltipFlag.NORMAL);

                        appendHoverItemTooltip(tooltip);
//                        tooltip.add(Component.literal("missing").withColor(0XFF0000));
                        guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltip,
                                hoverIt.getTooltipImage(),mouseX, mouseY);

                    }
                } else {
                    hoverIt = null;
                }
            } else {
                hoverIt = null;
            }
        } else {
            hoverIt = null;
        }

        if(this.addButton){
            lastBt.render(guiGraphics,mouseX,mouseY,partialTicks);
            nextBt.render(guiGraphics,mouseX,mouseY,partialTicks);

            //容器信息
            String pageInfo = page + "/" + maxPage;
            guiGraphics.drawString(Minecraft.getInstance().font, pageInfo ,this.getX()+this.width/2 - Minecraft.getInstance().font.width(pageInfo)/2,this.getY()-10,0xffffff);

        }

//        if(this.isHoveredResult)
//            guiGraphics.drawString(Minecraft.getInstance().font, "Magic Storage", mouseX, mouseY, 0xFFFFFF);

    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public List<AbstractWidget> children() {
        return List.of(lastBt,nextBt);
    }



}
