package com.github.edg_thexu.awesome_storage.core.event;

import com.github.edg_thexu.awesome_storage.AwesomeStorage;
import com.github.edg_thexu.awesome_storage.api.adapter.CommonRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.adapter.SmithingRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.event.RegisterAdapterEvent;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicCraftPacket;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicStoragePacket;
import com.github.edg_thexu.awesome_storage.core.network.s2c.BlockPosSyncPacket;
import com.github.edg_thexu.awesome_storage.core.network.s2c.ChunkPacket;
import com.github.edg_thexu.awesome_storage.core.network.s2c.ConfigSyncPacket;
import com.github.edg_thexu.awesome_storage.core.network.s2c.StorageItemsSyncPacket;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;


@EventBusSubscriber(modid = AwesomeStorage.MODID)
public class ModEvent {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(MagicStoragePacket.TYPE, MagicStoragePacket.STREAM_CODEC, MagicStoragePacket::handle);
        registrar.playToServer(MagicCraftPacket.TYPE, MagicCraftPacket.STREAM_CODEC, MagicCraftPacket::handle);
        registrar.playToClient(ConfigSyncPacket.TYPE, ConfigSyncPacket.STREAM_CODEC, ConfigSyncPacket::handle);
        registrar.playToServer(BlockPosSyncPacket.TYPE, BlockPosSyncPacket.STREAM_CODEC, BlockPosSyncPacket::handle);

        registrar.playToClient(ChunkPacket.TYPE, ChunkPacket.STREAM_CODEC, ChunkPacket::handle);
        registrar.playToClient(StorageItemsSyncPacket.TYPE, StorageItemsSyncPacket.STREAM_CODEC, StorageItemsSyncPacket::handle);
 
    }

    @SubscribeEvent
    public static void onRegisterAdapter(RegisterAdapterEvent event) {
        event.register(RecipeType.CRAFTING, new CommonRecipeAdapter<>((RecipeType.CRAFTING)));
        event.register(RecipeType.SMITHING, new SmithingRecipeAdapter<>(RecipeType.SMITHING));
    }
}
