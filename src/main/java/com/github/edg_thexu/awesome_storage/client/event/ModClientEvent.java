package com.github.edg_thexu.awesome_storage.client.event;

import com.github.edg_thexu.awesome_storage.api.filter.FilterRuleRegistry;
import com.github.edg_thexu.awesome_storage.client.screen.StorageArrayScreen;
import com.github.edg_thexu.awesome_storage.client.screen.magicstorage.MagicStorageScreen;
import com.github.edg_thexu.awesome_storage.core.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;


import static com.github.edg_thexu.awesome_storage.AwesomeStorage.MODID;


@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ModClientEvent {

    @SubscribeEvent
    public static void joinLevel(FMLClientSetupEvent event){
        FilterRuleRegistry.init();
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MAGIC_STORAGE_MENU.get(), MagicStorageScreen::new);
        event.register(ModMenus.STORAGE_ARRAY_MENU.get(), StorageArrayScreen::new);

    }
}
