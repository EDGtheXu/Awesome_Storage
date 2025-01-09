package coffee.awesome_storage.event;

import coffee.awesome_storage.Awesome_storage;
import coffee.awesome_storage.api.adapter.AdapterManager;
import coffee.awesome_storage.config.CraftConfig;
import coffee.awesome_storage.config.StorageConfig;
import coffee.awesome_storage.item.RemoteController;
import coffee.awesome_storage.network.s2c.ConfigSyncPacket;
import coffee.awesome_storage.remote.RemoteBlockEntityCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;


import static coffee.awesome_storage.Awesome_storage.MODID;


@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)
public class GameEvent {
    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent event){
        if(event.getEntity() instanceof Player player){
            if(player instanceof ServerPlayer sp){
                if(sp.connection.tickCount == 0){
                    PacketDistributor.sendToPlayer(sp, new ConfigSyncPacket(StorageConfig.INSTANCE(),CraftConfig.INSTANCE()));
                }
            }else{
                RemoteBlockEntityCache.clientLevelSource = event.getLevel().dimension();
            }
        }
    }
    @SubscribeEvent
    public static void setUp(ServerStartedEvent event){
        Awesome_storage.LOGGER.info("loading config files");
        StorageConfig.INSTANCE().loadConfig();
        CraftConfig.INSTANCE().loadConfig();
        AdapterManager.init();
    }
}
