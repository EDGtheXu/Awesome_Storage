package coffee.awesome_storage.event;

import coffee.awesome_storage.Awesome_storage;
import coffee.awesome_storage.api.adapter.AdapterManager;
import coffee.awesome_storage.config.CraftConfig;
import coffee.awesome_storage.config.StorageConfig;
import coffee.awesome_storage.network.NetworkHandler;
import coffee.awesome_storage.network.s2c.ConfigSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

import static coffee.awesome_storage.Awesome_storage.MODID;


@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GameEvent {
    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent event){
        if(event.getEntity() instanceof Player player){
            if(player instanceof ServerPlayer sp){
                if(sp.connection.tickCount == 0){
                    System.out.println("Sending config to client");
                    NetworkHandler.CHANNEL.sendTo(new ConfigSyncPacket(StorageConfig.INSTANCE(),CraftConfig.INSTANCE())
                           ,sp.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
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
