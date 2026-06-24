package com.github.edg_thexu.awesome_storage.core.network.c2s;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.space;

public record RenameBlockPacket(Component name, BlockPos pos) implements CustomPacketPayload {

    public static final Type<RenameBlockPacket> TYPE = new Type<>(space("rename_block_packet_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameBlockPacket> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, RenameBlockPacket::name,
            BlockPos.STREAM_CODEC, RenameBlockPacket::pos,
            RenameBlockPacket::new
    ).cast();

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Component name = this.name;
            var be = context.player().level().getBlockEntity(pos);

            if(be instanceof MagicStorageBlockEntity mbe) {
                mbe.displayName = name;
                context.player().level().sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
            }
        });
    }

    @Override
    public @NotNull Type<RenameBlockPacket> type() {
        return TYPE;
    }
}
