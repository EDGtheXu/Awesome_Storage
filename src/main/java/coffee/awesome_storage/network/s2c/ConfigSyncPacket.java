package coffee.awesome_storage.network.s2c;

import coffee.awesome_storage.api.adapter.AdapterManager;
import coffee.awesome_storage.config.AbstractJsonConfig;
import coffee.awesome_storage.config.CraftConfig;
import coffee.awesome_storage.config.StorageConfig;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public record ConfigSyncPacket(
        AbstractJsonConfig storage,
        AbstractJsonConfig craft
) {

    public static void encode(ConfigSyncPacket packet, FriendlyByteBuf friendlyByteBuf) {
        byte[] Bytes = packet.storage.rawConfig().toString().getBytes();
        int len = Bytes.length;
        friendlyByteBuf.writeInt(len);
        friendlyByteBuf.writeBytes(Bytes);
        Bytes = packet.craft.rawConfig().toString().getBytes();
        len = Bytes.length;
        friendlyByteBuf.writeInt(len);
        friendlyByteBuf.writeBytes(Bytes);
    }

    public static ConfigSyncPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new ConfigSyncPacket(
                new StorageConfig(JsonParser.parseString(friendlyByteBuf.readBytes(friendlyByteBuf.readInt()).toString(StandardCharsets.UTF_8)).getAsJsonObject()),
                new CraftConfig(JsonParser.parseString(friendlyByteBuf.readBytes(friendlyByteBuf.readInt()).toString(StandardCharsets.UTF_8)).getAsJsonObject())
        );
    }

    // 客户端使用，防止重复处理
    static long last_time = System.currentTimeMillis() << 1;
    public static void handle(ConfigSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        if(last_time + 1000 > System.currentTimeMillis())
            return;
        context.enqueueWork(() -> {
            System.out.println("Received ConfigSyncPacket");

            AbstractJsonConfig storage = packet.storage;
            AbstractJsonConfig craft = packet.craft;
            storage.loadConfig();
            craft.loadConfig();

            GsonBuilder builder = new GsonBuilder();
//            builder.setPrettyPrinting();
            String storageJson = builder.create().toJson(storage.rawConfig());
            String craftJson = builder.create().toJson(craft.rawConfig());
            String adapters = String.valueOf(AdapterManager.Adapters.size());
            String message = "ConfigSyncPacket:\n\nStorage Config:\n" + storageJson + "\nCraft Config:\n" + craftJson + "\n\nLoaded Recipe Adapters: " + adapters+"\n\n";
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Awesome Storage:").withStyle(Style.EMPTY.withColor(0x00AA00)));
            Minecraft.getInstance().player.sendSystemMessage((Component.literal("Reload config success from server! "+message)).withStyle(Style.EMPTY.withColor(0xFFFFFF)));

            last_time   = System.currentTimeMillis();
        });
    }
}
