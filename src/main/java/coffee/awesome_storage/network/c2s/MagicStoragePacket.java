package coffee.awesome_storage.network.c2s;

import coffee.awesome_storage.Util.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static coffee.awesome_storage.Awesome_storage.space;

public record MagicStoragePacket(int id, ItemStack item) implements CustomPacketPayload {


    public static final Type<MagicStoragePacket> TYPE = new Type<>(space("magic_storage_packet_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MagicStoragePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MagicStoragePacket::id,
            ItemStack.STREAM_CODEC, MagicStoragePacket::item,
            MagicStoragePacket::new
    );


    @Override
    public @NotNull Type<MagicStoragePacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {

            var entity = Util.getStorageEntity(context.player());
            if(id < 10000) {//约定10000以下为存储
                NonNullList<ItemStack> items = entity.getItems();

                ItemStack item = this.item;
                // 合并堆叠
                for (int i = 0; i < items.size(); i++) {
                    ItemStack current = items.get(i);
                    if (current.isEmpty()) {
                        continue; // 跳过空槽
                    }
                    for (int j = i + 1; j < items.size(); j++) {
                        ItemStack next = items.get(j);
                        if (!next.isEmpty() && current.is(next.getItem()) && current.getCount() < current.getMaxStackSize()) {
                            // 合并堆叠
                            int transfer = Math.min(next.getCount(), current.getMaxStackSize() - current.getCount());
                            current.grow(transfer); // 增加当前物品的数量
                            next.shrink(transfer);  // 减少下一个物品的数量
                            if (next.isEmpty()) {
                                items.set(j, ItemStack.EMPTY); // 如果下一个物品被完全合并，设置为空
                            }
                        }
                    }
                }

                // 尝试将新物品堆叠到已有的相同物品槽中
                for (ItemStack current : items) {
                    if (!current.isEmpty() && current.is(item.getItem()) && current.getCount() < current.getMaxStackSize()) {
                        // 计算可以堆叠的数量
                        int transfer = Math.min(item.getCount(), current.getMaxStackSize() - current.getCount());
                        current.grow(transfer); // 增加当前物品的数量
                        item.shrink(transfer);  // 减少新物品的数量
                        if (item.isEmpty()) {
                            break; // 如果新物品被完全堆叠，退出循环
                        }
                    }
                }

                // 如果新物品未被完全堆叠，将其放入第一个空槽
                if (!item.isEmpty()) {
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).isEmpty()) {
                            items.set(i, item);
                            item = ItemStack.EMPTY; // 清空手持物品
                            break;
                        }
                    }
                }

                // 排序
                items.sort((item1, item2) -> {
                    // 如果 item1 为空且 item2 不为空，item1 排在后面
                    if (item1.isEmpty() && !item2.isEmpty()) {
                        return 1;
                    }
                    // 如果 item2 为空且 item1 不为空，item2 排在后面
                    if (item2.isEmpty() && !item1.isEmpty()) {
                        return -1;
                    }
                    // 如果两者都为空或都不为空，按名称排序
                    return item1.getDisplayName().getString().compareTo(item2.getDisplayName().getString());
                });

                // 如果手持物品未被完全堆叠或放入空槽，保留剩余数量
                if (!item.isEmpty()) {
                    context.player().containerMenu.setCarried(item);
                } else {
                    context.player().containerMenu.setCarried(ItemStack.EMPTY);
                }
                if(!context.player().containerMenu.getCarried().isEmpty()){
                    context.player().getInventory().add(context.player().containerMenu.getCarried());
                }
                entity.setItems(items);
                var level = context.player().level();
                level.sendBlockUpdated(entity.getBlockPos(), level.getBlockState(entity.getBlockPos()), level.getBlockState(entity.getBlockPos()), 3);

            }
            else{//10000以上为取出所有
                int index  = id - 10000;
                ItemStack fetch = entity.getItem(index);
                context.player().getInventory().add(fetch);
                List<ItemStack> items = entity.getItems();
                // 排序
                items.sort((item1, item2) -> {
                    // 如果 item1 为空且 item2 不为空，item1 排在后面
                    if (item1.isEmpty() && !item2.isEmpty()) {
                        return 1;
                    }
                    // 如果 item2 为空且 item1 不为空，item2 排在后面
                    if (item2.isEmpty() && !item1.isEmpty()) {
                        return -1;
                    }
                    // 如果两者都为空或都不为空，按名称排序
                    return item1.getDisplayName().getString().compareTo(item2.getDisplayName().getString());
                });


                var level = context.player().level();
                level.sendBlockUpdated(entity.getBlockPos(), level.getBlockState(entity.getBlockPos()), level.getBlockState(entity.getBlockPos()), 3);

            }

        }).exceptionally(e -> null);
    }
}
