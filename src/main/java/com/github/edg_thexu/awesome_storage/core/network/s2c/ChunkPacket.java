package com.github.edg_thexu.awesome_storage.core.network.s2c;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.menu.MagicStorageMenu;
import com.github.edg_thexu.awesome_storage.core.registry.ModBlocks;
import com.github.edg_thexu.awesome_storage.utils.RemoteBlockEntityCache;
import com.github.edg_thexu.awesome_storage.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.space;

public class ChunkPacket implements CustomPacketPayload  {
    private final int x;
    private final int z;
    private final ClientboundLevelChunkPacketData chunkData;
    LevelChunk chunk;
    BlockPos pos;
    public ChunkPacket(LevelChunk chunk, BlockPos pos) {
        ChunkPos chunkpos = chunk.getPos();
        this.x = chunkpos.x;
        this.z = chunkpos.z;
        this.chunk = chunk;
        this.chunkData = new ClientboundLevelChunkPacketData(chunk);
        this.pos = pos;
    }

    private ChunkPacket(RegistryFriendlyByteBuf buffer) {
        this.x = buffer.readInt();
        this.z = buffer.readInt();
        this.chunkData = new ClientboundLevelChunkPacketData(buffer, this.x, this.z);
        this.pos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.x);
        buffer.writeInt(this.z);
        this.chunkData.write(buffer);
        buffer.writeInt(this.pos.getX());
        buffer.writeInt(this.pos.getY());
        buffer.writeInt(this.pos.getZ());
    }

    public static final CustomPacketPayload.Type<ChunkPacket> TYPE = new CustomPacketPayload.Type<>(space("chunk_packet_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkPacket> STREAM_CODEC = StreamCodec.ofMember(
            ChunkPacket::write,
            ChunkPacket::new
    );

    public void handle(IPayloadContext handler) {
        chunk = Minecraft.getInstance().level.getChunk(x, z);
        this.chunkData.getBlockEntitiesTagsConsumer(x,z).accept((pos,type,tag)->{

            if(this.pos.equals(pos)) {
                Minecraft.getInstance().level.setBlock(pos, ModBlocks.CRAFTING_UNIT_BLOCK.get().defaultBlockState(), 2);
                if(type.getValidBlocks().contains(ModBlocks.CRAFTING_UNIT_BLOCK.get())){
                    BlockEntity blockEntity = type.create(pos, ModBlocks.CRAFTING_UNIT_BLOCK.get().defaultBlockState());
                    if(blockEntity instanceof MagicStorageBlockEntity magic) {
                        magic.loadWithComponents(tag, Minecraft.getInstance().level.registryAccess());
                        RemoteBlockEntityCache.getInstance().put(new BlockPos(pos.getX(), pos.getY(), pos.getZ()), magic);
                        Util.setStorageEntity(handler.player(), magic);
                        if(handler.player().containerMenu instanceof MagicStorageMenu menu){
                            menu.setDirty(true);
                        }
                    }
                }
            }

        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
