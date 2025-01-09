package coffee.awesome_storage.datagen.lang;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import static coffee.awesome_storage.Awesome_storage.MODID;
import static coffee.awesome_storage.Awesome_storage.chineseProviders;

public class ModChineseProvider extends LanguageProvider {
    public ModChineseProvider(PackOutput output) {
        super(output, MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add("container.awesome_storage.magic_storage", "魔法存储");
        add("creativetab.magic_storage.cards", "魔法存储");

        add("magic_storage.missing_ingredient", "缺少材料");
        add("magic_storage.can_craft", "点击制作");
        add("magic_storage_screen.storage", "存储");
        add("magic_storage_screen.craft", "合成");

        add("magic_craft.no_access", "放置工作方块以启用一键合成");

        add("magic_storage.message.no_component", "该物品没有组件: ");
        add("magic_storage.message.too_far", "你离目标太远了!");

        add("magic_storage.tooltip.controller_range", "通信距离: ");
        add("magic_storage.tooltip.distance", "距离: ");
        add("magic_storage.tooltip.block_pos", "位置: ");
        add("magic_storage.tooltip.error_level", "无信号");



        chineseProviders.forEach(a->a.accept(this));
    }

}