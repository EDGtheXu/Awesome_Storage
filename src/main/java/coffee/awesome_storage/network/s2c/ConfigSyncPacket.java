package coffee.awesome_storage.network.s2c;

import coffee.awesome_storage.api.adapter.AdapterManager;
import coffee.awesome_storage.config.AbstractJsonConfig;
import coffee.awesome_storage.config.CraftConfig;
import coffee.awesome_storage.config.StorageConfig;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static coffee.awesome_storage.Awesome_storage.space;

public record ConfigSyncPacket(
        AbstractJsonConfig storage,
        AbstractJsonConfig craft
) implements CustomPacketPayload {

    public static final Type<ConfigSyncPacket> TYPE = new Type<>(space("magic_storage_config_sync_packet_s2c"));
    public static final StreamCodec<ByteBuf, ConfigSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,  ins->ins.storage.rawConfig().toString(),
            ByteBufCodecs.STRING_UTF8,  ins->ins.craft.rawConfig().toString(),
            (s1,s2)->new ConfigSyncPacket(
                    new StorageConfig(JsonParser.parseString(s1).getAsJsonObject()),
                    new CraftConfig(JsonParser.parseString(s2).getAsJsonObject())
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            System.out.println("Received ConfigSyncPacket");

            storage.loadConfig();
            craft.loadConfig();

            GsonBuilder builder = new GsonBuilder();
//            builder.setPrettyPrinting();
            String storageJson = builder.create().toJson(storage.rawConfig());
            String craftJson = builder.create().toJson(craft.rawConfig());
            String adapters = String.valueOf(AdapterManager.Adapters.size());
            String message = "ConfigSyncPacket:\nStorage Config:\n" + storageJson + "\n\nCraft Config:\n" + craftJson + "\n\nLoaded Recipe Adapters: " + adapters;
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Awesome Storage: Reload config success from server! "+message));


        });
    }
}
