package coffee.awesome_storage.datagen;

import coffee.awesome_storage.datagen.lang.ModChineseProvider;
import coffee.awesome_storage.datagen.lang.ModEnglishProvider;
import coffee.awesome_storage.datagen.loot.ModLootTableProvider;
import coffee.awesome_storage.datagen.recipe.ModRecipeProvider;
import coffee.awesome_storage.datagen.tag.ModBlockTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;


import java.util.concurrent.CompletableFuture;

import static coffee.awesome_storage.Awesome_storage.MODID;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        net.minecraft.data.DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        boolean server = event.includeServer();
        ModBlockTagsProvider blockTagsProvider = new ModBlockTagsProvider(output, lookup, helper);
        generator.addProvider(server, blockTagsProvider);
        generator.addProvider(server, ModLootTableProvider.getProvider(output, lookup));
        generator.addProvider(server, new ModRecipeProvider(output, lookup));


        boolean client = event.includeClient();
        generator.addProvider(client, new ModChineseProvider(output));
        generator.addProvider(client, new ModEnglishProvider(output));

    }
}
