package coffee.awesome_storage.registry;

import coffee.awesome_storage.dataComponent.BlockPosComponent;
import coffee.awesome_storage.dataComponent.LevelAccessorComponent;
import coffee.awesome_storage.dataComponent.RangeComponent;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static coffee.awesome_storage.Awesome_storage.MODID;

public class ModDataComponent {
    public static final DeferredRegister<DataComponentType<?>> TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<BlockPosComponent>> SAVED_BLOCK_POS =
            TYPES.register("block_pos", () -> DataComponentType.<BlockPosComponent>builder().persistent(BlockPosComponent.CODEC).networkSynchronized(BlockPosComponent.STREAM_CODEC).cacheEncoding().build());

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<RangeComponent>> CONTROLLER_RANGE =
            TYPES.register("range", () -> DataComponentType.<RangeComponent>builder().persistent(RangeComponent.CODEC).networkSynchronized(RangeComponent.STREAM_CODEC).cacheEncoding().build());

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<LevelAccessorComponent>> LEVEL_ACCESSOR =
            TYPES.register("level", () -> DataComponentType.<LevelAccessorComponent>builder().persistent(LevelAccessorComponent.CODEC).networkSynchronized(LevelAccessorComponent.STREAM_CODEC).cacheEncoding().build());



}
