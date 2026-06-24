package com.github.edg_thexu.awesome_storage.core.manager;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class WorkstationManager {

    private final BlockEntity blockEntity;
    private List<String> blockAccessors;

    public WorkstationManager(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        this.blockAccessors = new ArrayList<>();
    }

    public List<String> getBlockAccessors() { return blockAccessors; }

    public void addBlockAccessor(String name) {
        if (!blockAccessors.contains(name)) {
            blockAccessors.add(name);
            blockEntity.setChanged();
            updateClient();
        }
    }

    public void removeBlockAccessor(int index) {
        if (index >= 0 && index < blockAccessors.size()) {
            blockAccessors.remove(index);
            blockEntity.setChanged();
            updateClient();
        }
    }

    public void loadFromNbt(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        if (tag.contains("BlockAccessors", 9)) {
            ListTag listTag = tag.getList("BlockAccessors", 8);
            this.blockAccessors = new ArrayList<>();
            for (Tag t : listTag) {
                this.blockAccessors.add(t.getAsString());
            }
        }
    }

    public void saveToNbt(CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (String r : blockAccessors) {
            listTag.add(StringTag.valueOf(r));
        }
        tag.put("BlockAccessors", listTag);
    }

    private void updateClient() {
        Level level = blockEntity.getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
        }
    }
}
