package coffee.awesome_storage.registry;

import coffee.awesome_storage.Awesome_storage;
import coffee.awesome_storage.dataComponent.LevelAccessorComponent;
import coffee.awesome_storage.dataComponent.RangeComponent;
import coffee.awesome_storage.item.RemoteController;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items  ITEMS = DeferredRegister.createItems("awesome_storage");


    public static final DeferredHolder<Item,Item> REMOTE_CONTROLLER_1000 = register("remote_controller_1k","1k 控制器", ()->new RemoteController(new Item.Properties()
            .component(ModDataComponent.CONTROLLER_RANGE.get(),new RangeComponent(1000))
    ));

    public static final DeferredHolder<Item,Item> REMOTE_CONTROLLER_5000 = register("remote_controller_5k","5k 控制器", ()->new RemoteController(new Item.Properties()
            .component(ModDataComponent.CONTROLLER_RANGE.get(),new RangeComponent(5000))
    ));

    public static final DeferredHolder<Item,Item> REMOTE_CONTROLLER_10000 = register("remote_controller_10k","10k 控制器", ()->new RemoteController(new Item.Properties()
            .component(ModDataComponent.CONTROLLER_RANGE.get(),new RangeComponent(10000))
    ));

    public static final DeferredHolder<Item,Item> REMOTE_CONTROLLER_INF = register("remote_controller_inf","INF 控制器", ()->new RemoteController(new Item.Properties()
            .component(ModDataComponent.CONTROLLER_RANGE.get(),new RangeComponent(-1))
    ));

    public static final DeferredHolder<Item,Item> REMOTE_CONTROLLER_THROUGH_CROSS_LEVEL = register("remote_controller_through_cross_level","跨纬度控制器", ()->new RemoteController(new Item.Properties()
            .component(ModDataComponent.CONTROLLER_RANGE.get(),new RangeComponent(-1))
            .component(ModDataComponent.LEVEL_ACCESSOR.get(),new LevelAccessorComponent(Level.OVERWORLD,true))
    ));



    public static DeferredHolder<Item,Item> register(String en, String zh, Supplier<Item> supplier) {
        DeferredItem<Item> holder = ITEMS.register(en, supplier);
        Awesome_storage.add_zh_en(holder,zh);
        return holder;
    }
}
