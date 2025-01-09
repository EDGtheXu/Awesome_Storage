package coffee.awesome_storage.dataComponent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;


public record BlockPosComponent(BlockPos pos) implements DataComponentType<BlockPosComponent> {

    public static final Codec<BlockPosComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(BlockPosComponent::pos)
    ).apply(instance, BlockPosComponent::new));

    public static final StreamCodec<ByteBuf, BlockPosComponent> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BlockPosComponent::pos,
            BlockPosComponent::new
    );

    @Override
    public @Nullable Codec<BlockPosComponent> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, BlockPosComponent> streamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof BlockPosComponent component)) return false;
        return pos.equals(component.pos);
    }

}
