package coffee.awesome_storage.datagen.lang;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Arrays;
import java.util.stream.Collectors;

import static coffee.awesome_storage.Awesome_storage.MODID;
import static coffee.awesome_storage.Awesome_storage.englishProviders;


public class ModEnglishProvider extends LanguageProvider {

    public ModEnglishProvider(PackOutput output) {
        super(output, MODID, "en_us");
    }
    public static String toTitleCase(String raw) {
        return Arrays.stream(raw
                        .split("[_/]"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
    @Override
    protected void addTranslations() {
        add("container.awesome_storage.magic_storage", "Magic Storage");
        add("creativetab.magic_storage.cards", "Magic Storage");

        add("magic_storage.missing_ingredient", "Missing Ingredient");
        add("magic_storage.can_craft", "Click to craft");
        add("magic_storage_screen.storage", "Storage");
        add("magic_storage_screen.craft", "Craft");

        add("magic_craft.no_access", "Push Work Table to Access Crafting");

        add("magic_storage.message.no_component", "This item has no component: ");
        add("magic_storage.message.too_far", "Container is Too Far!");

        add("magic_storage.tooltip.controller_range", "Communication Distance: ");
        add("magic_storage.tooltip.distance", "Distance: ");
        add("magic_storage.tooltip.block_pos", "Your Position: ");
        add("magic_storage.tooltip.error_level", "No Signal");








        englishProviders.forEach(a->a.accept(this));
    }

}