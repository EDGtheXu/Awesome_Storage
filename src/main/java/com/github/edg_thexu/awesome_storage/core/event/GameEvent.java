package com.github.edg_thexu.awesome_storage.core.event;

import com.github.edg_thexu.awesome_storage.AwesomeStorage;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.api.event.RegisterWorkstationEvent;
import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import com.github.edg_thexu.awesome_storage.core.network.s2c.ConfigSyncPacket;
import com.github.edg_thexu.awesome_storage.utils.RemoteBlockEntityCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;


import static com.github.edg_thexu.awesome_storage.AwesomeStorage.MODID;


@EventBusSubscriber(modid = MODID)
public class GameEvent {
    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent event){
        if(event.getEntity() instanceof Player player){
            if(player instanceof ServerPlayer sp){
                if(sp.connection.tickCount == 0){
                    PacketDistributor.sendToPlayer(sp, new ConfigSyncPacket(CraftConfig.INSTANCE()));
                }
            }else{
                RemoteBlockEntityCache.clientLevelSource = event.getLevel().dimension();
            }
        }
    }
    @SubscribeEvent
    public static void setUp(ServerStartedEvent event){
        AwesomeStorage.LOGGER.info("loading config files");

        CraftConfig.INSTANCE().loadConfig();

        // Scan all mods for @RecipeWorkstation annotations (like JeiPlugin)
        com.github.edg_thexu.awesome_storage.api.event.RecipeWorkstationScanner.scanAll();

        // Fire event for programmatic registration
        ModLoader.postEvent(new RegisterWorkstationEvent());

        AdapterManager.init();
    }
}
