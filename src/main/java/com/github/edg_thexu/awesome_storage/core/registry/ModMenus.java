package com.github.edg_thexu.awesome_storage.core.registry;

import com.github.edg_thexu.awesome_storage.core.menu.MagicStorageMenu;
import com.github.edg_thexu.awesome_storage.core.menu.StorageArrayMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.MODID;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> TYPES = DeferredRegister.create(BuiltInRegistries.MENU, MODID);

    public static final Supplier<MenuType<MagicStorageMenu>> MAGIC_STORAGE_MENU = TYPES.register("magic_storage_menu", () -> new MenuType<>(MagicStorageMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<StorageArrayMenu>> STORAGE_ARRAY_MENU = TYPES.register("storage_array_menu", () -> new MenuType<>(StorageArrayMenu::new, FeatureFlags.VANILLA_SET));
}
