package coffee.awesome_storage.event;

import coffee.awesome_storage.api.adapter.SmithingRecipeAdapter;
import coffee.awesome_storage.api.event.RegisterAdapterEvent;
import coffee.awesome_storage.network.NetworkHandler;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import static coffee.awesome_storage.Awesome_storage.MODID;
import static coffee.awesome_storage.api.adapter.AdapterManager.defaultAdapter;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvent {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
        });
    }

    @SubscribeEvent
    public static void onRegisterAdapter(RegisterAdapterEvent event) {
        event.register(RecipeType.CRAFTING, defaultAdapter);
        event.register(RecipeType.SMITHING, new SmithingRecipeAdapter<>(RecipeType.SMITHING));
    }


}
