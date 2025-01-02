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

        chineseProviders.forEach(a->a.accept(this));
    }

}