package coffee.awesome_storage.network.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static coffee.awesome_storage.Awesome_storage.space;

public record BlockPosSyncPacket(BlockPos pos, ResourceKey<Level> levelResourceKey, int code) implements CustomPacketPayload {

    public static final Type<BlockPosSyncPacket> TYPE = new Type<>(space("block_pos_sync_packet_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPosSyncPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,  BlockPosSyncPacket::pos,
            ByteBufCodecs.fromCodec(Level.RESOURCE_KEY_CODEC),  BlockPosSyncPacket::levelResourceKey,
            ByteBufCodecs.INT,  BlockPosSyncPacket::code,
            BlockPosSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if(!context.player().level().isClientSide){
                Level real = context.player().getServer().getLevel(levelResourceKey);
                if(real == null) return;
                LevelChunk chunk = real.getChunkAt(pos);
                PacketDistributor.sendToPlayer((ServerPlayer) context.player(),new ChunkPacket(chunk, pos));
//                PacketDistributor.sendToPlayer((ServerPlayer) context.player(),new ChunkPacket(chunk, pos));
            }
        });
    }
}
