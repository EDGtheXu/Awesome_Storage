package coffee.awesome_storage.block;

import coffee.awesome_storage.config.StorageConfig;
import coffee.awesome_storage.mix_util.IPlayer;
import coffee.awesome_storage.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

public class MagicStorageBlock extends BaseEntityBlock  {

    public MagicStorageBlock(Properties properties) {
        super(properties);
    }


    @Override
    public <T extends BlockEntity> BlockEntityTicker getTicker(@NotNull Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide ? null : createTickerHelper(pBlockEntityType, ModBlocks.MAGIC_STORAGE_BLOCK_ENTITY.get(), MagicStorageBlockEntity::serverTick);
    }


    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand pHand, BlockHitResult pHit) {
        if(state.hasBlockEntity()){
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if(blockEntity instanceof MagicStorageBlockEntity magic){
                // 存储升级      public InteractionResult use(Level p_60665_, Player p_60666_, InteractionHand p_60667_, BlockHitResult p_60668_) {
                //         return this.getBlock().use(this.asState(), p_60665_, p_60668_.getBlockPos(), p_60666_, p_60667_, p_60668_);
                //      }
                int lvl = magic.lvl;
                if(StorageConfig.getUpgradeLine().containsKey(lvl+1)){
                    var upgrade = StorageConfig.getUpgradeLine().get(lvl+1);
                    ItemStack stack = player.getItemInHand(pHand);
                    if(upgrade.material().is(stack.getItem())&& stack.getCount()>=upgrade.material().getCount()){
                        magic.addContainerSize(upgrade.extend());
                        magic.lvl++;
                        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                        if(level.isClientSide)
                            player.sendSystemMessage(Component.literal("Upgrade added!"));
                        stack.shrink(upgrade.material().getCount());
                        return InteractionResult.SUCCESS;
                    }
                }
                // 合成升级



                ((IPlayer)player).awesomeStorage$setContainer(magic);
                if(!level.isClientSide){
                    player.openMenu(state.getMenuProvider(level, pos));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;

    }


    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new MagicStorageBlockEntity(pPos, pState);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {

        if (state.hasBlockEntity() && !state.is(newState.getBlock())) {
            MagicStorageBlockEntity blockEntity = (MagicStorageBlockEntity) level.getBlockEntity(pos);
            if (blockEntity != null) {
                Containers.dropContents(level, pos,blockEntity);
                int lvl = blockEntity.lvl;

                for(int i=1;i<=lvl;i++){
                    if(!StorageConfig.getUpgradeLine().containsKey(i))
                        continue;
                    var upgrade = StorageConfig.getUpgradeLine().get(i);
                    ItemStack stack = upgrade.material().copy();
                    ItemEntity itemEntity = new ItemEntity(level, pos.getX()+level.random.nextFloat()*0.5, pos.getY()+0.1, pos.getZ()+level.random.nextFloat()*0.5, stack);
                    itemEntity.setDeltaMovement(0, 0.1, 0);
                    level.addFreshEntity(itemEntity);

                }


            }
            level.removeBlockEntity(pos);
        }

    }
}
