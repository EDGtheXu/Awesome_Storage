package coffee.awesome_storage.client.screen.widget;

import coffee.awesome_storage.block.MagicStorageBlockEntity;
import coffee.awesome_storage.config.CraftConfig;
import coffee.awesome_storage.network.c2s.MagicStoragePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

import static coffee.awesome_storage.Util.Util.getStorageEntity;

public class MagicCraftAccessWidget extends AbstractFloatWidget{
    List<ItemStack> itemStacks;
    MagicCraftWidget parent;

    public MagicCraftAccessWidget(MagicCraftWidget parent, int x, int y, int width, int height, Component message) {
        super(parent.screen, x, y, width, height, message);
        this.itemStacks = new ArrayList<>();
        this.parent = parent;
    }

    @Override
    protected List<ItemStack> getItems() {
        MagicStorageBlockEntity storage = getStorageEntity(Minecraft.getInstance().player);
        itemStacks.clear();
        for(String str : storage.getBlock_accessors()){
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(str));
            itemStacks.add(new ItemStack(block));
        }
        return itemStacks;
    }
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);

        if(isHovered&&!menu.getCarried().isEmpty()){
            if(menu.getCarried().getItem() instanceof BlockItem block &&
                    CraftConfig.ENABLED_RECIPES.containsKey(block.getBlock())){
                guiGraphics.drawString(Minecraft.getInstance().font, "Add Work Block", mouseX+10, mouseY+5, 0xFFFFFF);
            }
        }
        if(getItems().isEmpty()){
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("magic_craft.no_access"), left +2, top +2, 0xFFFFFF);
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int pButton) {
        if(!menu.getCarried().isEmpty()){// add work block
            if(menu.getCarried().getItem() instanceof BlockItem block &&
                    CraftConfig.ENABLED_RECIPES.containsKey(block.getBlock())){
                PacketDistributor.sendToServer(new MagicStoragePacket(1,menu.getCarried()));
                parent.reloadingTime = 0;
            }
            return true;
        }
        if(hoverIt != null){
            System.out.println(hoverIndex);
            PacketDistributor.sendToServer(new MagicStoragePacket(20000 + hoverIndex,hoverIt));
            parent.reloadingTime = 0;
        }
        return false;
    }
}
