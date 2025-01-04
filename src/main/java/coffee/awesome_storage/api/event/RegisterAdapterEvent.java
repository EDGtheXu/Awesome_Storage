package coffee.awesome_storage.api.event;

import coffee.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import coffee.awesome_storage.api.adapter.AdapterManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;


public class RegisterAdapterEvent extends Event implements IModBusEvent {
    public void register(RecipeType<?> recipeType, AbstractMagicCraftRecipeAdapter adapter) {
        AdapterManager.registerAdapters(recipeType, adapter);
    }
}
