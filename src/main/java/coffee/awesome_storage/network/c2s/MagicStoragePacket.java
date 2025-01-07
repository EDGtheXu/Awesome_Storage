package coffee.awesome_storage.network.c2s;

import coffee.awesome_storage.config.CraftConfig;
import coffee.awesome_storage.utils.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

import static coffee.awesome_storage.utils.Util.tryAddItemStackToItemStacks;
import static coffee.awesome_storage.utils.Util.unionItemStacks;

public record MagicStoragePacket(int id, ItemStack item){

    public static void encode(MagicStoragePacket packet, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeInt(packet.id());
        friendlyByteBuf.writeItemStack(packet.item(),false);
    }

    public static MagicStoragePacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new MagicStoragePacket(friendlyByteBuf.readInt(), friendlyByteBuf.readItem());
    }

    public static void handle(MagicStoragePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        
        context.enqueueWork(() -> {
            Player player = context.getSender();
            int id = packet.id();
            ItemStack item = packet.item();
            var entity = Util.getStorageEntity(player);
            if(id < 10000) {//约定10000以下为存储
                if(id == 1){

                    if(item.getItem() instanceof BlockItem block &&
                            CraftConfig.isEnabledBlock(block.getBlock())
                    ){
                        String block_name = String.valueOf(BuiltInRegistries.BLOCK.getKey(block.getBlock()));
                        if(!entity.getBlock_accessors().contains(block_name)){
                            player.containerMenu.getCarried().shrink(1);
                            entity.getBlock_accessors().add(block_name);
                            player.level().sendBlockUpdated(entity.getBlockPos(), player.level().getBlockState(entity.getBlockPos()), player.level().getBlockState(entity.getBlockPos()), 3);
                        }
                    }
                    return;
                }

                NonNullList<ItemStack> items = entity.getItems();

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
                    player.containerMenu.setCarried(item);
                } else {
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                }
                if(!player.containerMenu.getCarried().isEmpty()){
                    player.getInventory().add(player.containerMenu.getCarried());
                }
                entity.setItems(items);
                var level = player.level();
                level.sendBlockUpdated(entity.getBlockPos(), level.getBlockState(entity.getBlockPos()), level.getBlockState(entity.getBlockPos()), 3);

            }
            else if(id < 20000){//10000 - 19999 为取出物品
                int index  = id - 10000;
                ItemStack fetch = entity.getItem(index);

                player.getInventory().placeItemBackInInventory(fetch.copy());
                entity.getItem(index).setCount(0);

                entity.sortItems();

                var level = player.level();
                level.sendBlockUpdated(entity.getBlockPos(), level.getBlockState(entity.getBlockPos()), level.getBlockState(entity.getBlockPos()), 3);

            } else if (id < 30000) {//20000 - 29999 为取出accessor

                List<String> accessors = entity.getBlock_accessors();
                if(accessors.size() > id - 20000) {
                    String block_name =accessors.remove(id - 20000);
                    Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(block_name));
                    player.level().sendBlockUpdated(entity.getBlockPos(), player.level().getBlockState(entity.getBlockPos()), player.level().getBlockState(entity.getBlockPos()), 3);
                    player.getInventory().add(new ItemStack(block.asItem()));
                }

                return;
            }

        });
    }
}
