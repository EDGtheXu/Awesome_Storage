package coffee.awesome_storage.mix_util;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface IPlayer {
    BlockEntity awesomeStorage$getContainer();
    void awesomeStorage$setContainer(BlockEntity containerItems);
}