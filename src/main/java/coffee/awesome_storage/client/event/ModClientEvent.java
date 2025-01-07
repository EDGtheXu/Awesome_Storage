package coffee.awesome_storage.client.event;

import coffee.awesome_storage.client.screen.MagicStorageScreen;
import coffee.awesome_storage.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static coffee.awesome_storage.Awesome_storage.MODID;


@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvent {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

            MenuScreens.register(ModMenus.MAGIC_STORAGE_MENU.get(), MagicStorageScreen::new);

        });
    }


}
