package coffee.awesome_storage.client.screen.widget;

import coffee.awesome_storage.block.MagicStorageBlockEntity;
import coffee.awesome_storage.client.screen.MagicStorageScreen;
import coffee.awesome_storage.config.StorageConfig;
import coffee.awesome_storage.network.NetworkHandler;
import coffee.awesome_storage.network.c2s.MagicStoragePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

import static coffee.awesome_storage.utils.Util.*;

@OnlyIn(Dist.CLIENT)
public class MagicStorageWidget extends AbstractFloatWidget {
    MagicStorageBlockEntity storageEntity;

    public MagicStorageWidget(MagicStorageScreen screen, int x, int y, int width, int height, Component message) {
        super(screen, x, y, width, height, message);
        this.storageEntity = getStorageEntity(Minecraft.getInstance().player);
    }

    @Override
    protected List<ItemStack> getItems() {
        return getStorageItems(Minecraft.getInstance().player);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);

        var size = storageEntity.getContainerSize();

        String info = getNonEmptyItemsCount() +"/"+size+"  "+"lvl:"+storageEntity.lvl;
        guiGraphics.drawString(Minecraft.getInstance().font,info ,this.getX()+this.width + 5,this.getY()-10,0xffffff);

        String upgradeInfo;
        if(StorageConfig.getUpgradeLine().containsKey(storageEntity.lvl+1)){
            var upgrade = StorageConfig.getUpgradeLine().get(storageEntity.lvl+1);
            upgradeInfo = "Next: +"+ upgrade.extend() + " <--";
            ItemStack need = StorageConfig.getUpgradeLine().get(storageEntity.lvl+1).material();
            int x = this.getX() + this.width + 5 + 70;
            int y = this.getY() - 1;
            renderItemStack(guiGraphics,need,x,y,false);
            if(mouseX > x && mouseY > y  && mouseX < x + 16 && mouseY < y + 16){
                guiGraphics.renderTooltip(Minecraft.getInstance().font, need,mouseX, mouseY);
            }
        }else{
            upgradeInfo = "Max lvl";
        }
        guiGraphics.drawString(Minecraft.getInstance().font, upgradeInfo ,this.getX()+this.width + 5,this.getY() + 4,0xffffff);

    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {

        // 取
        if(screen.getMenu().getCarried().isEmpty()) {
            if (hoverIt != null) {
                NetworkHandler.CHANNEL.sendToServer(new MagicStoragePacket(hoverIndex + 10000, new ItemStack(Items.WOODEN_AXE)));
                return true;
            }
        }
        // 存
        else{
            if(this.isHovered){

                NetworkHandler.CHANNEL.sendToServer(new MagicStoragePacket(0, screen.getMenu().getCarried()));
                return true;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);

    }
}
