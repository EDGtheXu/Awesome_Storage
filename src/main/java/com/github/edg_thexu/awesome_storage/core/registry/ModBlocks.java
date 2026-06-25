package com.github.edg_thexu.awesome_storage.core.registry;

import com.github.edg_thexu.awesome_storage.AwesomeStorage;
import com.github.edg_thexu.awesome_storage.core.block.*;
import com.mojang.datafixers.DSL;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.add_zh_en;


public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AwesomeStorage.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AwesomeStorage.MODID);
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(AwesomeStorage.MODID);

    public static final Supplier<BaseEntityBlock> STORAGE_CORE_BLOCK = register("storage_core","存储核心", () -> new StorageCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).destroyTime(20).noOcclusion()));

    public static final Supplier<BaseEntityBlock> CRAFTING_UNIT_BLOCK = register("crafting_unit","合成单元", () -> new CraftingUnitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).destroyTime(20).noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagicStorageBlockEntity>> MAGIC_STORAGE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("magic_storage_block_entity", () -> BlockEntityType.Builder.of(MagicStorageBlockEntity::new, CRAFTING_UNIT_BLOCK.get(), STORAGE_CORE_BLOCK.get()).build(DSL.remainderType()));

    // Storage Units (tiered capacities)
    public static final Supplier<BaseEntityBlock> STORAGE_UNIT_BLOCK = registerUnit("storage_unit", "存储单元", 40);
    public static final Supplier<BaseEntityBlock> STORAGE_UNIT_COPPER_BLOCK = registerUnit("storage_unit_copper", "存储单元(铜)", 80);
    public static final Supplier<BaseEntityBlock> STORAGE_UNIT_IRON_BLOCK = registerUnit("storage_unit_iron", "存储单元(铁)", 120);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageUnitBlockEntity>> STORAGE_UNIT_BE =
            BLOCK_ENTITIES.register("storage_unit_be", () -> BlockEntityType.Builder.of(
                    StorageUnitBlockEntity::new,
                    STORAGE_UNIT_BLOCK.get(),
                    STORAGE_UNIT_COPPER_BLOCK.get(),
                    STORAGE_UNIT_IRON_BLOCK.get()
            ).build(DSL.remainderType()));

    // Storage Array
    public static final Supplier<BaseEntityBlock> STORAGE_ARRAY_BLOCK = register("storage_array","存储阵列", () -> new StorageArrayBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).destroyTime(20).noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageArrayBlockEntity>> STORAGE_ARRAY_BE =
            BLOCK_ENTITIES.register("storage_array_be", () -> BlockEntityType.Builder.of(StorageArrayBlockEntity::new, STORAGE_ARRAY_BLOCK.get()).build(DSL.remainderType()));

    public static <T extends Block>Supplier<T> register(String name, String zh, Supplier<T> blockSupplier) {
        DeferredBlock<T> block =  BLOCKS.register(name, blockSupplier);
        DeferredItem<Item> item = BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        add_zh_en(item, zh);
        return block ;
    }

    private static Supplier<BaseEntityBlock> registerUnit(String name, String zh, int capacity) {
        DeferredBlock<BaseEntityBlock> block = BLOCKS.register(name, () -> new StorageUnitBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).destroyTime(20).noOcclusion(),
                capacity
        ));
        DeferredItem<Item> item = BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        add_zh_en(item, zh);
        return block;
    }
}
