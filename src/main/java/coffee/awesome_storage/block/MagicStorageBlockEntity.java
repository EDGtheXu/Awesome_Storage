package coffee.awesome_storage.block;

import coffee.awesome_storage.menu.MagicStorageMenu;
import coffee.awesome_storage.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class MagicStorageBlockEntity extends BaseContainerBlockEntity {
    private static final Component CONTAINER_TITLE = Component.translatable("container.awesome_storage.magic_storage");

    public int max_size = 20 ;
    public int lvl = 0;
    private int tick_count = 0;
    private final ContainerData dataAccess;
    private NonNullList<ItemStack> items;

//    public int block_accessor_count = 10;
//    private NonNullList<ItemStack> blockAccessors;


    public MagicStorageBlockEntity(BlockEntityType<MagicStorageBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
//        this.blockAccessors = NonNullList.withSize(block_accessor_count, Items.AIR.getDefaultInstance());
        this.dataAccess = new ContainerData() {
            public int get(int id) {
                return switch (id) {
                    case 0 -> max_size;
                    default -> 0;
                };
            }
            public void set(int id, int value) {
                switch (id) {
                    case 0 -> max_size = value;
                }

            }
            public int getCount() {
                return 1;
            }
        };

    }

    public MagicStorageBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlocks.MAGIC_STORAGE_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicStorageBlockEntity blockEntity) {
        blockEntity.tick_count++;

    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        this.max_size = tag.getInt("max_size");
        this.lvl = tag.getInt("lvl");
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, lookupProvider);
//        this.block_accessor_count = tag.getInt("block_accessor_count");
//        this.blockAccessors = NonNullList.withSize(tag.getInt("block_accessor"), ItemStack.EMPTY);


    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("max_size", max_size);
        tag.putInt("lvl", lvl);
        ContainerHelper.saveAllItems(tag, this.items, registries);
//        tag.putInt("block_accessor_count", block_accessor_count);
//        tag.put("block_accessor", ContainerHelper.saveAllItems(new CompoundTag(), blockAccessors, registries));
        return tag;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        max_size = tag.getInt("max_size");
        lvl = tag.getInt("lvl");
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
//        this.block_accessor_count = tag.getInt("block_accessor_count");
//        this.blockAccessors = NonNullList.withSize(tag.getInt("block_accessor"), ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("max_size", max_size);
        tag.putInt("lvl", lvl);
        ContainerHelper.saveAllItems(tag, this.items, registries);
//        tag.putInt("block_accessor_count", block_accessor_count);
//        tag.put("block_accessor", ContainerHelper.saveAllItems(new CompoundTag(), blockAccessors, registries));
    }

    @Override
    protected Component getDefaultName() {
        return CONTAINER_TITLE;
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        setChanged();
        if(items.size() != getContainerSize()){
            NonNullList<ItemStack> filledItems = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < items.size() && i < getContainerSize(); i++) {
                filledItems.set(i, items.get(i));
            }
            return filledItems;
        }
        return items;
    }

    @Override
    public void setItems(NonNullList<ItemStack> nonNullList) {
        if(items!= nonNullList) {
            setChanged();
            level.sendBlockUpdated(this.getBlockPos(), level.getBlockState(this.getBlockPos()), level.getBlockState(this.getBlockPos()), 3);

        }
        items = nonNullList;
    }
    @Override
    public void setItem(int index, ItemStack stack) {
        ItemStack itemstack = getItem(index);
        boolean flag = !stack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, stack);
        getItems().set(index, stack);
        stack.limitSize(getMaxStackSize(stack));
        if (index < max_size && !flag) {
            setChanged();
            level.sendBlockUpdated(this.getBlockPos(), level.getBlockState(this.getBlockPos()), level.getBlockState(this.getBlockPos()), 3);
        }

    }
    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new MagicStorageMenu(id, inventory, this,this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return max_size;
    }

    public void addContainerSize(int size ) {
        this.max_size += size;
        setChanged();
    }


}
