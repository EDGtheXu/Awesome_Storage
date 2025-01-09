package coffee.awesome_storage.block;

import coffee.awesome_storage.menu.MagicStorageMenu;
import coffee.awesome_storage.network.s2c.BlockPosSyncPacket;
import coffee.awesome_storage.network.s2c.ChunkPacket;
import coffee.awesome_storage.registry.ModBlocks;
import coffee.awesome_storage.registry.ModDataComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public final class MagicStorageBlockEntity extends BaseContainerBlockEntity {
    private static final Component CONTAINER_TITLE = Component.translatable("container.awesome_storage.magic_storage");

    public int max_size = 20 ;
    public int lvl = 0;
    private int tick_count = 0;
    private final ContainerData dataAccess;
    private NonNullList<ItemStack> items;

    private List<String> block_accessors;

//    public int block_accessor_count = 10;
//    private NonNullList<ItemStack> blockAccessors;

    boolean fake = false;

    public MagicStorageBlockEntity(BlockEntityType<MagicStorageBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        this.block_accessors = new ArrayList<>();

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


        if (tag.contains("BlockAccessors", 9)) {
            ListTag listTag = tag.getList("BlockAccessors", 8);
            this.block_accessors = new ArrayList<>();
            for (Tag tag1 : listTag) {
                this.block_accessors.add(tag1.getAsString());
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        max_size = tag.getInt("max_size");
        lvl = tag.getInt("lvl");
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);




        if (tag.contains("BlockAccessors", 9)) {
            ListTag listTag = tag.getList("BlockAccessors", 8);
            this.block_accessors = new ArrayList<>();
            for (Tag tag1 : listTag) {
                this.block_accessors.add(tag1.getAsString());
            }
        }

    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("max_size", max_size);
        tag.putInt("lvl", lvl);
        ContainerHelper.saveAllItems(tag, this.items, registries);

        ListTag listTag1 = new ListTag();
        for (String resource : block_accessors) {
            listTag1.add(StringTag.valueOf(resource));
        }
        tag.put("BlockAccessors", listTag1);

        return tag;
    }



    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("max_size", max_size);
        tag.putInt("lvl", lvl);
        ContainerHelper.saveAllItems(tag, this.items, registries);

        ListTag listTag1 = new ListTag();
        for (String resource : block_accessors) {
            listTag1.add(StringTag.valueOf(resource));
        }
        tag.put("BlockAccessors", listTag1);

    }

    @Override
    protected Component getDefaultName() {
        return CONTAINER_TITLE;
    }

    @Override
    public NonNullList<ItemStack> getItems() {
//        setChanged();
        if(items.size() != getContainerSize()){
            setChanged();
            NonNullList<ItemStack> filledItems = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < items.size() && i < getContainerSize(); i++) {
                filledItems.set(i, items.get(i));
            }
            return filledItems;
        }
        return items;
    }
    public void sortItems() {
        getItems().sort((item1, item2) -> {
            // 如果 item1 为空且 item2 不为空，item1 排在后面
            if (item1.isEmpty() && !item2.isEmpty()) {
                return 1;
            }
            // 如果 item2 为空且 item1 不为空，item2 排在后面
            if (item2.isEmpty() && !item1.isEmpty()) {
                return -1;
            }
            // 如果两者都为空或都不为空，按名称排序
            return item1.getDisplayName().getString().compareTo(item2.getDisplayName().getString());
        });
    }

    @Override
    public void setItems(NonNullList<ItemStack> nonNullList) {
        if(items!= nonNullList) {
            setChanged();
            level.sendBlockUpdated(this.getBlockPos(), level.getBlockState(this.getBlockPos()), level.getBlockState(this.getBlockPos()), 3);
        }
        items = nonNullList;
        sortItems();


    }
    public void setItems(List<ItemStack> items) {
        NonNullList<ItemStack> nonNullItems = NonNullList.withSize(getItems().size(), ItemStack.EMPTY);
        for (ItemStack stack : items) {
            nonNullItems.set(getItems().indexOf(stack), stack);
        }
        setItems(nonNullItems);
    }

//    public void syncFromRemote(){
//        if(fake && (level==null || level.isClientSide())){
//            PacketDistributor.sendToServer(new BlockPosSyncPacket(this.getBlockPos(),0));
//        }
//    }

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


    public List<String> getBlock_accessors() {
        return block_accessors;
    }

    public void setFake(boolean fake){
        this.fake = fake;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if(fake && (level==null || level.isClientSide())){
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    ItemStack stack = Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
                    var levelData = stack.getComponents().get(ModDataComponent.LEVEL_ACCESSOR.get());
                    if(levelData!= null)
                        PacketDistributor.sendToServer(new BlockPosSyncPacket(getBlockPos(),levelData.key(),0));
                    else
                        PacketDistributor.sendToServer(new BlockPosSyncPacket(getBlockPos(),level.dimension(),0));
                }
            }, 10);
        }
    }
}
