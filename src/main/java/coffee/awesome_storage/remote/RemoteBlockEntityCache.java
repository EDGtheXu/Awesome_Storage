package coffee.awesome_storage.remote;

import coffee.awesome_storage.block.MagicStorageBlockEntity;
import coffee.awesome_storage.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData.BlockEntityInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;

public class RemoteBlockEntityCache {
    Map<BlockPos, MagicStorageBlockEntity> CACHE;
    public static ResourceKey<Level> clientLevelSource = Level.END;

    static RemoteBlockEntityCache instance = new RemoteBlockEntityCache();
    public static RemoteBlockEntityCache getInstance(){
        return instance;
    }

    RemoteBlockEntityCache() {
        CACHE = new java.util.HashMap<>();
    }

    public boolean contains(BlockPos pos) {
        return CACHE.containsKey(pos);
    }
    public MagicStorageBlockEntity get(BlockPos pos) {
        return CACHE.get(pos);
    }
    public void put(BlockPos pos, MagicStorageBlockEntity blockEntity) {
        CACHE.put(pos, blockEntity);
    }
    public MagicStorageBlockEntity load(BlockEntityInfo blockEntityInfo){
        BlockPos pos = new BlockPos(blockEntityInfo.packedXZ >> 4, blockEntityInfo.y, blockEntityInfo.packedXZ & 15);
        MagicStorageBlockEntity entity = new MagicStorageBlockEntity(pos, ModBlocks.MAGIC_STORAGE_BLOCK.get().defaultBlockState());
        entity.saveAdditional(blockEntityInfo.tag, Minecraft.getInstance().level.registryAccess());
        return entity;
    }

}
