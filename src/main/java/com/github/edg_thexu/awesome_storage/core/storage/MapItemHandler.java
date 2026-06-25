package com.github.edg_thexu.awesome_storage.core.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MapItemHandler implements IItemHandler {

    private final List<StorageEntry> entries;
    private final Runnable onChanged;
    private final int maxStackSize;
    private int slotCapacity;

    private boolean dirty = true;

    public MapItemHandler(List<StorageEntry> entries, Runnable onChanged, int maxStackSize, int slotCapacity) {
        this.entries = entries;
        this.onChanged = onChanged;
        this.maxStackSize = maxStackSize;
        this.slotCapacity = slotCapacity;
    }

    public MapItemHandler(List<StorageEntry> entries, Runnable onChanged) {
        this(entries, onChanged, 999, Integer.MAX_VALUE);
    }

    public void setSlotCapacity(int slotCapacity) {
        this.slotCapacity = slotCapacity;
    }

    private void sort() {
        if (!dirty) return;
        entries.sort(Comparator.comparing(e -> e.key().item().getDescriptionId()));
        dirty = false;
    }

    @Override
    public int getSlots() {
        return slotCapacity;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        sort();
        if (slot < 0 || slot >= entries.size()) return ItemStack.EMPTY;
        StorageEntry e = entries.get(slot);
        return e.key().toStack(Math.min(e.count(), maxStackSize));
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        sort();

        ItemKey incoming = ItemKey.from(stack);

        if (slot < entries.size()) {
            StorageEntry e = entries.get(slot);
            if (!e.key().equals(incoming)) return stack;

            int current = e.count();
            int canAdd = maxStackSize - current;
            if (canAdd <= 0) return stack;

            int toAdd = Math.min(stack.getCount(), canAdd);
            int remaining = stack.getCount() - toAdd;

            if (!simulate && toAdd > 0) {
                entries.set(slot, e.withCount(current + toAdd));
                onChanged.run();
            }

            if (remaining > 0) {
                ItemStack leftover = stack.copy();
                leftover.setCount(remaining);
                return leftover;
            }
            return ItemStack.EMPTY;
        }

        int remaining = stack.getCount();
        if (!simulate) {
            for (int i = 0; i < entries.size() && remaining > 0; i++) {
                StorageEntry e = entries.get(i);
                if (e.key().equals(incoming) && e.count() < maxStackSize) {
                    int canAdd = maxStackSize - e.count();
                    int toAdd = Math.min(remaining, canAdd);
                    entries.set(i, e.withCount(e.count() + toAdd));
                    remaining -= toAdd;
                }
            }
            while (remaining > 0 && entries.size() < slotCapacity) {
                int toAdd = Math.min(remaining, maxStackSize);
                entries.add(new StorageEntry(incoming, toAdd));
                remaining -= toAdd;
                dirty = true;
            }
            if (remaining < stack.getCount()) {
                onChanged.run();
            }
        } else {
            int availableSpace = 0;
            for (StorageEntry e : entries) {
                if (e.key().equals(incoming)) {
                    availableSpace += maxStackSize - e.count();
                }
            }
            int totalSlots = entries.size();
            if (totalSlots < slotCapacity) {
                availableSpace += (slotCapacity - totalSlots) * maxStackSize;
            }
            remaining = Math.max(0, stack.getCount() - availableSpace);
        }

        if (remaining > 0) {
            ItemStack leftover = stack.copy();
            leftover.setCount(remaining);
            return leftover;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        sort();
        if (slot < 0 || slot >= entries.size()) return ItemStack.EMPTY;

        StorageEntry e = entries.get(slot);
        int current = e.count();
        if (current <= 0) return ItemStack.EMPTY;

        int toExtract = Math.min(amount, current);
        ItemStack result = e.key().toStack(toExtract);

        if (!simulate) {
            if (toExtract >= current) {
                entries.remove(slot);
                dirty = true;
            } else {
                entries.set(slot, e.withCount(current - toExtract));
            }
            onChanged.run();
        }

        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return maxStackSize;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return !stack.isEmpty();
    }
}
