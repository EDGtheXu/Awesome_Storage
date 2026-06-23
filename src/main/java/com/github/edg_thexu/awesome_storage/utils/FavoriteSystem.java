package com.github.edg_thexu.awesome_storage.utils;

import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicStoragePacket;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.MODID;

public class FavoriteSystem {

    private final Set<Integer> favoriteSlots = new HashSet<>();
    public boolean clickedSlotWasFav = false;

    private static FavoriteSystem instance;

    public static FavoriteSystem getInstance() {
        if (instance == null) instance = new FavoriteSystem();
        return instance;
    }

    private static File getFavoritesFile() {
        return new File(FMLPaths.CONFIGDIR.get().toFile(), MODID + "/favorites.dat");
    }

    public void loadFavorites() {
        favoriteSlots.clear();
        try {
            File file = getFavoritesFile();
            if (file.exists()) {
                long mask = Long.parseLong(new String(Files.readAllBytes(file.toPath())).trim());
                for (int i = 0; i < 36; i++) {
                    if ((mask & (1L << i)) != 0) favoriteSlots.add(i);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void syncToServer() {
        long mask = 0;
        for (int slot : favoriteSlots) {
            mask |= (1L << slot);
        }
        PacketDistributor.sendToServer(new MagicStoragePacket(5, ItemStack.EMPTY, mask));
    }

    public void saveFavorites() {
        try {
            long mask = 0;
            for (int slot : favoriteSlots) {
                mask |= (1L << slot);
            }
            PacketDistributor.sendToServer(new MagicStoragePacket(5, ItemStack.EMPTY, mask));
            File file = getFavoritesFile();
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), String.valueOf(mask).getBytes());
        } catch (Exception ignored) {
        }
    }

    public long getFavoriteMask() {
        long mask = 0;
        for (int slot : favoriteSlots) {
            mask |= (1L << slot);
        }
        return mask;
    }

    public boolean isFavorited(int slotIndex) {
        return favoriteSlots.contains(slotIndex);
    }

    public void toggleFavorite(int slotIndex) {
        if (!favoriteSlots.remove(slotIndex)) {
            favoriteSlots.add(slotIndex);
        }
    }

    public boolean isEmpty() {
        return favoriteSlots.isEmpty();
    }

    public void exchange(int idx, boolean valid) {
        boolean exchange = clickedSlotWasFav;
        clickedSlotWasFav = favoriteSlots.contains(idx) && valid;
        if (exchange) {
            favoriteSlots.add(idx);
        } else {
            favoriteSlots.remove(idx);
        }
    }


    public void validate(NonNullList<ItemStack> items) {
        for(int i=0; i< 36; i++) {
            if(items.get( i).isEmpty()) {
                favoriteSlots.remove(i);
            }
        }
    }
}
