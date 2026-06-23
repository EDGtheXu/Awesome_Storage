package coffee.awesome_storage.network.s2c;

import coffee.awesome_storage.block.MagicStorageBlockEntity;
import coffee.awesome_storage.remote.RemoteBlockEntityCache;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static coffee.awesome_storage.Awesome_storage.space;

public record StorageItemsSyncPacket(BlockPos pos, List<ItemStack> items, int usedSlots, int totalSlots) implements CustomPacketPayload {
    public static final Type<StorageItemsSyncPacket> TYPE = new Type<>(space("storage_items_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageItemsSyncPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StorageItemsSyncPacket::pos,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, StorageItemsSyncPacket::items,
            ByteBufCodecs.INT, StorageItemsSyncPacket::usedSlots,
            ByteBufCodecs.INT, StorageItemsSyncPacket::totalSlots,
            StorageItemsSyncPacket::new
    );

    @Override
    public @NotNull Type<StorageItemsSyncPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Check level BE first (local access)
            var level = context.player().level();
            MagicStorageBlockEntity be = null;
            if (level.getBlockEntity(pos) instanceof MagicStorageBlockEntity e) {
                be = e;
            }
            // Check RemoteBlockEntityCache for remote/fake entities
            if (be == null) {
                be = RemoteBlockEntityCache.getInstance().get(pos);
            }
            if (be != null) {
                be.setCachedItems(items, usedSlots, totalSlots);
                if(RemoteBlockEntityCache.getInstance().get(pos) != null) {
                    RemoteBlockEntityCache.getInstance().get(pos).setCachedItems(items, usedSlots, totalSlots);
                }
            }
        });
    }
}
