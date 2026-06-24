package com.github.edg_thexu.awesome_storage.compat.jade;

import com.github.edg_thexu.awesome_storage.AwesomeStorage;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

public enum MagicBlockJadeProvider implements IBlockComponentProvider, StreamServerDataProvider<BlockAccessor, String> {
    INSTANCE;

    static final ResourceLocation uid = AwesomeStorage.space("magic_block");

    MagicBlockJadeProvider() {
    }

    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if(accessor.getBlockEntity() instanceof MagicStorageBlockEntity mbe) {
            if(!mbe.displayName.equals(Component.empty())) {
                tooltip.add(mbe.displayName);
            }
        }
    }

    public String streamData(BlockAccessor accessor) {
        return null;
    }

    public StreamCodec<RegistryFriendlyByteBuf, String> streamCodec() {
        return ByteBufCodecs.STRING_UTF8.cast();
    }

    public ResourceLocation getUid() {
        return uid;
    }

}
