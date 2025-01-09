package coffee.awesome_storage.item;

import coffee.awesome_storage.block.MagicStorageBlockEntity;
import coffee.awesome_storage.mix_util.IPlayer;
import coffee.awesome_storage.network.s2c.ChunkPacket;
import coffee.awesome_storage.registry.ModDataComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class RemoteController extends Item {
    public RemoteController(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        var data1 = stack.get(ModDataComponent.CONTROLLER_RANGE);
        // 必须有范围
        if(data1== null){
            if(!level.isClientSide) player.sendSystemMessage(Component.translatable("magic_storage.message.no_component"+ ModDataComponent.CONTROLLER_RANGE.get()));
            return super.use(level, player, usedHand);
        }

        // 必须有保存的位置
        var data = stack.get(ModDataComponent.SAVED_BLOCK_POS);
        if(data!= null&& !level.isClientSide){
            var levelData = stack.get(ModDataComponent.LEVEL_ACCESSOR);
            BlockEntity entity;

            // 必须有对应的维度
            if(levelData == null){
                return super.use(level, player, usedHand);
//                entity = level.getBlockEntity(data.pos());
            }else{
                var l = player.getServer().getLevel(levelData.key());
                if(l != level && !levelData.on()){
                    player.sendSystemMessage(Component.translatable("magic_storage.message.no_level"));
                    return super.use(level, player, usedHand);
                }
                entity = l.getBlockEntity(data.pos());
            }

            BlockPos pos = data.pos();
            double distance = player.distanceToSqr((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
            if(Math.sqrt(distance) > data1.range() && data1.range() != -1){
                player.sendSystemMessage(Component.translatable("magic_storage.message.too_far"));
            }else {
                if (entity instanceof MagicStorageBlockEntity entity1 ) {
//                Util.setStorageEntity(player, entity1);
                    ((IPlayer) player).awesomeStorage$setContainer(entity1);
                    player.openMenu(entity1, data.pos());
                    LevelChunk chunk = entity1.getLevel().getChunkAt(data.pos());
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new ChunkPacket(chunk, data.pos()));

                }
            }
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        var data = stack.get(ModDataComponent.CONTROLLER_RANGE);
        if(data!= null) {
            tooltipComponents.add(Component.literal(""));
            var data1 = stack.get(ModDataComponent.SAVED_BLOCK_POS);
            if(data1!= null) {
                var levelData = stack.get(ModDataComponent.LEVEL_ACCESSOR);
                if(levelData == null) return;
                ResourceKey<Level> real = Minecraft.getInstance().level.dimension();
                if(real != levelData.key() && !levelData.on()){
                    tooltipComponents.add(Component.translatable("magic_storage.tooltip.error_level"));
                    return;
                }
                String posText = "X: " + data1.pos().getX() + " Y: " + data1.pos().getY() + " Z: " + data1.pos().getZ();
                tooltipComponents.add(Component.translatable("magic_storage.tooltip.block_pos").append(posText));
                BlockPos pos = data1.pos();
                double distance = Minecraft.getInstance().player.distanceToSqr((float) pos.getX(), (float) pos.getY(), (float) pos.getZ());
                Component distanceText = Component.translatable("magic_storage.tooltip.distance").append(" " + (int) Math.sqrt(distance)).withColor(
                        data.range() != -1 && Math.sqrt(distance)-1 > data.range() ? 0xff0000 : 0x00ff00
                );
                tooltipComponents.add(distanceText);
            }
            tooltipComponents.add(Component.literal(""));
            tooltipComponents.add(Component.translatable("magic_storage.tooltip.controller_range").append(data.range() == -1 ? "inf" : String.valueOf(data.range())));



        }

    }



}
