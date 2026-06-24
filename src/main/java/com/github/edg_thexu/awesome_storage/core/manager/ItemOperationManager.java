package com.github.edg_thexu.awesome_storage.core.manager;

import com.github.edg_thexu.awesome_storage.core.network.s2c.StorageItemsSyncPacket;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.stream.Collectors;

public class ItemOperationManager {

    private final StorageContainerScanner scanner;
    private List<ItemStack> cachedItems;
    private int cachedUsedSlots;
    private int cachedTotalSlots;
    private final BlockEntity blockEntity;

    public ItemOperationManager(BlockEntity blockEntity, StorageContainerScanner scanner) {
        this.blockEntity = blockEntity;
        this.scanner = scanner;
    }

    public void setCachedItems(List<ItemStack> items) {
        this.cachedItems = items;
    }

    public void setCachedItems(List<ItemStack> items, int usedSlots, int totalSlots) {
        this.cachedItems = items;
        this.cachedUsedSlots = usedSlots;
        this.cachedTotalSlots = totalSlots;
    }

    public List<ItemStack> getStoredItems() {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return cachedItems != null ? cachedItems : new ArrayList<>();
        }
        List<Container> containers = scanner.getAdjacentContainers(level, blockEntity.getBlockPos());
        List<ItemStack> result = new ArrayList<>();
        for (Container c : containers) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                if (s.isEmpty()) continue;
                boolean merged = false;
                for (ItemStack r : result) {
                    if (ItemStack.isSameItemSameComponents(r, s)) {
                        int add = Math.min(s.getCount(), r.getMaxStackSize() - r.getCount());
                        r.grow(add);
                        if (add < s.getCount()) {
                            ItemStack remainder = s.copy();
                            remainder.setCount(s.getCount() - add);
                            result.add(remainder);
                        }
                        merged = true;
                        break;
                    }
                }
                if (!merged) result.add(s.copy());
            }
        }
        result.sort(Comparator.comparing(a -> a.getDisplayName().getString()));
        return result;
    }

    public void syncToClient(Player player) {
        Level level = blockEntity.getLevel();
        if (level != null && !level.isClientSide && player != null) {
            List<ItemStack> items = getStoredItems();
            int used = getUsedSlots();
            int total = getTotalSlots();
            PacketDistributor.sendToPlayer((ServerPlayer) player,
                    new StorageItemsSyncPacket(blockEntity.getBlockPos(), items, used, total));
        }
    }

    public int getTotalItemCount() {
        int count = 0;
        for (ItemStack s : getStoredItems()) if (!s.isEmpty()) count++;
        return count;
    }

    public int getTotalSlots() {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return cachedTotalSlots;
        }
        int slots = 0;
        for (Container c : scanner.getAdjacentContainers(level, blockEntity.getBlockPos())) {
            slots += c.getContainerSize();
        }
        return slots;
    }

    public int getUsedSlots() {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return cachedUsedSlots;
        }
        int used = 0;
        for (Container c : scanner.getAdjacentContainers(level, blockEntity.getBlockPos())) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                if (!c.getItem(i).isEmpty()) used++;
            }
        }
        return used;
    }

    public int storeItem(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Level level = blockEntity.getLevel();
        ItemStack remaining = stack.copy();
        for (Container c : scanner.getAdjacentContainers(level, blockEntity.getBlockPos())) {
            remaining = tryAddToContainer(c, remaining);
            if (remaining.isEmpty()) return 0;
        }
        return remaining.getCount();
    }

    private ItemStack tryAddToContainer(Container container, ItemStack stack) {
        if (container instanceof StorageContainerScanner.ItemHandlerContainer ihc) {
            IItemHandler h = ihc.handler;
            ItemStack remaining = stack.copy();
            for (int i = 0; i < h.getSlots(); i++) {
                if (remaining.isEmpty()) break;
                remaining = h.insertItem(i, remaining, false);
            }
            return remaining;
        }
        ItemStack toAdd = stack.copy();
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (toAdd.isEmpty()) break;
            ItemStack existing = container.getItem(i);
            if (existing.isEmpty()) {
                int maxSize = Math.min(container.getMaxStackSize(toAdd), toAdd.getMaxStackSize());
                int count = Math.min(toAdd.getCount(), maxSize);
                ItemStack put = toAdd.copy();
                put.setCount(count);
                container.setItem(i, put);
                toAdd.shrink(count);
            } else if (ItemStack.isSameItemSameComponents(existing, toAdd) && existing.getCount() < existing.getMaxStackSize()) {
                int maxAdd = Math.min(existing.getMaxStackSize() - existing.getCount(), toAdd.getCount());
                existing.grow(maxAdd);
                container.setItem(i, existing);
                toAdd.shrink(maxAdd);
            }
        }
        container.setChanged();
        return toAdd;
    }

    public ItemStack takeItem(int index) {
        Level level = blockEntity.getLevel();
        List<ItemStack> all = getStoredItems();
        if (index < 0 || index >= all.size()) return ItemStack.EMPTY;
        ItemStack target = all.get(index);
        int needed = target.getCount();
        ItemStack result = target.copy();
        result.setCount(0);

        outer:
        for (Container c : scanner.getAdjacentContainers(level, blockEntity.getBlockPos())) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, target)) {
                    int take = Math.min(needed - result.getCount(), s.getCount());
                    if (take > 0) {
                        result.grow(take);
                        s.shrink(take);
                        if (s.isEmpty()) c.setItem(i, ItemStack.EMPTY);
                        else c.setItem(i, s);
                        c.setChanged();
                        if (result.getCount() >= needed) break outer;
                    }
                }
            }
        }
        updateClient();
        return result.isEmpty() ? ItemStack.EMPTY : result;
    }

    public void depositAll(Player player, long favoriteMask) {
        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if ((favoriteMask & (1L << i)) != 0) continue;
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                ItemStack toStore = stack.copy();
                int remaining = storeItem(toStore);
                if (remaining < toStore.getCount()) {
                    stack.setCount(remaining);
                    if (stack.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        updateClient();
    }

    public void quickStack(Player player, long favoriteMask) {
        Level level = blockEntity.getLevel();
        List<ItemStack> stored = getStoredItems();
        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if ((favoriteMask & (1L << i)) != 0) continue;
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                boolean exists = false;
                for (ItemStack s : stored) {
                    if (ItemStack.isSameItemSameComponents(s, stack)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    ItemStack toStore = stack.copy();
                    int remaining = storeItem(toStore);
                    if (remaining < toStore.getCount()) {
                        stack.setCount(remaining);
                        if (stack.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        updateClient();
    }

    public void refill(Player player) {
        Level level = blockEntity.getLevel();
        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.isStackable() && stack.getCount() < stack.getMaxStackSize()) {
                int needed = stack.getMaxStackSize() - stack.getCount();
                for (Container c : scanner.getAdjacentContainers(level, blockEntity.getBlockPos())) {
                    if (needed <= 0) break;
                    for (int j = 0; j < c.getContainerSize(); j++) {
                        ItemStack s = c.getItem(j);
                        if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)) {
                            int take = Math.min(needed, s.getCount());
                            s.shrink(take);
                            stack.grow(take);
                            needed -= take;
                            if (s.isEmpty()) c.setItem(j, ItemStack.EMPTY);
                            c.setChanged();
                            if (needed <= 0) break;
                        }
                    }
                }
            }
        }
        updateClient();
    }

    public List<ItemStack> craftAndConsume(NonNullList<Ingredient> ingredients, Set<ItemStack> excludedItems) {
        Level level = blockEntity.getLevel();
        List<Container> containers = scanner.getAdjacentContainers(level, blockEntity.getBlockPos());

        for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) continue;
            int required = ing.getItems().length == 0 ? 1 : ing.getItems()[0].getCount();
            int available = 0;
            for (Container c : containers) {
                for (int i = 0; i < c.getContainerSize(); i++) {
                    ItemStack s = c.getItem(i);
                    if (s.isEmpty() || !ing.test(s)) continue;
                    if (isExcluded(s, excludedItems)) continue;
                    available += s.getCount();
                }
            }
            if (available < required) return null;
        }

        List<ItemStack> consumed = new ArrayList<>();
        for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) continue;
            int remaining = ing.getItems().length == 0 ? 1 : ing.getItems()[0].getCount();

            for (Container c : containers) {
                for (int i = 0; i < c.getContainerSize(); i++) {
                    if (remaining <= 0) break;
                    ItemStack s = c.getItem(i);
                    if (s.isEmpty() || !ing.test(s)) continue;
                    if (isExcluded(s, excludedItems)) continue;

                    int take = Math.min(remaining, s.getCount());
                    ItemStack part = s.copy();
                    part.setCount(take);
                    consumed.add(part);

                    s.shrink(take);
                    if (s.isEmpty()) c.setItem(i, ItemStack.EMPTY);
                    else c.setItem(i, s);
                    c.setChanged();

                    remaining -= take;
                }
            }
        }

        if (!consumed.isEmpty()) {
            updateClient();
        }

        return consumed;
    }

    private boolean isExcluded(ItemStack stack, Set<ItemStack> excluded) {
        for (ItemStack ex : excluded) {
            if (ItemStack.isSameItemSameComponents(stack, ex)) return true;
        }
        return false;
    }

    private void updateClient() {
        Level level = blockEntity.getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
        }
    }
}
