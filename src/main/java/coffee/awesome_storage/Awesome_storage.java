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
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static coffee.awesome_storage.datagen.lang.ModEnglishProvider.toTitleCase;

@SuppressWarnings("removal")
@Mod(Awesome_storage.MODID)
public class Awesome_storage {
    public static final String MODID = "awesome_storage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation space(String path){return new ResourceLocation(MODID, path);}

    public static List<Consumer<ModChineseProvider>> chineseProviders = new ArrayList<>();
    public static List<Consumer<ModEnglishProvider>> englishProviders = new ArrayList<>();
    public static void add_zh_en(Supplier<Item> item, String zh){
        chineseProviders.add((c)->c.add(item.get(),zh));
        englishProviders.add((c)->c.add(item.get(),toTitleCase(ForgeRegistries.ITEMS.getKey(item.get()).getPath())));
    }
    public static <T extends Entity> void add_zh_en(Supplier<EntityType<T>> e, String zh, Void flag){
        chineseProviders.add((c)->c.add(e.get(),zh));
        englishProviders.add((c)->c.add(e.get(),toTitleCase(ForgeRegistries.ENTITY_TYPES.getKey(e.get()).getPath())));
    }

    public Awesome_storage() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModMenus.TYPES.register(modEventBus);

        ModBlocks.BLOCK_ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ENTITIES.register(modEventBus);

        ModTabs.TABS.register(modEventBus);

//        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
//        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);


    }



}
