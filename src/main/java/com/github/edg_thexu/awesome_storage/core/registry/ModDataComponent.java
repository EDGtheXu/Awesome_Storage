package com.github.edg_thexu.awesome_storage.core.registry;

import com.github.edg_thexu.awesome_storage.core.data_component.BlockPosComponent;
import com.github.edg_thexu.awesome_storage.core.data_component.LevelAccessorComponent;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.MODID;

public class ModDataComponent {
    public static final DeferredRegister<DataComponentType<?>> TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<BlockPosComponent>> SAVED_BLOCK_POS =
            TYPES.register("block_pos", () -> DataComponentType.<BlockPosComponent>builder().persistent(BlockPosComponent.CODEC).networkSynchronized(BlockPosComponent.STREAM_CODEC).cacheEncoding().build());

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> CONTROLLER_RANGE =
            TYPES.register("range", () -> DataComponentType.<Integer>builder().persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).cacheEncoding().build());

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<LevelAccessorComponent>> LEVEL_ACCESSOR =
            TYPES.register("level", () -> DataComponentType.<LevelAccessorComponent>builder().persistent(LevelAccessorComponent.CODEC).networkSynchronized(LevelAccessorComponent.STREAM_CODEC).cacheEncoding().build());

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> STORAGE_SIZE =
            TYPES.register("storage_size", () -> DataComponentType.<Integer>builder().persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

}
