package coffee.awesome_storage.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredRegister;


import java.util.function.Supplier;

import static coffee.awesome_storage.Awesome_storage.MODID;


public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final Supplier<CreativeModeTab> CARD = TABS.register("cards",
            () -> CreativeModeTab.builder()
                    .icon(Items.ENDER_CHEST::getDefaultInstance)
                    .title(Component.translatable("creativetab.magic_storage.cards"))
                    .displayItems((parameters, output) -> {
                        ModBlocks.BLOCKS.getEntries().forEach(item -> output.accept(item.get()));

                    })
                    .build()
    );

}
