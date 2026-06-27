package com.github.edg_thexu.awesome_storage.compat.refinedstorage;

import com.github.edg_thexu.awesome_storage.core.registry.ModDataComponent;
import com.refinedmods.refinedstorage.common.content.Items;
import com.refinedmods.refinedstorage.common.storage.ItemStorageVariant;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

public class RefinedStorageHelper {
    static Boolean isLoad;

    public static boolean isLoaded() {
        if(isLoad == null){
            isLoad = ModList.get().isLoaded("refinedstorage");
        }

        return isLoad;
    }


    public static void modifyItem(ModifyDefaultComponentsEvent event){
        event.modify(Items.INSTANCE.getItemStorageDisk(ItemStorageVariant.ONE_K), builder -> builder.set(ModDataComponent.STORAGE_SIZE.get(), 10));
        event.modify(Items.INSTANCE.getItemStorageDisk(ItemStorageVariant.FOUR_K), builder -> builder.set(ModDataComponent.STORAGE_SIZE.get(), 40));
        event.modify(Items.INSTANCE.getItemStorageDisk(ItemStorageVariant.SIXTEEN_K), builder -> builder.set(ModDataComponent.STORAGE_SIZE.get(), 160));
        event.modify(Items.INSTANCE.getItemStorageDisk(ItemStorageVariant.SIXTY_FOUR_K), builder -> builder.set(ModDataComponent.STORAGE_SIZE.get(), 640));
    }

}
