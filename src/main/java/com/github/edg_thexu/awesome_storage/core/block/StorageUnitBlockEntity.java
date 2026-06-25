package com.github.edg_thexu.awesome_storage.core.block;

import com.github.edg_thexu.awesome_storage.core.registry.ModBlocks;
import com.github.edg_thexu.awesome_storage.core.storage.MapItemHandler;
import com.github.edg_thexu.awesome_storage.core.storage.StorageEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StorageUnitBlockEntity extends BlockEntity {

    private final List<StorageEntry> storage = new ArrayList<>();
    private final MapItemHandler itemHandler;
    private int slotItemLimit = 999;
    private int slotCapacity = 40;

    public StorageUnitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.STORAGE_UNIT_BE.get(), pos, state);
        this.itemHandler = new MapItemHandler(storage, this::setChanged, slotItemLimit, slotCapacity);
    }

    public List<StorageEntry> getStorage() {
        return storage;
    }

    public int getSlotItemLimit() {
        return slotItemLimit;
    }

    public void setSlotItemLimit(int slotItemLimit) {
        this.slotItemLimit = slotItemLimit;
    }

    public int getSlotCapacity() {
        return slotCapacity;
    }

    public void setSlotCapacity(int slotCapacity) {
        this.slotCapacity = slotCapacity;
    }

    public int getTotalItemCount() {
        return storage.stream().mapToInt(StorageEntry::count).sum();
    }

    public int getUniqueItemCount() {
        return (int) storage.stream().map(StorageEntry::key).distinct().count();
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("maxStackSize")) {
            slotItemLimit = tag.getInt("maxStackSize");
        }
        if (tag.contains("slotCapacity")) {
            slotCapacity = tag.getInt("slotCapacity");
        }
        storage.clear();
        storage.addAll(StorageEntry.loadList(tag, registries));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("maxStackSize", slotItemLimit);
        tag.putInt("slotCapacity", slotCapacity);
        ListTag list = new ListTag();
        StorageEntry.saveList(list, storage, registries);
        tag.put("Items", list);
    }
}
