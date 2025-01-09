package coffee.awesome_storage.network.c2s;

import coffee.awesome_storage.utils.Util;
import coffee.awesome_storage.config.CraftConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static coffee.awesome_storage.Awesome_storage.space;
import static coffee.awesome_storage.utils.Util.*;

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
            Level level = getTargetLevel(context.player());
            var entity = Util.getStorageEntity(context.player());
            if(entity==null) return;
            if(id < 10000) {//约定10000以下为存储
                if(id == 1){

                    if(item.getItem() instanceof BlockItem block &&
                            CraftConfig.isEnabledBlock(block.getBlock())
                    ){
                        String block_name = String.valueOf(BuiltInRegistries.BLOCK.getKey(block.getBlock()));
                        if(!entity.getBlock_accessors().contains(block_name)){
                            context.player().containerMenu.getCarried().shrink(1);
                            entity.getBlock_accessors().add(block_name);

                            level.sendBlockUpdated(entity.getBlockPos(), level.getBlockState(entity.getBlockPos()), level.getBlockState(entity.getBlockPos()), 3);
                        }
                    }
                    return;
                }

                NonNullList<ItemStack> items = entity.getItems();
                ItemStack item = this.item;

                // step 1: 合并相同物品槽中的物品
                unionItemStacks(items);

                // step 2: 尝试将新物品放入空槽
                tryAddItemStackToItemStacks(item, items);



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
                level.sendBlockUpdated(entity.getBlockPos(), level.getBlockState(entity.getBlockPos()), level.getBlockState(entity.getBlockPos()), 3);

            }
            else if(id < 20000){//10000 - 19999 为取出物品
                int index  = id - 10000;
                ItemStack fetch = entity.getItem(index);

                context.player().getInventory().placeItemBackInInventory(fetch.copy());
                entity.getItem(index).setCount(0);

                entity.sortItems();

                level.sendBlockUpdated(entity.getBlockPos(), level.getBlockState(entity.getBlockPos()), level.getBlockState(entity.getBlockPos()), 3);

            } else if (id < 30000) {//20000 - 29999 为取出accessor

                List<String> accessors = entity.getBlock_accessors();
                if(accessors.size() > id - 20000) {
                    String block_name =accessors.remove(id - 20000);
                    Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block_name));
                    level.sendBlockUpdated(entity.getBlockPos(), level.getBlockState(entity.getBlockPos()), level.getBlockState(entity.getBlockPos()), 3);
                    context.player().getInventory().add(new ItemStack(block.asItem()));
                }

                return;
            }

        });
    }
}
