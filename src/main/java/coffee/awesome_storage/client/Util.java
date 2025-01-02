package coffee.awesome_storage.client;

import coffee.awesome_storage.MixinUtil.IPlayer;
import coffee.awesome_storage.block.MagicStorageBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
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
