package com.github.edg_thexu.awesome_storage.core.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class WirelessNetworkCard extends Item {

    private final int range;

    public WirelessNetworkCard(Properties properties, int range) {
        super(properties);
        this.range = range;
    }

    public int getRange() {
        return range;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("awesome_storage.tooltip.network_range", range > 0 ? range + " blocks" : "Unlimited"));
    }
}
