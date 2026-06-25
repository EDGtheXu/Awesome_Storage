package com.github.edg_thexu.awesome_storage.core.block;

import com.github.edg_thexu.awesome_storage.core.item.QueueUpgradeItem;
import com.github.edg_thexu.awesome_storage.core.item.WirelessNetworkCard;
import com.github.edg_thexu.awesome_storage.core.manager.CraftingQueueManager;
import com.github.edg_thexu.awesome_storage.core.manager.ItemOperationManager;
import com.github.edg_thexu.awesome_storage.core.manager.StorageContainerScanner;
import com.github.edg_thexu.awesome_storage.core.manager.WirelessNetworkManager;
import com.github.edg_thexu.awesome_storage.core.manager.WorkstationManager;
import com.github.edg_thexu.awesome_storage.core.menu.MagicStorageMenu;
import com.github.edg_thexu.awesome_storage.core.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class MagicStorageBlockEntity extends BlockEntity implements MenuProvider, CraftingQueueManager.BlockEntityAccess {

    public Component displayName = Component.empty();

    private final StorageContainerScanner scanner;
    private final ItemOperationManager itemOps;
    private final WorkstationManager workstationMgr;
    private final CraftingQueueManager queueMgr;

    private ItemStack upgradeSlot = ItemStack.EMPTY;
    private int frequency;
    private final ItemStack[] queueUpgradeSlots = new ItemStack[4];

    public MagicStorageBlockEntity(BlockEntityType<MagicStorageBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.scanner = new StorageContainerScanner();
        this.itemOps = new ItemOperationManager(this, scanner);
        this.workstationMgr = new WorkstationManager(this);
        this.queueMgr = new CraftingQueueManager(this);
        for (int i = 0; i < queueUpgradeSlots.length; i++) queueUpgradeSlots[i] = ItemStack.EMPTY;
    }

    public MagicStorageBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlocks.MAGIC_STORAGE_BLOCK_ENTITY.get(), pos, state);
    }

    public CraftingQueueManager getQueueManager() { return queueMgr; }

    // -- Wireless Upgrade --

    public ItemStack getUpgradeSlot() { return upgradeSlot; }

    public void setUpgradeSlot(ItemStack stack) {
        this.upgradeSlot = stack.copy();
        setChanged();
        updateWirelessRegistration();
    }

    public int getFrequency() { return frequency; }

    public void setFrequency(int freq) {
        this.frequency = freq;
        setChanged();
        updateWirelessRegistration();
        this.updateClient();
    }

    private void updateClient() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public int getWirelessRange() {
        if (upgradeSlot.getItem() instanceof WirelessNetworkCard card) {
            return card.getRange();
        }
        return 0;
    }

    private void updateWirelessRegistration() {
        if (level == null || level.isClientSide) return;
        if (upgradeSlot.getItem() instanceof WirelessNetworkCard card && frequency != 0) {
            WirelessNetworkManager.getInstance().register(worldPosition, level.dimension(), frequency, card.getRange());
        } else {
            WirelessNetworkManager.getInstance().unregister(worldPosition);
        }
    }

    // -- Queue Upgrades --

    public ItemStack[] getQueueUpgradeSlots() { return queueUpgradeSlots; }

    public void setQueueUpgradeSlot(int index, ItemStack stack) {
        if (index < 0 || index >= queueUpgradeSlots.length) return;
        queueUpgradeSlots[index] = stack.copy();
        setChanged();
        recalculateQueueSlots();this.updateClient();
    }

    public int getTotalQueueSlots() {
        int total = CraftingQueueManager.BASE_SLOT_COUNT;
        for (ItemStack s : queueUpgradeSlots) {
            if (s.getItem() instanceof QueueUpgradeItem q) {
                total += q.getExtraSlots();
            }
        }
        return Math.min(total, 20); // cap at 20
    }

    private void recalculateQueueSlots() {
        queueMgr.resizeSlots(getTotalQueueSlots());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            updateWirelessRegistration();
            recalculateQueueSlots();
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) {
            WirelessNetworkManager.getInstance().unregister(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            WirelessNetworkManager.getInstance().unregister(worldPosition);
        }
    }

    private int tickCounter;

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicStorageBlockEntity blockEntity) {
        blockEntity.queueMgr.tick();
        blockEntity.tickCounter++;
        if (blockEntity.tickCounter % 5 == 0 && !blockEntity.queueMgr.isIdle()) {
            level.sendBlockUpdated(pos, state, state, 2);
        }
    }

    // ========================================================================
    // CraftingQueueManager.BlockEntityAccess
    // ========================================================================

    @Override public Level getLevel() { return level; }
    @Override public boolean isClientSide() { return level != null && level.isClientSide; }
    @Override public ItemOperationManager getItemOps() { return itemOps; }
    @Override public List<String> getBlockAccessors() { return workstationMgr.getBlockAccessors(); }
    @Override public void syncItemsToTrackingPlayers() {
        if (level == null || level.isClientSide) return;
        for (Player p : level.players()) {
            if (p instanceof ServerPlayer sp && sp.containerMenu instanceof MagicStorageMenu) {
                itemOps.syncToClient(sp);
            }
        }
    }

    // ========================================================================
    // Delegated methods
    // ========================================================================

    public List<Container> getAdjacentContainers() {
        return scanner.getAdjacentContainers(getLevel(), worldPosition);
    }

    // -- Cached Items --

    public void setCachedItems(List<ItemStack> items) {
        itemOps.setCachedItems(items);
    }

    public void setCachedItems(List<ItemStack> items, int usedSlots, int totalSlots) {
        itemOps.setCachedItems(items, usedSlots, totalSlots);
    }

    public List<ItemStack> getStoredItems() {
        return itemOps.getStoredItems();
    }

    public void syncToClient(Player player) {
        itemOps.syncToClient(player);
    }

    public int getTotalItemCount() {
        return itemOps.getTotalItemCount();
    }

    public int getTotalSlots() {
        return itemOps.getTotalSlots();
    }

    public int getUsedSlots() {
        return itemOps.getUsedSlots();
    }

    // -- Item Operations --

    public int storeItem(ItemStack stack) {
        return itemOps.storeItem(stack);
    }

    public ItemStack takeItem(int index) {
        return itemOps.takeItem(index);
    }

    public ItemStack takeItem(ItemStack stack) {
        return itemOps.takeItem(stack);
    }

    public ItemStack takeItem(ItemStack stack, int amount) {
        return itemOps.takeItem(stack, amount);
    }

    public void depositAll(Player player, long favoriteMask) {
        itemOps.depositAll(player, favoriteMask);
    }

    public void quickStack(Player player, long favoriteMask) {
        itemOps.quickStack(player, favoriteMask);
    }

    public void refill(Player player) {
        itemOps.refill(player);
    }

    @Nullable
    public List<ItemStack> craftAndConsume(NonNullList<Ingredient> ingredients, Set<ItemStack> excludedItems) {
        return itemOps.craftAndConsume(ingredients, excludedItems);
    }

    // -- Workstation Management --

    public List<String> getBlock_accessors() { return workstationMgr.getBlockAccessors(); }

    public void addBlockAccessor(String name) {
        workstationMgr.addBlockAccessor(name);
    }

    public void removeBlockAccessor(int index) {
        workstationMgr.removeBlockAccessor(index);
    }

    // ========================================================================
    // Sync & Persistence
    // ========================================================================

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(@NotNull Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.@NotNull Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        workstationMgr.loadFromNbt(tag, lookupProvider);
        if (tag.contains("CraftingQueue")) {
            queueMgr.loadSlots(tag.getCompound("CraftingQueue"));
        }
        if (tag.contains("UpgradeSlot")) {
            upgradeSlot = ItemStack.parseOptional(lookupProvider, tag.getCompound("UpgradeSlot"));
        }
        frequency = tag.getInt("Frequency");
        loadQueueUpgradeSlots(tag, lookupProvider);
        if(tag.contains("DisplayName")) {
            this.displayName = Component.Serializer.fromJson(tag.getString("DisplayName"), lookupProvider);
        }
    }

    private void loadQueueUpgradeSlots(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("QueueUpgradeSlots", 9)) {
            var list = tag.getList("QueueUpgradeSlots", 10);
            for (int i = 0; i < Math.min(list.size(), queueUpgradeSlots.length); i++) {
                queueUpgradeSlots[i] = ItemStack.parseOptional(registries, list.getCompound(i));
            }
        }
    }

    private void saveQueueUpgradeSlots(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new net.minecraft.nbt.ListTag();
        for (ItemStack s : queueUpgradeSlots) {
            list.add(s.saveOptional(registries));
        }
        tag.put("QueueUpgradeSlots", list);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        workstationMgr.loadFromNbt(tag, registries);
        if (tag.contains("CraftingQueue")) {
            queueMgr.loadSlots(tag.getCompound("CraftingQueue"));
        }
        if (tag.contains("UpgradeSlot")) {
            upgradeSlot = ItemStack.parseOptional(registries, tag.getCompound("UpgradeSlot"));
        }
        frequency = tag.getInt("Frequency");
        loadQueueUpgradeSlots(tag, registries);
        if(tag.contains("DisplayName")) {
            this.displayName = Component.Serializer.fromJson(tag.getString("DisplayName"), registries);
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        workstationMgr.saveToNbt(tag);
        tag.put("CraftingQueue", queueMgr.saveSlots());
        if (!upgradeSlot.isEmpty()) {
            tag.put("UpgradeSlot", upgradeSlot.saveOptional(registries));
        }
        tag.putInt("Frequency", frequency);
        saveQueueUpgradeSlots(tag, registries);
        tag.putString("DisplayName", Component.Serializer.toJson(displayName, registries));
        return tag;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        workstationMgr.saveToNbt(tag);
        tag.put("CraftingQueue", queueMgr.saveSlots());
        if (!upgradeSlot.isEmpty()) {
            tag.put("UpgradeSlot", upgradeSlot.saveOptional(registries));
        }
        tag.putInt("Frequency", frequency);
        saveQueueUpgradeSlots(tag, registries);
        tag.putString("DisplayName", Component.Serializer.toJson(displayName, registries));
    }

    @Override
    public @NotNull Component getDisplayName() { return displayName; }

    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        syncToClient(player);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            queueMgr.syncToPlayer(sp);
        }
        return new MagicStorageMenu(id, inventory, new ContainerData() {
            public int get(int id) { return 0; }
            public void set(int id, int value) {}
            public int getCount() { return 0; }
        });
    }
}
