package coffee.awesome_storage.block;

import coffee.awesome_storage.menu.MagicStorageMenu;
import coffee.awesome_storage.network.s2c.BlockPosSyncPacket;
import coffee.awesome_storage.network.s2c.StorageItemsSyncPacket;
import coffee.awesome_storage.registry.ModBlocks;
import coffee.awesome_storage.registry.ModDataComponent;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public final class MagicStorageBlockEntity extends BlockEntity implements MenuProvider {
    private static final Component CONTAINER_TITLE = Component.translatable("container.awesome_storage.magic_storage");

    public int max_size = 20;
    public int lvl = 0;
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
                switch (be) {
                    case MagicStorageBlockEntity ignored -> queue.add(n);
                    case Container c -> containers.add(c);
                    case null, default -> {
                    }
                }
            }
        }
        return containers;
    }

    // ========================================================================
    // Item Operations (aggregated from adjacent containers)
    // ========================================================================
    public void setCachedItems(List<ItemStack> items) {
        this.cachedItems = items;
    }

    public List<ItemStack> getStoredItems() {
        // On client, return cached items from server sync
        if (level != null && level.isClientSide) {
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
            PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player,
                    new StorageItemsSyncPacket(worldPosition, items));
        }
    }

    public int getTotalItemCount() {
        int count = 0;
        for (ItemStack s : getStoredItems()) if (!s.isEmpty()) count++;
        return count;
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

    public boolean craftAndConsume(NonNullList<Ingredient> ingredients) {
        // First check if all ingredients are available
        List<Container> containers = getAdjacentContainers();
        Map<Item, Integer> needed = new HashMap<>();
        for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) continue;
            boolean found = false;
            for (ItemStack is : ing.getItems()) {
                Item item = is.getItem();
                int count = is.getCount();
                if (countAvailable(containers, item) >= count) {
                    needed.merge(item, count, Integer::sum);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        // Remove items from containers
        for (Map.Entry<Item, Integer> entry : needed.entrySet()) {
            removeFromContainers(containers, entry.getKey(), entry.getValue());
        }
        return true;
    }

    private int countAvailable(List<Container> containers, Item item) {
        int total = 0;
        for (Container c : containers) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                if (s.getItem() == item) total += s.getCount();
            }
        }
        return total;
    }

    private void removeFromContainers(List<Container> containers, Item item, int amount) {
        for (Container c : containers) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                if (s.getItem() == item && amount > 0) {
                    int take = Math.min(amount, s.getCount());
                    s.shrink(take);
                    if (s.isEmpty()) c.setItem(i, ItemStack.EMPTY);
                    else c.setItem(i, s);
                    c.setChanged();
                    amount -= take;
                    if (amount <= 0) return;
                }
            }
        }
    }

    // ========================================================================
    // Crafting Accessors
    // ========================================================================
    public List<String> getBlock_accessors() { return block_accessors; }

    public void addBlockAccessor(String name) {
        if (!block_accessors.contains(name)) {
            block_accessors.add(name);
            setChanged();
        }
    }

    public void removeBlockAccessor(int index) {
        if (index >= 0 && index < block_accessors.size()) {
            block_accessors.remove(index);
            setChanged();
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
        this.lvl = tag.getInt("lvl");
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
        lvl = tag.getInt("lvl");
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
        tag.putInt("lvl", lvl);
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
        tag.putInt("lvl", lvl);
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
        return new MagicStorageMenu(id, inventory, null, new ContainerData() {
            public int get(int id) { return 0; }
            public void set(int id, int value) {}
            public int getCount() { return 0; }
        });
    }

    // ========================================================================
    // Fake entity support
    // ========================================================================
    public void setFake(boolean fake) { this.fake = fake; }

    @Override
    public void setChanged() {
        super.setChanged();
        if (fake && (level == null || level.isClientSide())) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    ItemStack stack = Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
                    var levelData = stack.getComponents().get(ModDataComponent.LEVEL_ACCESSOR.get());
                    PacketDistributor.sendToServer(new BlockPosSyncPacket(
                            getBlockPos(),
                            levelData != null ? levelData.key() : level.dimension(),
                            0));
                }
            }, 10);
        }
    }
}
