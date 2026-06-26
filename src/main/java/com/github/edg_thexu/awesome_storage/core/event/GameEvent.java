package com.github.edg_thexu.awesome_storage.core.event;

import com.github.edg_thexu.awesome_storage.AwesomeStorage;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.api.event.RecipeWorkstationScanner;
import com.github.edg_thexu.awesome_storage.api.event.RegisterWorkstationEvent;
import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import com.github.edg_thexu.awesome_storage.core.network.s2c.ConfigSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
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
}
