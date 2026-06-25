package com.github.edg_thexu.awesome_storage.core.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public record StorageEntry(ItemKey key, int count) {

    public StorageEntry withCount(int newCount) {
        return new StorageEntry(key, newCount);
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = key.save(registries);
        tag.putInt("count", count);
        return tag;
    }

    public static StorageEntry load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemKey key = ItemKey.load(tag, registries);
        int count = tag.getInt("count");
        return new StorageEntry(key, count);
    }

    public static List<StorageEntry> loadList(CompoundTag parentTag, HolderLookup.Provider registries) {
        List<StorageEntry> list = new ArrayList<>();
        if (parentTag.contains("Items", Tag.TAG_LIST)) {
            ListTag items = parentTag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag entry = items.getCompound(i);
                StorageEntry se = load(entry, registries);
                if (se.count() > 0) {
                    list.add(se);
                }
            }
        }
        return list;
    }

    public static void saveList(ListTag list, List<StorageEntry> entries, HolderLookup.Provider registries) {
        for (StorageEntry se : entries) {
            list.add(se.save(registries));
        }
    }
}
