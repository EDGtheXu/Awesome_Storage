package com.github.edg_thexu.awesome_storage.core.network.c2s;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.utils.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.space;

public record QueueActionPacket(int action, int slotIndex, int entryIndex, ResourceLocation recipeId, ResourceLocation adapterID, int quantity) implements CustomPacketPayload {

    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;
    public static final int ACTION_CLEAR_SLOT = 2;
    public static final int ACTION_CLEAR_ALL = 3;
    public static final int ACTION_TOGGLE_PAUSE = 4;

    private static final ResourceLocation EMPTY = ResourceLocation.withDefaultNamespace("_");

    public static final Type<QueueActionPacket> TYPE = new Type<>(space("queue_action_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QueueActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, QueueActionPacket::action,
            ByteBufCodecs.INT, QueueActionPacket::slotIndex,
            ByteBufCodecs.INT, QueueActionPacket::entryIndex,
            ResourceLocation.STREAM_CODEC, QueueActionPacket::recipeId,
            ResourceLocation.STREAM_CODEC, QueueActionPacket::adapterID,
            ByteBufCodecs.INT, QueueActionPacket::quantity,
            QueueActionPacket::new
    );

    public static QueueActionPacket add(ResourceLocation recipeId, ResourceLocation adapterID, int quantity) {
        return new QueueActionPacket(ACTION_ADD, 0, 0, recipeId, adapterID, quantity);
    }

    public static QueueActionPacket remove(int slotIndex, int entryIndex) {
        return new QueueActionPacket(ACTION_REMOVE, slotIndex, entryIndex, EMPTY, EMPTY, 0);
    }

    public static QueueActionPacket clearSlot(int slotIndex) {
        return new QueueActionPacket(ACTION_CLEAR_SLOT, slotIndex, 0, EMPTY, EMPTY, 0);
    }

    public static QueueActionPacket clearAll() {
        return new QueueActionPacket(ACTION_CLEAR_ALL, 0, 0, EMPTY, EMPTY, 0);
    }

    public static QueueActionPacket togglePause(int slotIndex) {
        return new QueueActionPacket(ACTION_TOGGLE_PAUSE, slotIndex, 0, EMPTY, EMPTY, 0);
    }

    @Override
    public @NotNull Type<QueueActionPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            MagicStorageBlockEntity be = Util.getStorageEntity(context.player());
            if (be == null || be.getQueueManager() == null) return;

            switch (action) {
                case ACTION_ADD -> {
                    if (!hasIngredients(context, be)) return;
                    int cookTime = getCookTime(context);
                    be.getQueueManager().addToQueue(recipeId, adapterID, quantity, cookTime);
                    be.getQueueManager().syncToPlayer((ServerPlayer) context.player());
                }
                case ACTION_REMOVE -> {
                    be.getQueueManager().removeFromPending(entryIndex);
                    be.getQueueManager().syncToPlayer((ServerPlayer) context.player());
                }
                case ACTION_CLEAR_SLOT -> {
                    be.getQueueManager().clearSlot(slotIndex);
                    be.getQueueManager().syncToPlayer((ServerPlayer) context.player());
                }
                case ACTION_CLEAR_ALL -> {
                    be.getQueueManager().clearAll();
                    be.getQueueManager().syncToPlayer((ServerPlayer) context.player());
                }
                case ACTION_TOGGLE_PAUSE -> {
                    var slot = be.getQueueManager().getSlots().get(slotIndex);
                    be.getQueueManager().setPaused(slotIndex, !slot.paused);
                    be.getQueueManager().syncToPlayer((ServerPlayer) context.player());
                }
            }
        });
    }

    private boolean hasIngredients(IPayloadContext context, MagicStorageBlockEntity be) {
        var optRecipe = context.player().level().getRecipeManager().byKey(recipeId);
        if (optRecipe.isEmpty()) return false;
        var recipeType = BuiltInRegistries.RECIPE_TYPE.get(adapterID);
        if (recipeType == null) return false;
        AbstractMagicCraftRecipeAdapter<?, ?> adapter = AdapterManager.Adapters.get(recipeType);
        if (adapter == null) return false;
        @SuppressWarnings("unchecked")
        var ingredients = adapter.getIngredients((RecipeHolder) optRecipe.get());
        return be.getItemOps().hasIngredients(ingredients);
    }

    private int getCookTime(IPayloadContext context) {
        var optRecipe = context.player().level().getRecipeManager().byKey(recipeId);
        if (optRecipe.isEmpty()) return 200;
        var recipeType = BuiltInRegistries.RECIPE_TYPE.get(adapterID);
        if (recipeType == null) return 200;
        AbstractMagicCraftRecipeAdapter<?, ?> adapter = AdapterManager.Adapters.get(recipeType);
        if (adapter == null) return 200;
        @SuppressWarnings("unchecked")
        int time = adapter.getCookTime((RecipeHolder) optRecipe.get());
        return time > 0 ? time : 200;
    }
}
