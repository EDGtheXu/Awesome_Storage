package com.github.edg_thexu.awesome_storage.core.block;

import com.github.edg_thexu.awesome_storage.compat.sophisticated.SophisticatedHelper;
import com.github.edg_thexu.awesome_storage.core.menu.MagicStorageMenu;
import com.github.edg_thexu.awesome_storage.core.network.s2c.StorageItemsSyncPacket;
import com.github.edg_thexu.awesome_storage.core.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;

import java.util.*;

public final class MagicStorageBlockEntity extends BlockEntity implements MenuProvider {
    private static final Component CONTAINER_TITLE = Component.translatable("container.awesome_storage.magic_storage");

    private List<ItemStack> cachedItems;

    private List<String> block_accessors;
    boolean fake = false;

    public MagicStorageBlockEntity(BlockEntityType<MagicStorageBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.block_accessors = new ArrayList<>();
    }

    public MagicStorageBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlocks.MAGIC_STORAGE_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicStorageBlockEntity blockEntity) {
    }

    // ========================================================================
    // BFS Container Scanning — traverse through storage blocks to find all connected containers
    // Supports both vanilla Container and IItemHandler (Sophisticated Storage, etc.)
    // ========================================================================
    public List<Container> getAdjacentContainers() {
        List<Container> containers = new ArrayList<>();
        if (level == null) return containers;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        int maxSteps = 128;

        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty() && maxSteps-- > 0) {
            BlockPos cur = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos n = cur.relative(dir);
                if (!visited.add(n)) continue;
                BlockEntity be = level.getBlockEntity(n);
                if (be == null) continue;
                if (be instanceof MagicStorageBlockEntity) {
                    // Storage/crafting blocks are traversal nodes (no items, but connect to containers)
                    queue.add(n);
                } else if (be instanceof Container c) {
                    containers.add(c);
                    queue.add(n);
                } else {
                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, n, dir.getOpposite());
                    if (handler != null) {
                        ItemHandlerContainer ihc = new ItemHandlerContainer(handler);
                        if(SophisticatedHelper.isStorageLoaded() && be instanceof ChestBlockEntity chestBlockEntity) {
                            if(chestBlockEntity.isMainChest()) {
                                containers.add(ihc);
                            }
                        }else{
                            containers.add(ihc);
                        }
                        queue.add(n);
                    }
                }
            }
        }
        return containers;
    }

    /** Wraps an IItemHandler as a vanilla Container for compatibility. */
    private static class ItemHandlerContainer implements Container {
        final IItemHandler handler;
        ItemHandlerContainer(IItemHandler handler) { this.handler = handler; }
        @Override public int getContainerSize() { return handler.getSlots(); }
        @Override public boolean isEmpty() { for (int i = 0; i < handler.getSlots(); i++) if (!handler.getStackInSlot(i).isEmpty()) return false; return true; }
        @Override public ItemStack getItem(int slot) { return handler.getStackInSlot(slot); }
        @Override public ItemStack removeItem(int slot, int amount) { return handler.extractItem(slot, amount, false); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return handler.extractItem(slot, handler.getStackInSlot(slot).getCount(), false); }
        @Override public void setItem(int slot, ItemStack stack) {
            handler.extractItem(slot, handler.getStackInSlot(slot).getCount(), false);
            handler.insertItem(slot, stack, false);
        }
        @Override public void setChanged() {}
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { for (int i = 0; i < handler.getSlots(); i++) handler.extractItem(i, handler.getStackInSlot(i).getCount(), false); }
        @Override public int getMaxStackSize() { return 64; }
    }

    // ========================================================================
    // Item Operations (aggregated from adjacent containers)
    // ========================================================================
    private int cachedUsedSlots;
    private int cachedTotalSlots;

    public void setCachedItems(List<ItemStack> items) {
        this.cachedItems = items;
    }

    public void setCachedItems(List<ItemStack> items, int usedSlots, int totalSlots) {
        this.cachedItems = items;
        this.cachedUsedSlots = usedSlots;
        this.cachedTotalSlots = totalSlots;
    }

    public List<ItemStack> getStoredItems() {
        // For fake/remote entities (level is null) or client side, return cached items from server sync
        if (level == null || level.isClientSide) {
            return cachedItems != null ? cachedItems : new ArrayList<>();
        }
        // On server, compute from adjacent containers
        List<ItemStack> result = new ArrayList<>();
        for (Container c : getAdjacentContainers()) {
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
        if (level != null && !level.isClientSide && player != null) {
            List<ItemStack> items = getStoredItems();
            int used = getUsedSlots();
            int total = getTotalSlots();
            PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player,
                    new StorageItemsSyncPacket(worldPosition, items, used, total));
        }
    }

    public int getTotalItemCount() {
        int count = 0;
        for (ItemStack s : getStoredItems()) if (!s.isEmpty()) count++;
        return count;
    }

    public int getTotalSlots() {
        if (level == null || level.isClientSide) {
            return cachedTotalSlots;
        }
        int slots = 0;
        for (Container c : getAdjacentContainers()) {
            slots += c.getContainerSize();
        }
        return slots;
    }

    public int getUsedSlots() {
        if (level == null || level.isClientSide) {
            return cachedUsedSlots;
        }
        int used = 0;
        for (Container c : getAdjacentContainers()) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                if (!c.getItem(i).isEmpty()) used++;
            }
        }
        return used;
    }

    public int storeItem(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        ItemStack remaining = stack.copy();
        for (Container c : getAdjacentContainers()) {
            remaining = tryAddToContainer(c, remaining);
            if (remaining.isEmpty()) return 0;
        }
        return remaining.getCount();
    }

    private ItemStack tryAddToContainer(Container container, ItemStack stack) {
        // For IItemHandler wrappers, use insertItem directly for accurate remainder handling
        if (container instanceof ItemHandlerContainer ihc) {
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
        List<ItemStack> all = getStoredItems();
        if (index < 0 || index >= all.size()) return ItemStack.EMPTY;
        ItemStack target = all.get(index);
        int needed = target.getCount();
        ItemStack result = target.copy();
        result.setCount(0);

        outer:
        for (Container c : getAdjacentContainers()) {
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
        if (!level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return result.isEmpty() ? ItemStack.EMPTY : result;
    }

    @Nullable
    public List<ItemStack> craftAndConsume(NonNullList<Ingredient> ingredients, Set<ItemStack> excludedItems) {
        List<Container> containers = getAdjacentContainers();

        // Phase 1: Check availability using Ingredient.test() — skip excluded items
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

        // Phase 2: Consume and record actual consumed stacks — skip excluded items
        List<ItemStack> consumed = new ArrayList<>();
        for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) continue;
            int required = ing.getItems().length == 0 ? 1 : ing.getItems()[0].getCount();
            int remaining = required;

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

        if (!consumed.isEmpty() && level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        return consumed;
    }

    private boolean isExcluded(ItemStack stack, Set<ItemStack> excluded) {
        for (ItemStack ex : excluded) {
            if (ItemStack.isSameItemSameComponents(stack, ex)) return true;
        }
        return false;
    }

    // ========================================================================
    // Crafting Accessors
    // ========================================================================
    public List<String> getBlock_accessors() { return block_accessors; }

    public void addBlockAccessor(String name) {
        if (!block_accessors.contains(name)) {
            block_accessors.add(name);
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public void removeBlockAccessor(int index) {
        if (index >= 0 && index < block_accessors.size()) {
            block_accessors.remove(index);
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    // ========================================================================
    // Sync & Persistence
    // ========================================================================
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag.contains("BlockAccessors", 9)) {
            ListTag listTag = tag.getList("BlockAccessors", 8);
            this.block_accessors = new ArrayList<>();
            for (Tag t : listTag) {
                this.block_accessors.add(t.getAsString());
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("BlockAccessors", 9)) {
            ListTag listTag = tag.getList("BlockAccessors", 8);
            this.block_accessors = new ArrayList<>();
            for (Tag t : listTag) {
                this.block_accessors.add(t.getAsString());
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        ListTag listTag1 = new ListTag();
        for (String r : block_accessors) {
            listTag1.add(StringTag.valueOf(r));
        }
        tag.put("BlockAccessors", listTag1);
        return tag;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag listTag1 = new ListTag();
        for (String r : block_accessors) {
            listTag1.add(StringTag.valueOf(r));
        }
        tag.put("BlockAccessors", listTag1);
    }

    @Override
    public Component getDisplayName() { return CONTAINER_TITLE; }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        syncToClient(player);
        return new MagicStorageMenu(id, inventory, new ContainerData() {
            public int get(int id) { return 0; }
            public void set(int id, int value) {}
            public int getCount() { return 0; }
        });
    }

    // ========================================================================
    // Fake entity support
    // ========================================================================
    public void setFake(boolean fake) { this.fake = fake; }

}
