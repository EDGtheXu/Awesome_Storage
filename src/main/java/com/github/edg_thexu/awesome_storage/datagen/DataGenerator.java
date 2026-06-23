package com.github.edg_thexu.awesome_storage.datagen;

import com.github.edg_thexu.awesome_storage.datagen.lang.ModChineseProvider;
import com.github.edg_thexu.awesome_storage.datagen.lang.ModEnglishProvider;
import com.github.edg_thexu.awesome_storage.datagen.loot.ModLootTableProvider;
import com.github.edg_thexu.awesome_storage.datagen.recipe.ModRecipeProvider;
import com.github.edg_thexu.awesome_storage.datagen.tag.ModBlockTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;


import java.util.concurrent.CompletableFuture;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.MODID;

@EventBusSubscriber(modid = MODID)
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
        generator.addProvider(client, new ModItemModelProvider(output, helper));

    }
}
