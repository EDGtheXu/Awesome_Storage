package com.github.edg_thexu.awesome_storage.mixin;

import com.github.edg_thexu.awesome_storage.mix_util.IPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public class PlayerMixin implements IPlayer {
    @Unique
    private BlockEntity awesomeStorage$container;

    @Override
    public BlockEntity awesomeStorage$getContainer() {
        return awesomeStorage$container;
    }

    @Override
    public void awesomeStorage$setContainer(BlockEntity containerItems) {
        this.awesomeStorage$container = containerItems;
    }
}
