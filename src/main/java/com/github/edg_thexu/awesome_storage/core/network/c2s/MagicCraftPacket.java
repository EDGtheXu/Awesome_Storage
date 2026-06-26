package com.github.edg_thexu.awesome_storage.core.network.c2s;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.utils.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.space;

public record MagicCraftPacket(ResourceLocation id, ResourceLocation adapterID, List<ItemStack> excluded) implements CustomPacketPayload {

    public static final Type<MagicCraftPacket> TYPE = new Type<>(space("magic_craft_packet_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagicCraftPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MagicCraftPacket::id,
            ResourceLocation.STREAM_CODEC, MagicCraftPacket::adapterID,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, MagicCraftPacket::excluded,
            MagicCraftPacket::new
    ).cast();

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var recipeOpt = context.player().level().getRecipeManager().byKey(id);
            if (recipeOpt.isEmpty()) return;
            var recipe = recipeOpt.get();
            var adapter = AdapterManager.Adapters.get(BuiltInRegistries.RECIPE_TYPE.get(adapterID));
            if (adapter == null) return;

            NonNullList<Ingredient> ingredients = adapter.getIngredients((RecipeHolder<Recipe<RecipeInput>>) recipe);
            MagicStorageBlockEntity entity = Util.getStorageEntity(context.player());
            if (entity == null) return;

            // Excluded items sent from client (matched by isSameItemSameComponents on server)
            Set<ItemStack> excludedItems = new HashSet<>(excluded);

            // Check and consume ingredients from adjacent containers
            List<ItemStack> consumed = entity.craftAndConsume(ingredients, excludedItems);
            if (consumed == null) return;

            context.player().awardRecipes(Collections.singleton(recipe));
            ItemStack result = adapter.getCraftResult((RecipeHolder<Recipe<RecipeInput>>) recipe, consumed, context.player().level().registryAccess()).copy();
            result.onCraftedBy(context.player().level(), context.player(), result.getCount());

            int remaining = entity.storeItem(result);
            if (remaining > 0) {
                result.setCount(remaining);
                context.player().getInventory().placeItemBackInInventory(result);
            }
            adapter.onCraftFinish((RecipeHolder) recipe, consumed, context.player(), context.player().level());
            entity.syncToClient(context.player());
        });
    }

    @Override
    public @NotNull Type<MagicCraftPacket> type() {
        return TYPE;
    }
}
