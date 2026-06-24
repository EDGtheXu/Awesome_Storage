package com.github.edg_thexu.awesome_storage.compat.jade;

import com.github.edg_thexu.awesome_storage.core.block.CraftingUnitBlock;
import com.github.edg_thexu.awesome_storage.core.block.StorageCoreBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class ASJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {

        registration.registerBlockComponent(MagicBlockJadeProvider.INSTANCE, CraftingUnitBlock.class);
        registration.registerBlockComponent(MagicBlockJadeProvider.INSTANCE, StorageCoreBlock.class);

    }
}
