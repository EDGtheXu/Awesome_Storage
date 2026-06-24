package com.github.edg_thexu.awesome_storage.core.network.c2s;

import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.space;

public record MagicStoragePacket(int id, ItemStack item, long extra) implements CustomPacketPayload {
    public MagicStoragePacket(int id, ItemStack item) {
        this(id, item, 0L);
    }

    public static final Type<MagicStoragePacket> TYPE = new Type<>(space("magic_storage_packet_c2s"));
    private static final StreamCodec<ByteBuf, Long> LONG_STREAM_CODEC = new StreamCodec<>() {
        public Long decode(ByteBuf buf) { return buf.readLong(); }
        public void encode(ByteBuf buf, Long v) { buf.writeLong(v); }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, MagicStoragePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MagicStoragePacket::id,
            ItemStack.OPTIONAL_STREAM_CODEC, MagicStoragePacket::item,
            LONG_STREAM_CODEC, MagicStoragePacket::extra,
            MagicStoragePacket::new
    );

    @Override
    public @NotNull Type<MagicStoragePacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var entity = Util.getStorageEntity(context.player());
            if (entity == null) return;

            // add block accessor
            if (id == 1) {
                if (item.getItem() instanceof BlockItem block && CraftConfig.isEnabledBlock(block.getBlock())) {
                    String blockName = BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
                    entity.addBlockAccessor(blockName);
                    ItemStack carried = context.player().containerMenu.getCarried();
                    if (!carried.isEmpty()) {
                        carried.shrink(1);
                        if (carried.isEmpty()) {
                            context.player().containerMenu.setCarried(ItemStack.EMPTY);
                        }
                    }
                    context.player().containerMenu.broadcastChanges();
                }
                return;
            }

            // remove block accessor
            if (id >= 20000 && id < 30000) {
                int index = id - 20000;
                var accessors = entity.getBlock_accessors();
                if (index >= 0 && index < accessors.size()) {
                    String blockName = accessors.remove(index);
                    Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockName));
                    context.player().getInventory().add(new ItemStack(block.asItem()));
                    entity.getLevel().sendBlockUpdated(entity.getBlockPos(), entity.getBlockState(),
                            entity.getBlockState(), 3);
                    entity.setChanged();
                }
                return;
            }

            // store item
            if (id == 0) {
                if (context.player().containerMenu.getCarried().isEmpty()) return;
                ItemStack toStore = context.player().containerMenu.getCarried().copy();
                int remaining = entity.storeItem(toStore);
                if (remaining <= 0) {
                    context.player().containerMenu.setCarried(ItemStack.EMPTY);
                } else {
                    toStore.setCount(remaining);
                    context.player().containerMenu.setCarried(toStore);
                }
                entity.syncToClient(context.player());
                return;
            }

            // take item
            if (id >= 10000 && id < 20000) {
                ItemStack taken;
                if (!item.isEmpty()) {
                    taken = entity.takeItem(item);
                } else {
                    int index = id - 10000;
                    taken = entity.takeItem(index);
                }
                if (!taken.isEmpty()) {
                    context.player().getInventory().placeItemBackInInventory(taken.copy());
                }
                entity.syncToClient(context.player());
                return;
            }

            // deposit all
            if (id == 2) {
                entity.depositAll(context.player(), extra);
                entity.syncToClient(context.player());
                return;
            }

            // quick stack
            if (id == 3) {
                entity.quickStack(context.player(), extra);
                entity.syncToClient(context.player());
                return;
            }

            // refill
            if (id == 4) {
                entity.refill(context.player());
                entity.syncToClient(context.player());
                return;
            }

            // favorite slots
            if (id == 5) {
                context.player().getPersistentData().putLong("FavoriteSlots", extra);
                return;
            }

            // only sync
            if (id == 6) {
                entity.syncToClient(context.player());
                return;
            }

        });
    }
}
