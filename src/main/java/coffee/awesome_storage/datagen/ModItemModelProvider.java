package coffee.awesome_storage.datagen;

import coffee.awesome_storage.Awesome_storage;
import coffee.awesome_storage.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

import static coffee.awesome_storage.Awesome_storage.MODID;


public class ModItemModelProvider extends ItemModelProvider {

    private static final ResourceLocation MISSING_ITEM = Awesome_storage.space("item/missing");
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        List<DeferredRegister.Items> ALL = new ArrayList<>();
        ALL.add(ModItems.ITEMS);

        ALL.forEach(registry -> registry.getEntries().forEach(item -> {
            String path = item.getId().getPath().toLowerCase();
            try {
                withExistingParent("item/"+path, "item/generated").texture("layer0", Awesome_storage.space("item/"+path));
            }catch (Exception e){
                withExistingParent("item/"+path,MISSING_ITEM);
            }
        }));



    }
}
