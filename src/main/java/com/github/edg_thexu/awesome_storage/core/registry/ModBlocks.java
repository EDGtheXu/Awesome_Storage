package com.github.edg_thexu.awesome_storage.core.registry;

import com.github.edg_thexu.awesome_storage.AwesomeStorage;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlock;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.block.StorageOnlyBlock;
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

    public static final Supplier<BaseEntityBlock> STORAGE_BLOCK = register("storage_block","存储块", () -> new StorageOnlyBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).destroyTime(20).noOcclusion()));

    public static final Supplier<BaseEntityBlock> MAGIC_STORAGE_BLOCK = register("magic_storage_block","魔法存储块", () -> new MagicStorageBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).destroyTime(20).noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagicStorageBlockEntity>> MAGIC_STORAGE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("magic_storage_block_entity", () -> BlockEntityType.Builder.of(MagicStorageBlockEntity::new, MAGIC_STORAGE_BLOCK.get(), STORAGE_BLOCK.get()).build(DSL.remainderType()));

    public static <T extends Block>Supplier<T> register(String name, String zh, Supplier<T> blockSupplier) {
        DeferredBlock<T> block =  BLOCKS.register(name, blockSupplier);
        DeferredItem<Item> item = BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        add_zh_en(item, zh);
        return block ;
    }
}
