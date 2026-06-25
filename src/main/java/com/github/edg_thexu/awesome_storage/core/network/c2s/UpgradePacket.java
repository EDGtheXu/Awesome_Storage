package com.github.edg_thexu.awesome_storage.core.network.c2s;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.item.QueueUpgradeItem;
import com.github.edg_thexu.awesome_storage.core.item.WirelessNetworkCard;
import com.github.edg_thexu.awesome_storage.utils.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.space;

public record UpgradePacket(int action, ItemStack upgradeSlot, int extra) implements CustomPacketPayload {

    public static final int ACTION_PLACE = 0;
    public static final int ACTION_TAKE = 1;
    public static final int ACTION_SET_FREQ = 2;
    public static final int ACTION_QUEUE_PLACE = 3;
    public static final int ACTION_QUEUE_TAKE = 4;

    public static final Type<UpgradePacket> TYPE = new Type<>(space("upgrade_packet_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, UpgradePacket::action,
            ItemStack.OPTIONAL_STREAM_CODEC, UpgradePacket::upgradeSlot,
            ByteBufCodecs.INT, UpgradePacket::extra,
            UpgradePacket::new
    );

    public static UpgradePacket queuePlace(int slotIndex, ItemStack stack) {
        return new UpgradePacket(ACTION_QUEUE_PLACE, stack, slotIndex);
    }

    public static UpgradePacket queueTake(int slotIndex) {
        return new UpgradePacket(ACTION_QUEUE_TAKE, ItemStack.EMPTY, slotIndex);
    }

    @Override
    public @NotNull Type<UpgradePacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            MagicStorageBlockEntity be = Util.getStorageEntity(context.player());
            if (be == null) return;

            switch (action) {
                case ACTION_PLACE -> {
                    ItemStack carried = context.player().containerMenu.getCarried();
                    if (carried.isEmpty() || !(carried.getItem() instanceof WirelessNetworkCard)) return;
                    ItemStack toSlot = carried.copy();
                    toSlot.setCount(1);
                    be.setUpgradeSlot(toSlot);
                    carried.shrink(1);
                    if (carried.isEmpty()) context.player().containerMenu.setCarried(ItemStack.EMPTY);
                    context.player().containerMenu.broadcastChanges();
                }
                case ACTION_TAKE -> {
                    ItemStack slotItem = be.getUpgradeSlot();
                    if (slotItem.isEmpty()) return;
                    be.setUpgradeSlot(ItemStack.EMPTY);
                    context.player().getInventory().placeItemBackInInventory(slotItem.copy());
                }
                case ACTION_SET_FREQ -> {
                    be.setFrequency(extra);
                }
                case ACTION_QUEUE_PLACE -> {
                    int slotIndex = extra;
                    ItemStack carried = context.player().containerMenu.getCarried();
                    if (carried.isEmpty() || !(carried.getItem() instanceof QueueUpgradeItem)) return;
                    ItemStack toSlot = carried.copy();
                    toSlot.setCount(1);
                    be.setQueueUpgradeSlot(slotIndex, toSlot);
                    carried.shrink(1);
                    if (carried.isEmpty()) context.player().containerMenu.setCarried(ItemStack.EMPTY);
                    context.player().containerMenu.broadcastChanges();
                }
                case ACTION_QUEUE_TAKE -> {
                    int slotIndex = extra;
                    ItemStack slotItem = be.getQueueUpgradeSlots()[slotIndex];
                    if (slotItem.isEmpty()) return;
                    be.setQueueUpgradeSlot(slotIndex, ItemStack.EMPTY);
                    context.player().getInventory().placeItemBackInInventory(slotItem.copy());
                }
            }
            be.setChanged();
            be.syncToClient(context.player());
        });
    }
}
