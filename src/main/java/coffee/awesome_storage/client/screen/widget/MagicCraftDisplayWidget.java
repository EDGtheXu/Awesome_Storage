package coffee.awesome_storage.client.screen.widget;

import coffee.awesome_storage.Awesome_storage;
import coffee.awesome_storage.network.NetworkHandler;
import coffee.awesome_storage.network.c2s.MagicCraftPacket;
import coffee.awesome_storage.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiPredicate;

import static coffee.awesome_storage.utils.Util.renderItemStack;
import static net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.renderSlotHighlight;

@OnlyIn(Dist.CLIENT)
public class MagicCraftDisplayWidget extends AbstractFloatWidget {

    int craftTabX;
    int craftTabY;
    int craftTabWidth;
    int craftTabHeight;
    boolean isClicked = false;
    int clickCount = 0;
    long lastClickTime = 0;

    MagicCraftWidget parent;
    List<ItemStack> cachedItems;
    boolean isHoveredResult = false;
    public MagicCraftDisplayWidget(MagicCraftWidget parent, int x, int y, int width, int height, Component message) {
        super(parent.screen, x, y, width, height, message);
        this.parent = parent;

        craftTabX = this.left - 10;
        craftTabY = this.top + 20;
        craftTabWidth = this.width;
        craftTabHeight = 20;
    }

    @Override
    protected List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>();
        if(parent.realIngredients!=null){
            for (var ingredient : parent.realIngredients.entrySet()) {
                int size =ingredient.getKey().getItems().length;
                if(size>0){
                    int offset = (int) (System.currentTimeMillis() % (size * 1000) / 1000);
                    ItemStack item = ingredient.getKey().getItems()[offset].copy();
                    item.setCount(ingredient.getValue());
                    items.add(item);
                }
            }
        }
        this.cachedItems = items;
        return items;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int pButton) {
        if(isHoveredResult){
            if(pButton == 0){
                isClicked = true;
            }
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            NetworkHandler.CHANNEL.sendToServer(new MagicCraftPacket(parent.selectedRecipe.getId(), BuiltInRegistries.RECIPE_TYPE.getKey(parent.selectedAdapter.getRecipe())));
            return true;
        }
       return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int pButton) {
        if(pButton == 0){
            if(isClicked) parent.refreshItems();
            isClicked = false;
            clickCount = 0;
        }

        return super.mouseReleased(mouseX, mouseY, pButton);
    }

    @Override
    protected BiPredicate<ItemStack,Integer> overlay(){
        return (it,i)->!(parent.haveIngredients!=null &&
                parent.haveIngredients.containsKey(it.getItem()) &&
                parent.haveIngredients.get(it.getItem()) >= it.getCount());
    }

    @Override
    protected void appendHoverItemTooltip(List<Component> tooltip){
        if(!parent.haveIngredients.containsKey(hoverIt.getItem()) || parent.haveIngredients.containsKey(hoverIt.getItem()) && parent.haveIngredients.get(hoverIt.getItem())<hoverIt.getCount())
            tooltip.add(Component.literal("missing  x "+(hoverIt.getCount() - (parent.haveIngredients.getOrDefault(hoverIt.getItem(), 0)))).setStyle(Style.EMPTY.withColor(0XFF0000)));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        if(isClicked){
            if(clickCount < 2){
                if(lastClickTime + 500 < System.currentTimeMillis()){
                    clickCount++;
                    lastClickTime = System.currentTimeMillis();
                }
            }else{
                double itnl = Math.max(Math.exp(-(clickCount) /20.0) * 200, 30);
                if(lastClickTime + itnl < System.currentTimeMillis()){
                    clickCount++;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1F,0.03f));
                    NetworkHandler.CHANNEL.sendToServer(new MagicCraftPacket(parent.selectedRecipe.getId(), BuiltInRegistries.RECIPE_TYPE.getKey(parent.selectedAdapter.getRecipe())));
                    parent.refreshItems();
                    lastClickTime = System.currentTimeMillis();
                }
            }


        }
        guiGraphics.pose().pushPose();

        int offsetX = this.cachedItems.size() * internal + 30 + left;
        int offsetY = top;

        Recipe recipe = parent.selectedRecipe;

        if(recipe!=null){
            ItemStack output = parent.selectedItem;
            renderItemStack(guiGraphics, output, offsetX, offsetY, false);
            guiGraphics.setColor(1,1f,1f,1);
            guiGraphics.blit(Awesome_storage.space("textures/gui/arrow.png"),offsetX - 25,offsetY+2 ,0,0,22,15,22,15);
            isHoveredResult = mouseX >= offsetX && mouseX <= offsetX + 16 && mouseY >= offsetY && mouseY <= offsetY + 16;
            if(isHoveredResult && !output.isEmpty()){
                renderSlotHighlight(guiGraphics, offsetX, offsetY, internal);
                List<Component> tooltip = output.getTooltipLines( Minecraft.getInstance().player, TooltipFlag.NORMAL);
                if(!Util.canCraftSimple(new HashMap<>(parent.haveIngredients),parent.selectedAdapter.getIngredients(recipe)))
                    tooltip.add(Component.translatable("magic_storage.missing_ingredient").withStyle(Style.EMPTY.withColor(0XFF0000)));
                else
                    tooltip.add(Component.translatable("magic_storage.can_craft").withStyle(Style.EMPTY.withColor(0X00FF00)));
                guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltip,
                        output.getTooltipImage(),mouseX, mouseY);
            }

        }

    }



}