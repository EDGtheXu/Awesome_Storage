package coffee.awesome_storage.menu;

import coffee.awesome_storage.network.c2s.MagicStoragePacket;
import coffee.awesome_storage.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class MagicStorageMenu extends AbstractContainerMenu {
    public final ContainerData access;
    private final Player player;
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 4);
    private final ResultContainer resultSlot = new ResultContainer();
    private final DataSlot selectedRecipeIndex = DataSlot.standalone();
    private boolean isDirty = false;

    public Container container;
    public MagicStorageMenu(int pContainerId, Inventory inventory) {
        this(pContainerId, inventory, new SimpleContainer(3),new SimpleContainerData(2));
    }

    public MagicStorageMenu(int pContainerId, Inventory pPlayerInventory, Container container, ContainerData pAccess) {
        super(ModMenus.MAGIC_STORAGE_MENU.get(), pContainerId);
        this.player = pPlayerInventory.player;
        checkContainerDataCount(pAccess, 1);
        this.access = pAccess;
        this.container = container;


//        addSlot(new Slot(container,1,16,38));
//        addSlot(new Slot(container,2,54,38));

        for (int k = 0; k < 3; k++) {
            for (int l = 0; l < 9; l++) {
                addSlot(new Slot(pPlayerInventory, l + k * 9 + 9, 8 + l * 18, 84 + k * 18));
            }
        }
        for (int m = 0; m < 9; m++) {
            addSlot(new Slot(pPlayerInventory, m, 8 + m * 18, 142));
        }

        addDataSlots(access);
        addDataSlot(selectedRecipeIndex);
    }
    public boolean isDirty() {
        return isDirty;
    }
    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player pPlayer, int pId) {

        return true;
    }


    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return container.stillValid(pPlayer);
    }

    @Override
    public void removed(@NotNull Player pPlayer) {
        super.removed(pPlayer);
        clearContainer(pPlayer, craftSlots);
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack pStack, Slot pSlot) {
        return pSlot.container != resultSlot && super.canTakeItemForPickAll(pStack, pSlot);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int index) {
        ItemStack itemStack = slots.get(index).getItem();

        if(!itemStack.isEmpty() && player instanceof ServerPlayer) {
            this.setCarried(itemStack);
            slots.get(index).set(ItemStack.EMPTY);
            this.broadcastChanges();
            PacketDistributor.sendToServer(new MagicStoragePacket(0, itemStack));
        }

//        itemStack.setCount(0);
        return ItemStack.EMPTY;
    }

}
