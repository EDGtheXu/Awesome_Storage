package com.github.edg_thexu.awesome_storage.core.network.c2s;

import com.github.edg_thexu.awesome_storage.utils.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.space;

public record AutoStockPacket(List<ItemStack> targets) implements CustomPacketPayload {

    public static final Type<AutoStockPacket> TYPE = new Type<>(space("auto_stock_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AutoStockPacket> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, AutoStockPacket::targets,
            AutoStockPacket::new
    );

    @Override
    public @NotNull Type<AutoStockPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var entity = Util.getStorageEntity(context.player());
            if (entity == null) return;

            var inv = context.player().getInventory();
            for (ItemStack target : targets) {
                if (target.isEmpty()) continue;
                int need = target.getCount();
                // Count existing in inventory
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, target)) {
                        need -= s.getCount();
                        if (need <= 0) break;
                    }
                }
                if (need <= 0) continue;
                // Take from storage
                ItemStack taken = entity.takeItem(target, need);
                if (!taken.isEmpty()) {
                    context.player().getInventory().placeItemBackInInventory(taken);
                }
            }
            entity.syncToClient(context.player());
        });
    }
}
