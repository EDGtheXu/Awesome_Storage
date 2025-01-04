package coffee.awesome_storage.event;

import coffee.awesome_storage.api.adapter.SmithingRecipeAdapter;
import coffee.awesome_storage.api.event.RegisterAdapterEvent;
import coffee.awesome_storage.network.c2s.MagicCraftPacket;
import coffee.awesome_storage.network.c2s.MagicStoragePacket;
import coffee.awesome_storage.api.adapter.AdapterManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static coffee.awesome_storage.Awesome_storage.MODID;
import static coffee.awesome_storage.api.adapter.AdapterManager.defaultAdapter;
import static coffee.awesome_storage.config.StorageConfig.loadStorageConfig;
import static coffee.awesome_storage.config.CraftConfig.loadCraftConfig;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEvent {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(MagicStoragePacket.TYPE, MagicStoragePacket.STREAM_CODEC, MagicStoragePacket::handle);
        registrar.playToServer(MagicCraftPacket.TYPE, MagicCraftPacket.STREAM_CODEC, MagicCraftPacket::handle);

        loadStorageConfig();
        loadCraftConfig();

        AdapterManager.init();
    }

    @SubscribeEvent
    public static void onConfigReload(RegisterAdapterEvent event) {
        event.register(RecipeType.CRAFTING, defaultAdapter);
        event.register(RecipeType.SMITHING, new SmithingRecipeAdapter<>(RecipeType.SMITHING));
    }
}
