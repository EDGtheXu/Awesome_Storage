package com.github.edg_thexu.awesome_storage.core.network.s2c;

import com.github.edg_thexu.awesome_storage.AwesomeStorage;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.api.event.RecipeWorkstationScanner;
import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.space;

public record ConfigSyncPacket(
        CraftConfig craft
) implements CustomPacketPayload {

    public static final Type<ConfigSyncPacket> TYPE = new Type<>(space("magic_storage_config_sync_packet_s2c"));
    public static final StreamCodec<ByteBuf, ConfigSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,  ins->ins.craft.rawConfig().toString(),
            (s1)->new ConfigSyncPacket(
                    new CraftConfig(JsonParser.parseString(s1).getAsJsonObject())
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            AwesomeStorage.LOGGER.info("Received ConfigSyncPacket");
            CraftConfig.INSTANCE().loadFrom(craft);
            RecipeWorkstationScanner.scanAll();
            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting();
            String craftJson = builder.create().toJson(CraftConfig.INSTANCE().rawConfig());
            String adapters = String.valueOf(AdapterManager.Adapters.size());
            String message = "Craft Config From Server:\n" + craftJson + "\n\nLoaded Recipe Adapters: " + adapters;
            AwesomeStorage.LOGGER.info(message);
//            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Awesome Storage: Reload config success from server! "+message));
        });
    }
}
