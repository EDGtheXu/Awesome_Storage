package coffee.awesome_storage.network.c2s;

import coffee.awesome_storage.block.MagicStorageBlockEntity;
import coffee.awesome_storage.api.adapter.AdapterManager;
import coffee.awesome_storage.utils.Util;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static coffee.awesome_storage.Awesome_storage.space;
import static coffee.awesome_storage.utils.Util.getStorageEntity;

public record MagicCraftPacket(ResourceLocation id, ResourceLocation adapterID)  implements CustomPacketPayload {


    public static final Type<MagicCraftPacket> TYPE = new Type<>(space("magic_craft_packet_s2c"));
    public static final StreamCodec<ByteBuf, MagicCraftPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,MagicCraftPacket::id,
            ResourceLocation.STREAM_CODEC,MagicCraftPacket::adapterID,
            MagicCraftPacket::new
    ).cast();

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var recipe = context.player().level().getRecipeManager().byKey(id);
            if(recipe.isPresent()){
                var adapter = AdapterManager.Adapters.get(BuiltInRegistries.RECIPE_TYPE.get(adapterID));
                NonNullList<Ingredient> ingredients = adapter.getIngredients((RecipeHolder<Recipe<RecipeInput>>) recipe.get());
                MagicStorageBlockEntity entity =  getStorageEntity(context.player());

                // step 1: 获取拥有的物品
                List<ItemStack> have = new ArrayList<>(Util.getStorageItems(context.player()));

                // step 2: 检查是否可以合成
                boolean canCraft = Util.canCraft(have, ingredients);

                if (canCraft) {

                    // step 3: 获取合成的物品
                    context.player().awardRecipes(Collections.singleton(recipe.get()));
                    ItemStack result = recipe.get().value().getResultItem(context.player().level().registryAccess()).copy();
                    result.onCraftedBy(context.player().level(), context.player(), result.getCount());
                    net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(context.player(), result, entity);

                     //step 4: 合成的物品放入存储空间
                    Util.tryAddItemStackToItemStacks(result, entity.getItems());
                    boolean c = false;
                    for(ItemStack stack : entity.getItems()){
                        if(stack.isEmpty()){
                            c=true;
                            PacketDistributor.sendToServer(new MagicStoragePacket(0,result));
                            break;
                        }
                    }
                    if(!c){
                        context.player().getInventory().placeItemBackInInventory(result);
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

    @Override
    public @NotNull Type<MagicCraftPacket> type() {
        return TYPE;
    }

}
