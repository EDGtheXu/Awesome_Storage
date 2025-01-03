package coffee.awesome_storage.config;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static coffee.awesome_storage.Awesome_storage.MODID;

public class StorageConfig {

    private static final Map<Integer, UpgradeLine> ENABLED_RECIPES = new HashMap<>();

    public static Map<Integer, UpgradeLine> getUpgradeLine() {
        return ENABLED_RECIPES;
    }

    public static JsonElement parseCodec(DataResult<?> result){
        return JsonParser.parseString(new Gson().toJson(result.result().get()));
    }

    public static void loadUpgradeLine(){
        Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(MODID);
        Path configFile = CONFIG_PATH.resolve("magic_storage_config.json");
        File file = configFile.toFile();
        try {
            Reader reader;
            JsonObject json;
            if (!file.exists()) {
                CONFIG_PATH.toFile().mkdirs();
                file.createNewFile();
                reader = new java.io.FileReader(file);
//                json = new JsonObject();

                Map<String, UpgradeLine> map = new HashMap<>();
                map.put("1", new UpgradeLine(new ItemStack(Items.IRON_INGOT,10), 10));
                map.put("2", new UpgradeLine(new ItemStack(Items.GOLD_INGOT,10), 10));
                map.put("3", new UpgradeLine(new ItemStack(Items.DIAMOND,10), 10));
                map.put("4", new UpgradeLine(new ItemStack(Items.EMERALD,10), 10));
                map.put("5", new UpgradeLine(new ItemStack(Items.NETHERITE_INGOT,10), 10));
                json = UpgradeLine.MAP_CODEC.encodeStart(JsonOps.INSTANCE, map).result().get().getAsJsonObject() ;

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Writer writer = new java.io.FileWriter(file);
                writer.write(gson.toJson(json));
                writer.close();
            }else{
                reader = new java.io.FileReader(file);
                json = GsonHelper.parse(reader);
            }
            var tempMap = UpgradeLine.MAP_CODEC.decode(JsonOps.INSTANCE, json).result();
            tempMap.get().getFirst().forEach((k, v)->ENABLED_RECIPES.put(Integer.parseInt(k), v));
            reader.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
