package com.github.edg_thexu.awesome_storage.core.registry;

import com.github.edg_thexu.awesome_storage.AwesomeStorage;
import com.github.edg_thexu.awesome_storage.core.data_component.LevelAccessorComponent;
import com.github.edg_thexu.awesome_storage.core.data_component.RangeComponent;
import com.github.edg_thexu.awesome_storage.core.item.RemoteController;
import com.github.edg_thexu.awesome_storage.core.item.QueueUpgradeItem;
import com.github.edg_thexu.awesome_storage.core.item.WirelessNetworkCard;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("awesome_storage");


    public static final DeferredHolder<Item, Item> BASE_REMOTE_CONTROLLER = register("base_portable_remote_storage_access", "基础便捷远程存储装置", () -> new RemoteController(new Item.Properties()
            .component(ModDataComponent.CONTROLLER_RANGE.get(), 200)
    ));

    public static final DeferredHolder<Item, Item> ADVANCE_REMOTE_CONTROLLER = register("advance_portable_remote_storage_access", "进阶便捷远程存储装置", () -> new RemoteController(new Item.Properties()
            .component(ModDataComponent.CONTROLLER_RANGE.get(), 1000)
    ));

    public static final DeferredHolder<Item, Item> FINAL_REMOTE_CONTROLLER = register("final_portable_remote_storage_access", "终极便捷远程存储装置", () -> new RemoteController(new Item.Properties()
            .component(ModDataComponent.CONTROLLER_RANGE.get(), -1)
            .component(ModDataComponent.LEVEL_ACCESSOR.get(), new LevelAccessorComponent(Level.OVERWORLD, true))
    ));


    public static final DeferredHolder<Item, Item> BASE_NETWORK_CARD = register("base_wireless_network_card", "基础无线网卡",
            () -> new WirelessNetworkCard(new Item.Properties().stacksTo(1), 10));

    public static final DeferredHolder<Item, Item> ADVANCE_NETWORK_CARD = register("advance_wireless_network_card", "进阶无线网卡",
            () -> new WirelessNetworkCard(new Item.Properties().stacksTo(1), 50));

    public static final DeferredHolder<Item, Item> FINAL_NETWORK_CARD = register("final_wireless_network_card", "终极无线网卡",
            () -> new WirelessNetworkCard(new Item.Properties().stacksTo(1), 100));

    public static final DeferredHolder<Item, Item> QUEUE_UPGRADE_1 = register("queue_upgrade_1", "队列升级 I",
            () -> new QueueUpgradeItem(new Item.Properties().stacksTo(1), 1));

    public static final DeferredHolder<Item, Item> QUEUE_UPGRADE_2 = register("queue_upgrade_2", "队列升级 II",
            () -> new QueueUpgradeItem(new Item.Properties().stacksTo(1), 2));

    public static final DeferredHolder<Item, Item> QUEUE_UPGRADE_3 = register("queue_upgrade_3", "队列升级 III",
            () -> new QueueUpgradeItem(new Item.Properties().stacksTo(1), 3));

    public static DeferredHolder<Item, Item> register(String en, String zh, Supplier<Item> supplier) {
        DeferredItem<Item> holder = ITEMS.register(en, supplier);
        AwesomeStorage.add_zh_en(holder, zh);
        return holder;
    }
}
