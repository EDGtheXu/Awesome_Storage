package coffee.awesome_storage.dataComponent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;


public record LevelAccessorComponent(ResourceKey<Level> key,boolean on) implements DataComponentType<LevelAccessorComponent> {

    public static final Codec<LevelAccessorComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("key").forGetter(LevelAccessorComponent::key),
            Codec.BOOL.fieldOf("on").forGetter(LevelAccessorComponent::on)
    ).apply(instance, LevelAccessorComponent::new));

    public static final StreamCodec<ByteBuf, LevelAccessorComponent> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    @Override
    public @Nullable Codec<LevelAccessorComponent> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, LevelAccessorComponent> streamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof LevelAccessorComponent component)) return false;
        return key.equals(component.key);
    }

}
