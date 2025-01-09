package coffee.awesome_storage.dataComponent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;


public record RangeComponent(int range) implements DataComponentType<RangeComponent> {

    public static final Codec<RangeComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("range").forGetter(RangeComponent::range)

    ).apply(instance, RangeComponent::new));

    public static final StreamCodec<ByteBuf, RangeComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, RangeComponent::range,
            RangeComponent::new
    );

    @Override
    public @Nullable Codec<RangeComponent> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, RangeComponent> streamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof RangeComponent component)) return false;
        return range == component.range;
    }

}
