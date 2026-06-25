package com.github.edg_thexu.awesome_storage.core.block;

import com.github.edg_thexu.awesome_storage.core.menu.StorageArrayMenu;
import com.github.edg_thexu.awesome_storage.core.registry.ModBlocks;
import com.github.edg_thexu.awesome_storage.core.storage.MapItemHandler;
import com.github.edg_thexu.awesome_storage.core.storage.StorageEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StorageArrayBlockEntity extends BlockEntity implements MenuProvider {

    public static final int UNIT_SLOT_COUNT = 8;

    private final ItemStack[] unitSlots = new ItemStack[UNIT_SLOT_COUNT];
    private final List<StorageEntry> storage = new ArrayList<>();
    private final MapItemHandler mapHandler;
    private final IItemHandler externalHandler;
    private final UnitSlotContainer unitSlotContainer = new UnitSlotContainer();
    private int slotItemLimit = 999;

    public StorageArrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.STORAGE_ARRAY_BE.get(), pos, state);
        this.mapHandler = new MapItemHandler(storage, this::setChanged, slotItemLimit, 0);
        this.externalHandler = new CapacityAwareHandler();
        Arrays.fill(unitSlots, ItemStack.EMPTY);
    }

    public ItemStack[] getUnitSlots() {
        return unitSlots;
    }

    public Container getUnitContainer() {
        return unitSlotContainer;
    }

    public List<StorageEntry> getStorage() {
        return storage;
    }

    public int getSlotItemLimit() {
        return slotItemLimit;
    }

    public IItemHandler getItemHandler() {
        return externalHandler;
    }

    public int getTotalItemCount() {
        return storage.stream().mapToInt(StorageEntry::count).sum();
    }

    public int getInstalledUnitCount() {
        int count = 0;
        for (ItemStack s : unitSlots) {
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof StorageUnitBlock) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCapacity() {
        int totalSlots = 0;
        for (ItemStack s : unitSlots) {
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof StorageUnitBlock sub) {
                totalSlots += sub.getSlotCapacity();
            }
        }
        return totalSlots * slotItemLimit;
    }

    public boolean canRemoveUnit(int slot) {
        if (slot < 0 || slot >= UNIT_SLOT_COUNT) return false;
        ItemStack stack = unitSlots[slot];
        if (stack.isEmpty()) return false;
        int unitCap = 0;
        if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof StorageUnitBlock sub) {
            unitCap = sub.getSlotCapacity() * slotItemLimit;
        }
        int remainingCapacity = getTotalCapacity() - unitCap;
        return getTotalItemCount() <= remainingCapacity;
    }

    public void setUnitSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= UNIT_SLOT_COUNT) return;
        unitSlots[slot] = stack.copy();
        refreshSlotCapacity();
        setChanged();
    }

    private void refreshSlotCapacity() {
        int total = 0;
        for (ItemStack s : unitSlots) {
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof StorageUnitBlock sub) {
                total += sub.getSlotCapacity();
            }
        }
        mapHandler.setSlotCapacity(total);
    }


    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.awesome_storage.storage_array");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new StorageArrayMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("maxStackSize")) {
            slotItemLimit = tag.getInt("maxStackSize");
        }
        if (tag.contains("UnitSlots", Tag.TAG_LIST)) {
            ListTag list = tag.getList("UnitSlots", Tag.TAG_COMPOUND);
            Arrays.fill(unitSlots, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(list.size(), UNIT_SLOT_COUNT); i++) {
                unitSlots[i] = ItemStack.parseOptional(registries, list.getCompound(i));
            }
        }
        storage.clear();
        storage.addAll(StorageEntry.loadList(tag, registries));
        refreshSlotCapacity();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("maxStackSize", slotItemLimit);
        ListTag unitList = new ListTag();
        for (ItemStack s : unitSlots) {
            unitList.add(s.saveOptional(registries));
        }
        tag.put("UnitSlots", unitList);
        ListTag itemList = new ListTag();
        StorageEntry.saveList(itemList, storage, registries);
        tag.put("Items", itemList);
    }

    private class CapacityAwareHandler implements IItemHandler {
        @Override public int getSlots() { return mapHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return mapHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            int totalCapacity = getTotalCapacity();
            if (totalCapacity <= 0) return stack;
            int currentTotal = getTotalItemCount();
            if (currentTotal >= totalCapacity) return stack;
            int maxInsert = totalCapacity - currentTotal;
            if (stack.getCount() <= maxInsert) {
                return mapHandler.insertItem(slot, stack, simulate);
            }
            ItemStack limited = stack.copy();
            limited.setCount(maxInsert);
            ItemStack remaining = mapHandler.insertItem(slot, limited, simulate);
            ItemStack result = stack.copy();
            int inserted = maxInsert - (remaining.isEmpty() ? 0 : remaining.getCount());
            result.setCount(stack.getCount() - inserted);
            return result.isEmpty() ? ItemStack.EMPTY : result;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return mapHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return mapHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return mapHandler.isItemValid(slot, stack);
        }
    }

    private class UnitSlotContainer implements Container {
        @Override public int getContainerSize() { return UNIT_SLOT_COUNT; }
        @Override public boolean isEmpty() {
            for (ItemStack s : unitSlots) if (!s.isEmpty()) return false;
            return true;
        }
        @Override public @NotNull ItemStack getItem(int slot) {
            if (slot < 0 || slot >= UNIT_SLOT_COUNT) return ItemStack.EMPTY;
            return unitSlots[slot];
        }
        @Override public @NotNull ItemStack removeItem(int slot, int amount) {
            if (slot < 0 || slot >= UNIT_SLOT_COUNT || unitSlots[slot].isEmpty()) return ItemStack.EMPTY;
            ItemStack stack = unitSlots[slot].split(amount);
            if (unitSlots[slot].isEmpty()) unitSlots[slot] = ItemStack.EMPTY;
            setChanged();
            refreshSlotCapacity();
            return stack;
        }
        @Override public @NotNull ItemStack removeItemNoUpdate(int slot) {
            if (slot < 0 || slot >= UNIT_SLOT_COUNT) return ItemStack.EMPTY;
            ItemStack stack = unitSlots[slot];
            unitSlots[slot] = ItemStack.EMPTY;
            return stack;
        }
        @Override public void setItem(int slot, @NotNull ItemStack stack) {
            setUnitSlot(slot, stack);
        }
        @Override public void setChanged() { StorageArrayBlockEntity.this.setChanged(); }
        @Override public boolean stillValid(@NotNull Player player) { return true; }
        @Override public void clearContent() {
            Arrays.fill(unitSlots, ItemStack.EMPTY);
            setChanged();
            refreshSlotCapacity();
        }
    }
}
