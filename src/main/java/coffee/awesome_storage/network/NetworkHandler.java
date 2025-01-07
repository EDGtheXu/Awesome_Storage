package coffee.awesome_storage.network;

import coffee.awesome_storage.Awesome_storage;
import coffee.awesome_storage.network.c2s.MagicCraftPacket;
import coffee.awesome_storage.network.c2s.MagicStoragePacket;
import coffee.awesome_storage.network.s2c.ConfigSyncPacket;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        Awesome_storage.space("main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++,  MagicCraftPacket.class,  MagicCraftPacket::encode,  MagicCraftPacket::decode,  MagicCraftPacket::handle);
        CHANNEL.registerMessage(packetId++,  MagicStoragePacket.class,  MagicStoragePacket::encode,  MagicStoragePacket::decode,  MagicStoragePacket::handle);
        CHANNEL.registerMessage(packetId++,  ConfigSyncPacket.class,  ConfigSyncPacket::encode,  ConfigSyncPacket::decode, ConfigSyncPacket::handle);


    }
}
