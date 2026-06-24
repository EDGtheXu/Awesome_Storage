package com.github.edg_thexu.awesome_storage.core.network.s2c;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.manager.CraftingQueueManager;
import com.github.edg_thexu.awesome_storage.utils.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.space;

public record QueueSyncPacket(CompoundTag queueData) implements CustomPacketPayload {

    public static final Type<QueueSyncPacket> TYPE = new Type<>(space("queue_sync_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QueueSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, QueueSyncPacket::queueData,
            QueueSyncPacket::new
    );

    @Override
    public @NotNull Type<QueueSyncPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            MagicStorageBlockEntity be = Util.getStorageEntity(context.player());
            if (be != null && be.getQueueManager() != null) {
                be.getQueueManager().loadSlots(queueData);
            }
        });
    }
}
