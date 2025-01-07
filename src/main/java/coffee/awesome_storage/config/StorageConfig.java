package coffee.awesome_storage.config;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class StorageConfig extends AbstractJsonConfig {
    private static final StorageConfig INSTANCE = new StorageConfig("magic_storage_config");
    public static StorageConfig INSTANCE() {
        return INSTANCE;
    }

    private static final Map<Integer, UpgradeLine> ENABLED_RECIPES = new HashMap<>();

    public static Map<Integer, UpgradeLine> getUpgradeLine() {
        return ENABLED_RECIPES;
    }

    protected StorageConfig(String name) {
        super(name);
    }

    public StorageConfig(JsonObject config){
        super(config);
    }

    @Override
    protected JsonObject defaultConfig() {
        Map<String, UpgradeLine> map = new HashMap<>();
        map.put("1", new UpgradeLine(new ItemStack(Items.IRON_INGOT,10), 10));
        map.put("2", new UpgradeLine(new ItemStack(Items.GOLD_INGOT,10), 10));
        map.put("3", new UpgradeLine(new ItemStack(Items.DIAMOND,10), 10));
        map.put("4", new UpgradeLine(new ItemStack(Items.EMERALD,10), 10));
        map.put("5", new UpgradeLine(new ItemStack(Items.NETHERITE_INGOT,10), 10));
        return UpgradeLine.MAP_CODEC.encodeStart(JsonOps.INSTANCE, map).result().get().getAsJsonObject();
    }

    @Override
    protected void initConfig(JsonObject json) {
        var tempMap = UpgradeLine.MAP_CODEC.decode(JsonOps.INSTANCE, json).result();
        tempMap.get().getFirst().forEach((k, v)->ENABLED_RECIPES.put(Integer.parseInt(k), v));
    }


    public record UpgradeLine(ItemStack material, int extend) {
        public static Codec<UpgradeLine> CODEC = RecordCodecBuilder.create(
                (instance) -> instance.group(
                        ItemStack.CODEC.fieldOf("material").forGetter(UpgradeLine::material),
                        Codec.INT.fieldOf("extend").forGetter(UpgradeLine::extend)
                ).apply(instance, UpgradeLine::new));

        public static UnboundedMapCodec<String, UpgradeLine> MAP_CODEC = Codec.unboundedMap(Codec.STRING, UpgradeLine.CODEC);

    }
}
