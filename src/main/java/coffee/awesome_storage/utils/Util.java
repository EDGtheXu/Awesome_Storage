package coffee.awesome_storage.utils;

import coffee.awesome_storage.mix_util.IPlayer;
import coffee.awesome_storage.block.MagicStorageBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;


public class Util {
    public static List<ItemStack> getStorageItems(Player player) {
        var entity = (MagicStorageBlockEntity)((IPlayer)player).awesomeStorage$getContainer();
       return entity.getItems();
    }
    public static MagicStorageBlockEntity getStorageEntity(Player player) {
        if(((IPlayer)player).awesomeStorage$getContainer() instanceof MagicStorageBlockEntity entity)
            return entity;
        return  null;
    }

    public static void renderItemStack(GuiGraphics guiGraphics, ItemStack it, int x, int y, boolean overLay){
        var minecraft = Minecraft.getInstance();
        guiGraphics.pose().pushPose();
        if(overLay) guiGraphics.setColor(1F,  0.5F,0.5F,1F);
        guiGraphics.renderItem(it,x,y);
        guiGraphics.setColor(1F,  1,1,1F);
        guiGraphics.renderItemDecorations(minecraft.font,it,x,y);

        guiGraphics.pose().popPose();
    }

}
