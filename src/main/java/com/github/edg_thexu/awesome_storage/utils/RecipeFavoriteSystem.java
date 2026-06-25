package com.github.edg_thexu.awesome_storage.utils;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.MODID;

public class RecipeFavoriteSystem {

    private final Set<String> favoriteRecipeIds = new HashSet<>();

    private static RecipeFavoriteSystem instance;

    public static RecipeFavoriteSystem getInstance() {
        if (instance == null) instance = new RecipeFavoriteSystem();
        return instance;
    }

    private static File getFavoritesFile() {
        return new File(FMLPaths.CONFIGDIR.get().toFile(), MODID + "/recipe_favorites.dat");
    }

    public void loadFavorites() {
        favoriteRecipeIds.clear();
        try {
            File file = getFavoritesFile();
            if (file.exists()) {
                for (String line : Files.readAllLines(file.toPath())) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        favoriteRecipeIds.add(line);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void saveFavorites() {
        try {
            File file = getFavoritesFile();
            file.getParentFile().mkdirs();
            StringBuilder sb = new StringBuilder();
            for (String id : favoriteRecipeIds) {
                sb.append(id).append('\n');
            }
            Files.write(file.toPath(), sb.toString().getBytes());
        } catch (Exception ignored) {
        }
    }

    public boolean isFavorited(ResourceLocation recipeId) {
        return favoriteRecipeIds.contains(recipeId.toString());
    }

    public void toggleFavorite(ResourceLocation recipeId) {
        String id = recipeId.toString();
        if (!favoriteRecipeIds.remove(id)) {
            favoriteRecipeIds.add(id);
        }
        saveFavorites();
    }

    public boolean isEmpty() {
        return favoriteRecipeIds.isEmpty();
    }
}
