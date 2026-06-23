package com.github.edg_thexu.awesome_storage.core.block;

import com.github.edg_thexu.awesome_storage.mix_util.IPlayer;
import com.github.edg_thexu.awesome_storage.core.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class StorageCoreBlock extends BaseEntityBlock {

    public StorageCoreBlock(Properties properties) {
        super(properties);
    }

    public static final MapCodec<StorageCoreBlock> CODEC = simpleCodec(StorageCoreBlock::new);

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker getTicker(@NotNull Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide ? null : createTickerHelper(pBlockEntityType, ModBlocks.MAGIC_STORAGE_BLOCK_ENTITY.get(), MagicStorageBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (state.hasBlockEntity()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MagicStorageBlockEntity magic) {
                ((IPlayer) player).awesomeStorage$setContainer(magic);
                if (!level.isClientSide) {
                    player.openMenu(state.getMenuProvider(level, pos));
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new MagicStorageBlockEntity(pPos, pState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.hasBlockEntity() && !state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MagicStorageBlockEntity be) {
                for (String name : be.getBlock_accessors()) {
                    Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
                    if (b != null) {
                        ItemStack stack = new ItemStack(b);
                        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                        item.setDeltaMovement(level.random.nextGaussian() * 0.1, 0.2, level.random.nextGaussian() * 0.1);
                        level.addFreshEntity(item);
                    }
                }
            }
            level.removeBlockEntity(pos);
        }
    }
}
