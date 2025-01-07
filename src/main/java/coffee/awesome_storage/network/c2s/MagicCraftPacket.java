package coffee.awesome_storage.network.c2s;

import coffee.awesome_storage.api.adapter.AdapterManager;
import coffee.awesome_storage.block.MagicStorageBlockEntity;
import coffee.awesome_storage.network.NetworkHandler;
import coffee.awesome_storage.utils.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static coffee.awesome_storage.utils.Util.getStorageEntity;
import static net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent;

public record MagicCraftPacket(ResourceLocation id, ResourceLocation adapterID){
    
    public static void encode(MagicCraftPacket packet, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeResourceLocation(packet.id());
        friendlyByteBuf.writeResourceLocation(packet.adapterID());
    }

    public static MagicCraftPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new MagicCraftPacket(friendlyByteBuf.readResourceLocation(), friendlyByteBuf.readResourceLocation());
    }
    
    public static void handle(MagicCraftPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ResourceLocation id = packet.id();
            ResourceLocation adapterID = packet.adapterID();
            Player player = context.getSender();
            var recipe = player.level().getRecipeManager().byKey(id);
            if(recipe.isPresent()){
                var adapter = AdapterManager.Adapters.get(BuiltInRegistries.RECIPE_TYPE.get(adapterID));
                NonNullList<Ingredient> ingredients = adapter.getIngredients((Recipe<Container>) recipe.get());
                MagicStorageBlockEntity entity =  getStorageEntity(player);

                // step 1: 获取拥有的物品
                List<ItemStack> have = new ArrayList<>(Util.getStorageItems(player));

                // step 2: 检查是否可以合成
                boolean canCraft = Util.canCraft(have, ingredients);

                if (canCraft) {

                    // step 3: 获取合成的物品
                    player.awardRecipes(Collections.singleton(recipe.get()));
                    ItemStack result = recipe.get().getResultItem(player.level().registryAccess()).copy();
                    result.onCraftedBy(player.level(), player, result.getCount());
                    firePlayerCraftingEvent(player, result, entity);

                     //step 4: 合成的物品放入存储空间
                    Util.tryAddItemStackToItemStacks(result, entity.getItems());
                    if(!result.isEmpty()){
                        //step 5: 现有堆叠不够，放入空物品栏
                        boolean c = false;
                        for(ItemStack stack : entity.getItems()){
                            if(stack.isEmpty()){
                                c=true;
                                NetworkHandler.CHANNEL.sendToServer(new MagicStoragePacket(0,result.copy()));
                                break;
                            }
                        }
                        if(!c){
                            player.getInventory().placeItemBackInInventory(result);
                        }
                    }

                    // step 5: 更新拥有的物品
                    Util.doCraft(have, ingredients);

                    // step 6: 更新存储空间
                    NonNullList<ItemStack> items = NonNullList.withSize(entity.getItems().size(), ItemStack.EMPTY);
                    for (ItemStack stack : have) {
                        items.set(entity.getItems().indexOf(stack), stack);
                    }
                    entity.setItems(items);

                }
            }
        });
    }

}
