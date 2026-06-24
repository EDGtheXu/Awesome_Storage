package com.github.edg_thexu.awesome_storage.core.manager;

import com.github.edg_thexu.awesome_storage.compat.sophisticated.SophisticatedHelper;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StorageContainerScanner {

    public List<Container> getAdjacentContainers(Level level, BlockPos worldPosition) {
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
                    case null -> {}
                    case MagicStorageBlockEntity ignored ->
                        queue.add(n);
                    case Container c -> {
                        containers.add(c);
                        queue.add(n);
                    }
                    default -> {
                        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, n, dir.getOpposite());
                        if (handler != null) {
                            ItemHandlerContainer ihc = new ItemHandlerContainer(handler);
                            if (SophisticatedHelper.isStorageLoaded() && be instanceof ChestBlockEntity chestBlockEntity) {
                                if (chestBlockEntity.isMainChest()) {
                                    containers.add(ihc);
                                }
                            } else {
                                containers.add(ihc);
                            }
                            queue.add(n);
                        }
                    }
                }
            }
        }
        return containers;
    }

    public static class ItemHandlerContainer implements Container {
        final IItemHandler handler;
        public ItemHandlerContainer(IItemHandler handler) { this.handler = handler; }
        @Override public int getContainerSize() { return handler.getSlots(); }
        @Override public boolean isEmpty() { for (int i = 0; i < handler.getSlots(); i++) if (!handler.getStackInSlot(i).isEmpty()) return false; return true; }
        @Override public @NotNull ItemStack getItem(int slot) { return handler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack removeItem(int slot, int amount) { return handler.extractItem(slot, amount, false); }
        @Override public @NotNull ItemStack removeItemNoUpdate(int slot) { return handler.extractItem(slot, handler.getStackInSlot(slot).getCount(), false); }
        @Override public void setItem(int slot, @NotNull ItemStack stack) {
            handler.extractItem(slot, handler.getStackInSlot(slot).getCount(), false);
            handler.insertItem(slot, stack, false);
        }
        @Override public void setChanged() {}
        @Override public boolean stillValid(@NotNull Player player) { return true; }
        @Override public void clearContent() { for (int i = 0; i < handler.getSlots(); i++) handler.extractItem(i, handler.getStackInSlot(i).getCount(), false); }
        @Override public int getMaxStackSize() { return 64; }
    }
}
