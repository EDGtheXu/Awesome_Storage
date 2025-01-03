package coffee.awesome_storage.block;

import coffee.awesome_storage.MixinUtil.IPlayer;
import coffee.awesome_storage.config.StorageConfig;
import coffee.awesome_storage.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
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

    public static final MapCodec<MagicStorageBlock> CODEC = simpleCodec(MagicStorageBlock::new);

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }


//    @Override
//    public @Nullable MenuProvider getMenuProvider(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos) {
//        return new SimpleMenuProvider((pContainerId, pPlayerInventory, pPlayer) -> new MagicStorageMenu(pContainerId, pPlayerInventory), CONTAINER_TITLE);
//    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker getTicker(@NotNull Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide ? null : createTickerHelper(pBlockEntityType, ModBlocks.MAGIC_STORAGE_BLOCK_ENTITY.get(), MagicStorageBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(state.hasBlockEntity()){
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if(blockEntity instanceof MagicStorageBlockEntity magic){
                // 存储升级
                int lvl = magic.lvl;
                if(StorageConfig.getUpgradeLine().containsKey(lvl+1)){
                    var upgrade = StorageConfig.getUpgradeLine().get(lvl+1);
                    if(upgrade.material().is(stack.getItem())&& stack.getCount()>=upgrade.material().getCount()){
                        magic.addContainerSize(upgrade.extend());
                        magic.lvl++;
                        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                        if(level.isClientSide)
                            player.sendSystemMessage(Component.literal("Upgrade added!"));
                        stack.shrink(upgrade.material().getCount());
                        return ItemInteractionResult.SUCCESS;
                    }
                }
                // 合成升级



                ((IPlayer)player).awesomeStorage$setContainer(magic);
                if(!level.isClientSide){
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
            MagicStorageBlockEntity blockEntity = (MagicStorageBlockEntity) level.getBlockEntity(pos);
            if (blockEntity != null) {
                Containers.dropContentsOnDestroy(state, newState, level, pos);
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
