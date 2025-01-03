package coffee.awesome_storage;

import coffee.awesome_storage.datagen.lang.ModChineseProvider;
import coffee.awesome_storage.datagen.lang.ModEnglishProvider;
import coffee.awesome_storage.registry.ModBlocks;
import coffee.awesome_storage.registry.ModMenus;
import coffee.awesome_storage.registry.ModTabs;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static coffee.awesome_storage.datagen.lang.ModEnglishProvider.toTitleCase;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Awesome_storage.MODID)
public class Awesome_storage {
    public static final String MODID = "awesome_storage";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation space(String path){return ResourceLocation.fromNamespaceAndPath(MODID, path);}

    public static List<Consumer<ModChineseProvider>> chineseProviders = new ArrayList<>();
    public static List<Consumer<ModEnglishProvider>> englishProviders = new ArrayList<>();
    public static void add_zh_en(DeferredItem<Item> item, String zh){
        chineseProviders.add((c)->c.add(item.get(),zh));
        englishProviders.add((c)->c.add(item.get(),toTitleCase(item.getId().getPath())));
    }
    public static <T extends Entity> void add_zh_en(DeferredHolder<EntityType<?>,EntityType<T>> e, String zh){
        chineseProviders.add((c)->c.add(e.get(),zh));
        englishProviders.add((c)->c.add(e.get(),toTitleCase(e.getId().getPath())));
    }

    public Awesome_storage(IEventBus modEventBus, ModContainer modContainer) {

        ModMenus.TYPES.register(modEventBus);

        ModBlocks.BLOCK_ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ENTITIES.register(modEventBus);

        ModTabs.TABS.register(modEventBus);

//        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
//        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);


    }


}
