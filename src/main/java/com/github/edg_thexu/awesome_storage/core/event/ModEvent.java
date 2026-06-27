package com.github.edg_thexu.awesome_storage.core.event;

import com.github.edg_thexu.awesome_storage.AwesomeStorage;
import com.github.edg_thexu.awesome_storage.api.adapter.CommonRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.event.RegisterAdapterEvent;
import com.github.edg_thexu.awesome_storage.core.network.c2s.AutoStockPacket;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicCraftPacket;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicStoragePacket;
import com.github.edg_thexu.awesome_storage.core.network.c2s.QueueActionPacket;
import com.github.edg_thexu.awesome_storage.core.network.c2s.RenameBlockPacket;
import com.github.edg_thexu.awesome_storage.core.network.c2s.UpgradePacket;
import com.github.edg_thexu.awesome_storage.core.network.s2c.BlockPosSyncPacket;
import com.github.edg_thexu.awesome_storage.core.network.s2c.ChunkPacket;
import com.github.edg_thexu.awesome_storage.core.network.s2c.ConfigSyncPacket;
import com.github.edg_thexu.awesome_storage.core.network.s2c.QueueSyncPacket;
import com.github.edg_thexu.awesome_storage.core.network.s2c.StorageItemsSyncPacket;
import com.github.edg_thexu.awesome_storage.core.registry.ModBlocks;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;


@EventBusSubscriber(modid = AwesomeStorage.MODID)
public class ModEvent {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(MagicStoragePacket.TYPE, MagicStoragePacket.STREAM_CODEC, MagicStoragePacket::handle);
        registrar.playToServer(MagicCraftPacket.TYPE, MagicCraftPacket.STREAM_CODEC, MagicCraftPacket::handle);
        registrar.playToServer(RenameBlockPacket.TYPE, RenameBlockPacket.STREAM_CODEC, RenameBlockPacket::handle);


        registrar.playToServer(BlockPosSyncPacket.TYPE, BlockPosSyncPacket.STREAM_CODEC, BlockPosSyncPacket::handle);

        registrar.playToClient(ConfigSyncPacket.TYPE, ConfigSyncPacket.STREAM_CODEC, ConfigSyncPacket::handle);
        registrar.playToClient(ChunkPacket.TYPE, ChunkPacket.STREAM_CODEC, ChunkPacket::handle);
        registrar.playToClient(StorageItemsSyncPacket.TYPE, StorageItemsSyncPacket.STREAM_CODEC, StorageItemsSyncPacket::handle);

        registrar.playToServer(QueueActionPacket.TYPE, QueueActionPacket.STREAM_CODEC, QueueActionPacket::handle);
        registrar.playToClient(QueueSyncPacket.TYPE, QueueSyncPacket.STREAM_CODEC, QueueSyncPacket::handle);

        registrar.playToServer(UpgradePacket.TYPE, UpgradePacket.STREAM_CODEC, UpgradePacket::handle);

        registrar.playToServer(AutoStockPacket.TYPE, AutoStockPacket.STREAM_CODEC, AutoStockPacket::handle);

    }

    @SubscribeEvent
    public static void onRegisterAdapter(RegisterAdapterEvent event) {
        event.register(RecipeType.CRAFTING, new CommonRecipeAdapter<>((RecipeType.CRAFTING)));
//        event.register(RecipeType.SMITHING, new SmithingRecipeAdapter<>(RecipeType.SMITHING));
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlocks.STORAGE_UNIT_BE.get(),
                (be, side) -> be.getItemHandler()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlocks.STORAGE_ARRAY_BE.get(),
                (be, side) -> be.getItemHandler()
        );
    }

}
