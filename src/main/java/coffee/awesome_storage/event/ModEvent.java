package coffee.awesome_storage.event;

import coffee.awesome_storage.api.adapter.SmithingRecipeAdapter;
import coffee.awesome_storage.api.event.RegisterAdapterEvent;
import coffee.awesome_storage.network.c2s.MagicCraftPacket;
import coffee.awesome_storage.network.c2s.MagicStoragePacket;
import coffee.awesome_storage.network.s2c.BlockPosSyncPacket;
import coffee.awesome_storage.network.s2c.ChunkPacket;
import coffee.awesome_storage.network.s2c.ConfigSyncPacket;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static coffee.awesome_storage.Awesome_storage.MODID;
import static coffee.awesome_storage.api.adapter.AdapterManager.defaultAdapter;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEvent {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(MagicStoragePacket.TYPE, MagicStoragePacket.STREAM_CODEC, MagicStoragePacket::handle);
        registrar.playToServer(MagicCraftPacket.TYPE, MagicCraftPacket.STREAM_CODEC, MagicCraftPacket::handle);
        registrar.playToClient(ConfigSyncPacket.TYPE, ConfigSyncPacket.STREAM_CODEC, ConfigSyncPacket::handle);
        registrar.playToServer(BlockPosSyncPacket.TYPE, BlockPosSyncPacket.STREAM_CODEC, BlockPosSyncPacket::handle);

        registrar.playToClient(ChunkPacket.TYPE, ChunkPacket.STREAM_CODEC, ChunkPacket::handle);

    }

    @SubscribeEvent
    public static void onRegisterAdapter(RegisterAdapterEvent event) {
        event.register(RecipeType.CRAFTING, defaultAdapter);
        event.register(RecipeType.SMITHING, new SmithingRecipeAdapter<>(RecipeType.SMITHING));
    }
}
