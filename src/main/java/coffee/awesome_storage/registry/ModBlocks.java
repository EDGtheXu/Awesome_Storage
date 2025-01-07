package coffee.awesome_storage.registry;

import coffee.awesome_storage.Awesome_storage;
import coffee.awesome_storage.block.MagicStorageBlock;
import coffee.awesome_storage.block.MagicStorageBlockEntity;
import com.mojang.datafixers.DSL;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

import static coffee.awesome_storage.Awesome_storage.add_zh_en;


public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,Awesome_storage.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Awesome_storage.MODID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Awesome_storage.MODID);

    public static final Supplier<BaseEntityBlock> MAGIC_STORAGE_BLOCK = register("magic_storage_block","魔法存储块", () -> new MagicStorageBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).destroyTime(20).noOcclusion()));
    public static final Supplier<BlockEntityType<MagicStorageBlockEntity>> MAGIC_STORAGE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("magic_storage_block_entity", () -> BlockEntityType.Builder.of(MagicStorageBlockEntity::new, MAGIC_STORAGE_BLOCK.get()).build(DSL.remainderType()));

    public static <T extends Block>Supplier<T> register(String name, String zh, Supplier<T> blockSupplier) {
        Supplier<T> block =  BLOCKS.register(name, blockSupplier);
        Supplier<Item> item = BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        add_zh_en(item, zh);
        return block ;
    }
}
