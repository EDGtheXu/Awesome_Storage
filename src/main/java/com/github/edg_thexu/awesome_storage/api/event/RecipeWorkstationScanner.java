package com.github.edg_thexu.awesome_storage.api.event;

import com.github.edg_thexu.awesome_storage.AwesomeStorage;
import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.api.adapter.CommonRecipeAdapter;
import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import net.neoforged.fml.ModList;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Scans ALL classes on the classpath for {@link RecipeWorkstation} annotations
 * and registers block↔recipe mappings + adapters automatically — like JeiPlugin scanning.
 * <p>
 * Other mods only need to annotate any class in their mod with {@code @RecipeWorkstation}.
 * No service files or manual registration needed.
 * <pre>
 * &#64;RecipeWorkstation(blocks = {"minecraft:furnace", "minecraft:blast_furnace"}, recipeTypes = "minecraft:smelting")
 * public class MyWorkstations {}
 * </pre>
 */
public class RecipeWorkstationScanner {

    /** Scans ALL classes on the classpath for @RecipeWorkstation annotations. */
    public static void scanAll() {
        Set<Path> scanned = new HashSet<>();

        // 1. Scan directories on classpath (dev environment)
        try {
            String cp = System.getProperty("java.class.path");
            if (cp != null) {
                for (String entry : cp.split(File.pathSeparator)) {
                    Path p = Paths.get(entry);
                    if (Files.isDirectory(p) && scanned.add(p)) {
                        scanDirectory(p, p, scanned);
                    }
                }
            }
        } catch (Exception e) {
            AwesomeStorage.LOGGER.warn("[RecipeWorkstation] Classpath scan error: " + e.getMessage());
        }

        // 2. Scan all mod jars (production + dev)
        try {
            ModList.get().getModFiles().forEach(modFile -> {
                try {
                    Path jarPath = modFile.getFile().getFilePath();
                    if (jarPath.toString().endsWith(".jar") && scanned.add(jarPath)) {
                        scanJarFile(jarPath);
                    }
                } catch (Exception e) {
                    AwesomeStorage.LOGGER.warn("[RecipeWorkstation] Could not scan mod file: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            AwesomeStorage.LOGGER.warn("[RecipeWorkstation] ModList scan error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Directory scanning (dev environment)
    // ========================================================================
    private static void scanDirectory(Path root, Path dir, Set<Path> scanned) {
        try (Stream<Path> stream = Files.walk(dir, Integer.MAX_VALUE)) {
            stream.filter(p -> p.toString().endsWith(".class")
                    && !p.toString().contains("$")
                    && !p.toString().contains("package-info")
                    && !p.toString().contains("module-info"))
                  .forEach(p -> processClassFile(root, p));
        } catch (Exception e) { /* skip unreadable */ }
    }

    private static void processClassFile(Path root, Path file) {
        String relative = root.relativize(file).toString();
        String className = relative.replace(File.separatorChar, '.').replace(".class", "");
        processClassName(className);
    }

    // ========================================================================
    // JAR scanning (production + dev)
    // ========================================================================
    private static void scanJarFile(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class") && !name.contains("$")
                        && !name.contains("package-info") && !name.contains("module-info")
                        && !name.startsWith("META-INF")) {
                    String className = name.replace('/', '.').replace(".class", "");
                    processClassName(className);
                }
            }
        } catch (Exception e) { /* skip corrupted jars */ }
    }

    // ========================================================================
    // Class loading & processing
    // ========================================================================
    private static void processClassName(String className) {
        // Skip known problematic packages
        if (className.startsWith("net.neoforged.") || className.startsWith("org.spongepowered.")
                || className.startsWith("cpw.mods.") || className.startsWith("com.mojang.")
                || className.startsWith("META-INF.") || className.startsWith("it.unimi.")
                || className.startsWith("org.slf4j.") || className.startsWith("org.apache.")
                || className.startsWith("com.google.") || className.startsWith("com.ibm.")
                || className.startsWith("io.netty.") || className.startsWith("org.lwjgl.")
                || className.startsWith("com.github.edg_thexu.qtcraft_api")
                || className.startsWith("jdk.") || className.startsWith("javax.")
                || className.startsWith("sun.") || className.startsWith("com.sun.")) {
            return;
        }
        try {
            Class<?> cls = Class.forName(className);
            if (!cls.isInterface() && !java.lang.reflect.Modifier.isAbstract(cls.getModifiers())) {
                RecipeWorkstation[] anns = cls.getAnnotationsByType(RecipeWorkstation.class);
                if (anns != null && anns.length > 0) {
                    for (RecipeWorkstation ann : anns) {
                        processAnnotation(ann);
                    }
                }
            }
        } catch (Exception e) {
            // skip any class that can't be loaded
        } catch (Throwable ignored) {

        }
    }

    private static void processAnnotation(RecipeWorkstation ann) {
        String[] blockIds = ann.blocks();
        for (String typeId : ann.recipeTypes()) {
            RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.parse(typeId));
            if (type == null) {
                AwesomeStorage.LOGGER.warn("[RecipeWorkstation] Unknown recipe type: " + typeId);
                continue;
            }

            try {
                AbstractMagicCraftRecipeAdapter adapter;
                if (ann.adapter() == CommonRecipeAdapter.class) {
                    adapter = new CommonRecipeAdapter(type);
                } else {
                    adapter = ann.adapter().getDeclaredConstructor().newInstance();
                    for(Constructor<?> c :ann.adapter().getConstructors()) {
                        if(c.getParameterCount() == 0) {
                            adapter = (AbstractMagicCraftRecipeAdapter) c.newInstance();
                            break;
                        }else if(c.getParameterCount() == 1) {
                            if(c.getParameterTypes()[0] == RecipeType.class) {
                                adapter = (AbstractMagicCraftRecipeAdapter) c.newInstance(type);
                            } else if(c.getParameterTypes()[0] == String.class) {
                                adapter = (AbstractMagicCraftRecipeAdapter) c.newInstance(typeId);
                            }
                            break;
                        }
                    }
                }
                CraftConfig.registerRecipeBlocks(typeId, blockIds);
                AdapterManager.registerAdapters(type, adapter);
            } catch (Exception e) {
                AwesomeStorage.LOGGER.error("[RecipeWorkstation] Failed to create adapter for " + typeId, e);
            }
        }
    }

}
