package coffee.awesome_storage.compat.sophisticated;

import net.neoforged.fml.ModList;

public class SophisticatedHelper {
    static Boolean isCoreLoad;
    static Boolean isStorageLoad;

    public static boolean isCoreLoaded() {
        if(isCoreLoad == null){
            isCoreLoad = ModList.get().isLoaded("sophisticatedcore");
        }
        return isCoreLoad;
    }

    public static boolean isStorageLoaded() {
        if(isStorageLoad == null){
            isStorageLoad = ModList.get().isLoaded("sophisticatedstorage");
        }
        return isStorageLoad;
    }
}
