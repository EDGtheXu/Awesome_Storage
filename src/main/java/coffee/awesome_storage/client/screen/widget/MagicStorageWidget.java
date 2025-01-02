package coffee.awesome_storage.client.screen.widget;

import coffee.awesome_storage.client.screen.MagicStorageScreen;
import coffee.awesome_storage.network.c2s.MagicStoragePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

import static coffee.awesome_storage.client.Util.getStorageEntity;
import static coffee.awesome_storage.client.Util.getStorageItems;

public class MagicStorageWidget extends AbstractFloatWidget {
    public MagicStorageWidget(MagicStorageScreen screen, int x, int y, int width, int height, Component message) {
        super(screen, x, y, width, height, message);
    }

    @Override
    protected List<ItemStack> getItems() {
        return getStorageItems(Minecraft.getInstance().player);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);

        var size = getStorageEntity(Minecraft.getInstance().player).getContainerSize();
        String info = getNonEmptyItemsCount() +"/"+size;
        guiGraphics.drawString(Minecraft.getInstance().font,info ,this.getX()+this.width/2 - Minecraft.getInstance().font.width(info)/2+30,this.getY()-10,0xffffff);


    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        // 取
        if(screen.getMenu().getCarried().isEmpty()) {
            if (hoverIt != null) {
                PacketDistributor.sendToServer(new MagicStoragePacket(hoverIndex + 10000, new ItemStack(Items.WOODEN_AXE)));
                return true;
            }
        }
        // 存
        else{
            if(this.isHovered){
                PacketDistributor.sendToServer(new MagicStoragePacket(0, screen.getMenu().getCarried()));
                return true;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);

    }
}
