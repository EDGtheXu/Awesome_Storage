package com.github.edg_thexu.awesome_storage.core.event;

import com.github.edg_thexu.awesome_storage.AwesomeStorage;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.api.event.RecipeWorkstationScanner;
import com.github.edg_thexu.awesome_storage.api.event.RegisterWorkstationEvent;
import com.github.edg_thexu.awesome_storage.compat.refinedstorage.RefinedStorageHelper;
import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import com.github.edg_thexu.awesome_storage.core.network.s2c.ConfigSyncPacket;
import com.github.edg_thexu.awesome_storage.core.registry.ModBlocks;
import com.github.edg_thexu.awesome_storage.core.registry.ModDataComponent;
import com.github.edg_thexu.qtcraft_api.core.QTheme;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.MODID;

@EventBusSubscriber(modid = MODID)
public class GameEvent {

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event){

        // Fire event for programmatic registration
        ModLoader.postEvent(new RegisterWorkstationEvent());
        AdapterManager.init();
        AwesomeStorage.clear();
    }

    @SubscribeEvent
    public static void joinLevel(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer sp && FMLEnvironment.dist.isDedicatedServer()){
            PacketDistributor.sendToPlayer(sp, new ConfigSyncPacket(CraftConfig.INSTANCE()));
        }
    }

    @SubscribeEvent
    public static void setUp(ServerStartedEvent event){
        AwesomeStorage.LOGGER.info("loading config files");

        CraftConfig.INSTANCE().loadConfig();
        // Scan all mods for @RecipeWorkstation annotations (like JeiPlugin)
        RecipeWorkstationScanner.scanAll();

    }

    @SubscribeEvent
    public static void collectTooltip(ItemTooltipEvent event){
        var data = event.getItemStack().get(ModDataComponent.STORAGE_SIZE.get());
        if(data != null) {
            event.getToolTip().add(Component.translatable("awesome_storage.tooltip.storage_unit", data).withStyle(Style.EMPTY.withColor(QTheme.TEXT.disabled.argb())));
        }
    }

    @SubscribeEvent
    public static void modifyItem(ModifyDefaultComponentsEvent event){
        event.modify(ModBlocks.STORAGE_UNIT_BLOCK.get().asItem(), builder -> builder.set(ModDataComponent.STORAGE_SIZE.get(), 40));

        if(RefinedStorageHelper.isLoaded()) {
            RefinedStorageHelper.modifyItem(event);
        }

    }
}
