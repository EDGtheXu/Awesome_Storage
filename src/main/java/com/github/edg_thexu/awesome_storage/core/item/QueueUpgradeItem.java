package com.github.edg_thexu.awesome_storage.core.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class QueueUpgradeItem extends Item {

    private final int extraSlots;

    public QueueUpgradeItem(Properties properties, int extraSlots) {
        super(properties);
        this.extraSlots = extraSlots;
    }

    public int getExtraSlots() {
        return extraSlots;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("magic_storage.tooltip.queue_upgrade", extraSlots));
    }
}
