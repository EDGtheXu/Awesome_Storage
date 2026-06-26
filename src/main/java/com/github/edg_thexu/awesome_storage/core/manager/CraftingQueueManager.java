package com.github.edg_thexu.awesome_storage.core.manager;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.core.network.s2c.QueueSyncPacket;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CraftingQueueManager {

    public static final int BASE_SLOT_COUNT = 1;
    private static final int TICK_INTERVAL = 20;

    private final List<QueueSlot> slots;
    private final Deque<QueuedRecipe> pendingQueue = new ArrayDeque<>();
    private final BlockEntityAccess access;
    private int tickCounter;

    public interface BlockEntityAccess {
        Level getLevel();
        boolean isClientSide();
        ItemOperationManager getItemOps();
        void setChanged();
        List<String> getBlockAccessors();
        void syncItemsToTrackingPlayers();
    }

    public void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new QueueSyncPacket(saveSlots()));
    }

    public static class QueueSlot {
        @Nullable
        private QueuedRecipe current;

        public boolean paused;

        @Nullable
        public QueuedRecipe current() { return current; }

        public boolean isIdle() { return current == null; }

        public void assign(QueuedRecipe recipe) { this.current = recipe; }

        /** @return the finished recipe if done, null otherwise */
        @Nullable
        public QueuedRecipe finishCurrent() {
            QueuedRecipe r = current;
            current = null;
            return r;
        }

        public void clear() { current = null; }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Paused", paused);
            if (current != null) tag.put("Current", current.save(null));
            return tag;
        }

        public static QueueSlot load(CompoundTag tag) {
            QueueSlot slot = new QueueSlot();
            slot.paused = tag.getBoolean("Paused");
            if (tag.contains("Current")) {
                slot.current = QueuedRecipe.load(tag.getCompound("Current"));
            }
            return slot;
        }
    }

    public CraftingQueueManager(BlockEntityAccess access) {
        this.access = access;
        this.slots = new ArrayList<>();
        for (int i = 0; i < BASE_SLOT_COUNT; i++) {
            slots.add(new QueueSlot());
        }
    }

    public void resizeSlots(int newCount) {
        newCount = Math.max(BASE_SLOT_COUNT, Math.min(newCount, 20));
        while (slots.size() < newCount) {
            slots.add(new QueueSlot());
        }
        while (slots.size() > newCount) {
            QueueSlot removed = slots.remove(slots.size() - 1);
            if (!removed.isIdle()) removed.paused = true;
        }
    }

    public List<QueueSlot> getSlots() { return slots; }

    public Deque<QueuedRecipe> getPendingQueue() { return pendingQueue; }

    public boolean isIdle() {
        for (QueueSlot slot : slots) if (!slot.isIdle()) return false;
        return pendingQueue.isEmpty();
    }

    /** Add a recipe to the pending queue. Merges with the last entry if identical. */
    public int addToQueue(ResourceLocation recipeId, ResourceLocation recipeTypeId, int quantity, int totalCookTime, UUID playerId) {
        QueuedRecipe last = pendingQueue.peekLast();
        if (last != null && last.recipeId.equals(recipeId) && last.recipeTypeId.equals(recipeTypeId)
                && Objects.equals(last.playerId, playerId)) {
            last.quantity += quantity;
        } else {
            pendingQueue.addLast(new QueuedRecipe(recipeId, recipeTypeId, quantity, totalCookTime, playerId));
        }
        access.setChanged();
        return pendingQueue.size();
    }

    public void removeFromPending(int index) {
        int i = 0;
        for (QueuedRecipe r : new ArrayList<>(pendingQueue)) {
            if (i == index) {
                pendingQueue.remove(r);
                access.setChanged();
                return;
            }
            i++;
        }
    }

    public void clearPending() {
        pendingQueue.clear();
        access.setChanged();
    }

    public void clearSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return;
        slots.get(slotIndex).clear();
        access.setChanged();
    }

    public void setPaused(int slotIndex, boolean paused) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return;
        slots.get(slotIndex).paused = paused;
        access.setChanged();
    }

    public void clearAll() {
        pendingQueue.clear();
        for (QueueSlot slot : slots) slot.clear();
        access.setChanged();
    }

    public void tick() {
        if (access.isClientSide()) return;
        tickCounter++;
        if (tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        Level level = access.getLevel();
        if (level == null) return;
        RecipeManager recipeManager = level.getRecipeManager();

        Set<Block> workstations = new HashSet<>();
        for (String id : access.getBlockAccessors()) {
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
            if (b != null) workstations.add(b);
        }

        boolean dirty = false;

        // Process active slots
        for (QueueSlot slot : slots) {
            if (slot.paused || slot.isIdle()) continue;
            QueuedRecipe current = slot.current();
            if (current == null) continue;

            float speed = 1.0f;
            RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(current.recipeTypeId);
            if (recipeType != null) {
                AbstractMagicCraftRecipeAdapter adapter = AdapterManager.Adapters.get(recipeType);
                if (adapter != null) {
                    var optRecipe = recipeManager.byKey(current.recipeId);
                    if (optRecipe.isPresent()) {
                        speed = adapter.getSpeedMultiplier(optRecipe.get(), workstations);
                    }
                }
            }

            current.progress += (int) (TICK_INTERVAL * speed);
            dirty = true;
            if (current.progress >= current.totalCookTime) {
                current.progress = 0;
                if (tryCraft(current, recipeManager)) {
                    access.syncItemsToTrackingPlayers();
                }
                slot.finishCurrent();
            }
        }

        // Assign pending items to idle slots — each slot takes exactly 1 craft
        for (QueueSlot slot : slots) {
            if (slot.paused || !slot.isIdle()) continue;
            if (pendingQueue.isEmpty()) break;
            // Skip pending items that can't be crafted (insufficient ingredients)
            while (!pendingQueue.isEmpty()) {
                QueuedRecipe next = pendingQueue.peekFirst();
                if (next == null) break;
                if (canCraft(next, recipeManager)) {
                    // Take only 1 from the entry
                    slot.assign(new QueuedRecipe(next.recipeId, next.recipeTypeId, 1, next.totalCookTime, next.playerId));
                    next.quantity--;
                    if (next.quantity <= 0) pendingQueue.pollFirst();
                    dirty = true;
                    break;
                } else {
                    pendingQueue.pollFirst(); // skip this item
                    dirty = true;
                }
            }
        }

        if (dirty) access.setChanged();
    }

    private boolean canCraft(QueuedRecipe queued, RecipeManager recipeManager) {
        Level level = access.getLevel();
        if (level == null) return false;
        var optRecipe = recipeManager.byKey(queued.recipeId);
        if (optRecipe.isEmpty()) return false;
        RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(queued.recipeTypeId);
        if (recipeType == null) return false;
        AbstractMagicCraftRecipeAdapter adapter = AdapterManager.Adapters.get(recipeType);
        if (adapter == null) return false;
        @SuppressWarnings("unchecked") NonNullList<Ingredient> ingredients = adapter.getIngredients((RecipeHolder) optRecipe.get());
        return access.getItemOps().hasIngredients(ingredients);
    }

    private boolean tryCraft(QueuedRecipe queued, RecipeManager recipeManager) {
        Level level = access.getLevel();
        if (level == null) return false;
        var optRecipe = recipeManager.byKey(queued.recipeId);
        if (optRecipe.isEmpty()) return false;
        RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(queued.recipeTypeId);
        if (recipeType == null) return false;
        AbstractMagicCraftRecipeAdapter adapter = AdapterManager.Adapters.get(recipeType);
        if (adapter == null) return false;
        RecipeHolder<?> holder = optRecipe.get();
        @SuppressWarnings("unchecked") NonNullList<Ingredient> ingredients = adapter.getIngredients(holder);
        var consumed = access.getItemOps().craftAndConsume(ingredients, Set.of());
        if (consumed == null) return false;
        ItemStack result = adapter.getCraftResult(holder, consumed, level.registryAccess());
        if (result.isEmpty()) return false;
        int remaining = access.getItemOps().storeItem(result);
        if (remaining > 0) {
            for (ItemStack c : consumed) access.getItemOps().storeItem(c);
            return false;
        }
        Player crafter = null;
        if (queued.playerId != null && level instanceof ServerLevel sl) {
            crafter = sl.getPlayerByUUID(queued.playerId);
        }
        adapter.onCraftFinish(holder, consumed, crafter, level);
        return true;
    }

    public CompoundTag saveSlots() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (QueueSlot slot : slots) {
            list.add(slot.save());
        }
        tag.put("Slots", list);
        // Save pending queue
        ListTag pendingList = new ListTag();
        for (QueuedRecipe r : pendingQueue) {
            pendingList.add(r.save(null));
        }
        tag.put("Pending", pendingList);
        return tag;
    }

    public void loadSlots(CompoundTag tag) {
        slots.clear();
        ListTag list = tag.getList("Slots", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            slots.add(QueueSlot.load((CompoundTag) t));
        }
        while (slots.size() < BASE_SLOT_COUNT) {
            slots.add(new QueueSlot());
        }
        // Load pending queue
        pendingQueue.clear();
        if (tag.contains("Pending")) {
            ListTag pendingList = tag.getList("Pending", Tag.TAG_COMPOUND);
            for (Tag t : pendingList) {
                pendingQueue.add(QueuedRecipe.load((CompoundTag) t));
            }
        }
    }
}
