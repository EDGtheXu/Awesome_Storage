package coffee.awesome_storage.datagen.loot;

import coffee.awesome_storage.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.stream.Collectors;

public class ModBlockLootProvider extends BlockLootSubProvider {


    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(),registries);
    }

    @Override
    protected void generate() {
        getKnownBlocks().forEach(this::dropSelf);

    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(entry -> entry.get()).collect(Collectors.toList());
    }
}