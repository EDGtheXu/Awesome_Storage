package coffee.awesome_storage.client.event;

import coffee.awesome_storage.client.screen.MagicStorageScreen;
import coffee.awesome_storage.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;


import static coffee.awesome_storage.Awesome_storage.MODID;


@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvent {

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MAGIC_STORAGE_MENU.get(), MagicStorageScreen::new);

    }
}
