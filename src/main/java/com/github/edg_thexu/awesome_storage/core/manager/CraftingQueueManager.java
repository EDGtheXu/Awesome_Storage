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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CraftingQueueManager {

    public static final int DEFAULT_SLOT_COUNT = 6;
    private static final int TICK_INTERVAL = 20;

    private final List<QueueSlot> slots;
    private final BlockEntityAccess access;
    private int tickCounter;

    public interface BlockEntityAccess {
        Level getLevel();
        boolean isClientSide();
        ItemOperationManager getItemOps();
        void setChanged();
        List<String> getBlockAccessors();
    }

    public static class QueueSlot {
        public final Deque<QueuedRecipe> queue = new ArrayDeque<>();
        public boolean paused;

        @Nullable
        public QueuedRecipe current() {
            return queue.peekFirst();
        }

        public void add(QueuedRecipe recipe) {
            queue.addLast(recipe);
        }

        public boolean isIdle() {
            return queue.isEmpty();
        }

        public void clear() {
            queue.clear();
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Paused", paused);
            ListTag list = new ListTag();
            for (QueuedRecipe r : queue) {
                list.add(r.save(null));
            }
            tag.put("Queue", list);
            return tag;
        }

        public static QueueSlot load(CompoundTag tag) {
            QueueSlot slot = new QueueSlot();
            slot.paused = tag.getBoolean("Paused");
            ListTag list = tag.getList("Queue", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                slot.queue.add(QueuedRecipe.load((CompoundTag) t));
            }
            return slot;
        }
    }

    public CraftingQueueManager(BlockEntityAccess access) {
        this.access = access;
        this.slots = new ArrayList<>();
        for (int i = 0; i < DEFAULT_SLOT_COUNT; i++) {
            slots.add(new QueueSlot());
        }
    }

    public List<QueueSlot> getSlots() {
        return slots;
    }

    public boolean isIdle() {
        for (QueueSlot slot : slots) {
            if (!slot.isIdle()) return false;
        }
        return true;
    }

    public int addToQueue(ResourceLocation recipeId, ResourceLocation recipeTypeId, int quantity, int totalCookTime) {
        for (QueueSlot slot : slots) {
            if (slot.isIdle() || (!slot.paused && slot.queue.size() < 16)) {
                slot.add(new QueuedRecipe(recipeId, recipeTypeId, quantity, totalCookTime));
                access.setChanged();
                return slots.indexOf(slot);
            }
        }
        return -1;
    }

    public void removeFromSlot(int slotIndex, int entryIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return;
        QueueSlot slot = slots.get(slotIndex);
        int i = 0;
        for (QueuedRecipe r : new ArrayList<>(slot.queue)) {
            if (i == entryIndex) {
                slot.queue.remove(r);
                access.setChanged();
                return;
            }
            i++;
        }
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

    public void tick() {
        if (access.isClientSide()) return;
        tickCounter++;
        if (tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        Level level = access.getLevel();
        if (level == null) return;
        RecipeManager recipeManager = level.getRecipeManager();

        // Resolve workstation blocks from accessor IDs
        Set<Block> workstations = new HashSet<>();
        for (String id : access.getBlockAccessors()) {
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
            if (b != null) workstations.add(b);
        }

        boolean dirty = false;
        for (QueueSlot slot : slots) {
            if (slot.paused || slot.isIdle()) continue;
            QueuedRecipe current = slot.current();
            if (current == null) continue;

            // Compute speed multiplier for this recipe based on workstations
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
                    current.quantity--;
                    if (current.isDone()) {
                        slot.queue.pollFirst();
                    }
                } else {
                    slot.paused = true;
                }
            }
        }
        if (dirty) {
            access.setChanged();
        }
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
        return true;
    }

    public void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new QueueSyncPacket(saveSlots()));
    }

    private void syncToAllPlayers() {
        // lazy: just mark dirty; the client reads via ContainerData or separate sync
    }

    public CompoundTag saveSlots() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (QueueSlot slot : slots) {
            list.add(slot.save());
        }
        tag.put("Slots", list);
        return tag;
    }

    public void loadSlots(CompoundTag tag) {
        slots.clear();
        ListTag list = tag.getList("Slots", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            slots.add(QueueSlot.load((CompoundTag) t));
        }
        while (slots.size() < DEFAULT_SLOT_COUNT) {
            slots.add(new QueueSlot());
        }
    }
}
