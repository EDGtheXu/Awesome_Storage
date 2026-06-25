package com.github.edg_thexu.awesome_storage.utils;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.MODID;

public class AutoStockSystem {

    private final Map<String, Integer> targets = new HashMap<>();

    private static AutoStockSystem instance;

    public static AutoStockSystem getInstance() {
        if (instance == null) instance = new AutoStockSystem();
        return instance;
    }

    private static File getStockFile() {
        return new File(FMLPaths.CONFIGDIR.get().toFile(), MODID + "/auto_stock.dat");
    }

    public void load() {
        targets.clear();
        try {
            File file = getStockFile();
            if (file.exists()) {
                for (String line : Files.readAllLines(file.toPath())) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    int sep = line.lastIndexOf('=');
                    if (sep > 0 && sep < line.length() - 1) {
                        String id = line.substring(0, sep);
                        int count = Integer.parseInt(line.substring(sep + 1));
                        if (count > 0) targets.put(id, count);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void save() {
        try {
            File file = getStockFile();
            file.getParentFile().mkdirs();
            StringBuilder sb = new StringBuilder();
            for (var e : targets.entrySet()) {
                sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
            }
            Files.write(file.toPath(), sb.toString().getBytes());
        } catch (Exception ignored) {
        }
    }

    public int getTarget(ItemStack stack) {
        return targets.getOrDefault(stack.getItemHolder().getRegisteredName(), 0);
    }

    public void setTarget(ItemStack stack, int count) {
        if (count <= 0) {
            targets.remove(stack.getItemHolder().getRegisteredName());
        } else {
            targets.put(stack.getItemHolder().getRegisteredName(), count);
        }
        save();
    }

    public void removeTarget(ItemStack stack) {
        targets.remove(stack.getItemHolder().getRegisteredName());
        save();
    }

    public Map<String, Integer> getAllTargets() {
        return Map.copyOf(targets);
    }

    public boolean hasTarget(ItemStack stack) {
        return targets.containsKey(stack.getItemHolder().getRegisteredName());
    }
}
