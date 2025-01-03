package coffee.awesome_storage.event;

import coffee.awesome_storage.network.c2s.MagicCraftPacket;
import coffee.awesome_storage.network.c2s.MagicStoragePacket;
import coffee.awesome_storage.adapter.AdapterManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static coffee.awesome_storage.Awesome_storage.MODID;
import static coffee.awesome_storage.config.StorageConfig.loadUpgradeLine;
import static coffee.awesome_storage.config.CraftConfig.readFromJson;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEvent {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(MagicStoragePacket.TYPE, MagicStoragePacket.STREAM_CODEC, MagicStoragePacket::handle);
        registrar.playToServer(MagicCraftPacket.TYPE, MagicCraftPacket.STREAM_CODEC, MagicCraftPacket::handle);


        readFromJson();
        loadUpgradeLine();


        AdapterManager.init();
    }
}
