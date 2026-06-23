package com.github.edg_thexu.awesome_storage.core.block;

import com.github.edg_thexu.awesome_storage.core.data_component.BlockPosComponent;
import com.github.edg_thexu.awesome_storage.core.data_component.LevelAccessorComponent;
import com.github.edg_thexu.awesome_storage.core.item.RemoteController;
import com.github.edg_thexu.awesome_storage.mix_util.IPlayer;
import com.github.edg_thexu.awesome_storage.core.registry.ModBlocks;
import com.github.edg_thexu.awesome_storage.core.registry.ModDataComponent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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

public class CraftingUnitBlock extends BaseEntityBlock {

    public CraftingUnitBlock(Properties properties) {
        super(properties);
    }

    public static final MapCodec<CraftingUnitBlock> CODEC = simpleCodec(CraftingUnitBlock::new);

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
                // 保存位置
                if(stack.getItem() instanceof RemoteController){
                    var data = stack.getComponents().get(ModDataComponent.CONTROLLER_RANGE.get());
                    if(data!=null) {
                        stack.set(ModDataComponent.SAVED_BLOCK_POS, new BlockPosComponent(magic.getBlockPos()));

                        // 保存维度
                        if(!level.isClientSide){
                            var levelData = stack.getComponents().get(ModDataComponent.LEVEL_ACCESSOR.get());
                            if(levelData!=null && levelData.on()) {
                                stack.set(ModDataComponent.LEVEL_ACCESSOR, new LevelAccessorComponent(level.dimension(), true));
                            }
                            else{
                                stack.set(ModDataComponent.LEVEL_ACCESSOR, new LevelAccessorComponent(level.dimension(), false));
                            }
                        }

                    }
                    else
                        player.sendSystemMessage(Component.translatable("magic_storage.message.no_component"+ ModDataComponent.CONTROLLER_RANGE.get()));
                    return ItemInteractionResult.SUCCESS;
                }
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
                // Drop all work station blocks
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
