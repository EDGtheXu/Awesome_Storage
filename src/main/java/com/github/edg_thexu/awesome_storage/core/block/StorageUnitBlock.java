package com.github.edg_thexu.awesome_storage.core.block;

import com.github.edg_thexu.awesome_storage.core.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class StorageUnitBlock extends BaseEntityBlock {

    private final int capacity;

    public StorageUnitBlock(Properties properties) {
        this(properties, 40);
    }

    public StorageUnitBlock(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public static final MapCodec<StorageUnitBlock> CODEC = simpleCodec(StorageUnitBlock::new);

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public int getSlotCapacity() {
        return capacity;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        StorageUnitBlockEntity be = new StorageUnitBlockEntity(pPos, pState);
        be.setSlotCapacity(capacity);
        return be;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.hasBlockEntity() && !state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof StorageUnitBlockEntity be) {
                for (var entry : be.getStorage()) {
                    ItemStack stack = entry.key().toStack(entry.count());
                    if (!stack.isEmpty()) {
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
