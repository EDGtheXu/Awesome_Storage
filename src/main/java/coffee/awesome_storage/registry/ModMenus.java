package coffee.awesome_storage.registry;

import coffee.awesome_storage.menu.MagicStorageMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static coffee.awesome_storage.Awesome_storage.MODID;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> TYPES = DeferredRegister.create(BuiltInRegistries.MENU, MODID);

    public static final Supplier<MenuType<MagicStorageMenu>> MAGIC_STORAGE_MENU = TYPES.register("magic_storage_menu", () -> new MenuType<>(MagicStorageMenu::new, FeatureFlags.VANILLA_SET));

}
