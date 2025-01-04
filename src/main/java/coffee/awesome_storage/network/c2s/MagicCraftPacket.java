package coffee.awesome_storage.network.c2s;

import coffee.awesome_storage.block.MagicStorageBlockEntity;
import coffee.awesome_storage.api.adapter.AdapterManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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
import static coffee.awesome_storage.utils.Util.getStorageItems;

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
                List<ItemStack> have = new ArrayList<>(getStorageItems(context.player()));

                Map<Item, Integer> haveIngredients = new HashMap<>();
                for (ItemStack stack : have) {
                    Item item = stack.getItem();
                    int count = stack.getCount();
                    haveIngredients.put(item, haveIngredients.getOrDefault(item, 0) + count);
                }

                boolean canCraft = true;
                for (Ingredient ingredient : ingredients) {
                    boolean ingredientFound = false;
                    if(ingredient.isEmpty()) continue;
                    for (ItemStack ingredientStack : ingredient.getItems()) {
                        Item ingredientItem = ingredientStack.getItem();
                        int requiredCount = ingredientStack.getCount();

                        // 如果已有原料中有这个原料，并且数量足够
                        if (haveIngredients.containsKey(ingredientItem) && haveIngredients.get(ingredientItem) >= requiredCount) {
                            // 减少对应原料的数量
                            haveIngredients.put(ingredientItem, haveIngredients.get(ingredientItem) - requiredCount);
                            ingredientFound = true;
                            break; // 找到后可以退出内层循环
                        }
                    }
                    // 如果没有找到足够的原料，则无法合成
                    if (!ingredientFound) {
                        canCraft = false;
                        break; // 退出外层循环
                    }
                }

                if (canCraft) {
                    // 处理合成逻辑，例如发送合成成功的消息，或者执行合成结果
                    context.player().awardRecipes(Collections.singleton(recipe.get()));
                    ItemStack result = recipe.get().value().getResultItem(context.player().level().registryAccess()).copy();
                    result.onCraftedBy(context.player().level(), context.player(), result.getCount());
                    net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(context.player(), result, entity);
//                    context.player().getInventory().add(result);

                    // todo
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


//                    context.player().getInventory().placeItemBackInInventory(result);
//                    if(result.getCount() > 0){
//
//                        System.out.println("result count: " + result.getCount());
//                    }
                    // 移除原料
                    for (Ingredient ingredient : ingredients) {
                        if(ingredient.isEmpty()) continue;
                        for (ItemStack ingredientStack : ingredient.getItems()) {
                            Item ingredientItem = ingredientStack.getItem();
                            int requiredCount = ingredientStack.getCount();

                            for(int i = 0; i < have.size(); i++){
                                ItemStack stack = have.get(i);
                                if(stack.getItem() == ingredientItem){
                                    int count = stack.getCount();
                                    if(count >= requiredCount){
                                        stack.shrink(requiredCount);
                                        break;
                                    }else{
                                        requiredCount -= count;
                                        have.remove(i);
                                    }
                                }
                            }
                        }
                    }
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
