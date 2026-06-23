package com.github.edg_thexu.awesome_storage.core.network.c2s;

import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.awesome_storage.config.CraftConfig;
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

public record MagicStoragePacket(int id, ItemStack item) implements CustomPacketPayload {
    public static final Type<MagicStoragePacket> TYPE = new Type<>(space("magic_storage_packet_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MagicStoragePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MagicStoragePacket::id,
            ItemStack.OPTIONAL_STREAM_CODEC, MagicStoragePacket::item,
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
                entity.syncToClient(context.player());
                return;
            }

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

            if (id < 20000) {
                int index = id - 10000;
                ItemStack taken = entity.takeItem(index);
                if (!taken.isEmpty()) {
                    context.player().getInventory().placeItemBackInInventory(taken.copy());
                }
                entity.syncToClient(context.player());
                return;
            }

            if (id < 30000) {
                int index = id - 20000;
                var accessors = entity.getBlock_accessors();
                if (index >= 0 && index < accessors.size()) {
                    String blockName = accessors.remove(index);
                    Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockName));
                    context.player().getInventory().add(new ItemStack(block.asItem()));
                    entity.getLevel().sendBlockUpdated(entity.getBlockPos(), entity.getBlockState(),
                            entity.getBlockState(), 3);
                }
                entity.syncToClient(context.player());
            }
        });
    }
}
