package coffee.awesome_storage.registry;

import coffee.awesome_storage.Awesome_storage;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {

    public static final DeferredRegister.Items  ITEMS = DeferredRegister.createItems("awesome_storage");

    public static DeferredHolder<Item,Item> register(String en, String zh, Supplier<Item> supplier) {
        DeferredItem<Item> holder = ITEMS.register(en, supplier);
        Awesome_storage.add_zh_en(holder,zh);
        return holder;
    }
}
