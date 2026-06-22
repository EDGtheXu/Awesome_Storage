package coffee.awesome_storage.client.screen;

import coffee.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import coffee.awesome_storage.api.adapter.AdapterManager;
import coffee.awesome_storage.api.adapter.CommonRecipeAdapter;
import coffee.awesome_storage.block.MagicStorageBlockEntity;
import coffee.awesome_storage.block.StorageOnlyBlock;
import coffee.awesome_storage.config.CraftConfig;
import coffee.awesome_storage.menu.MagicStorageMenu;
import coffee.awesome_storage.network.c2s.MagicCraftPacket;
import coffee.awesome_storage.network.c2s.MagicStoragePacket;
import coffee.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.client.screen.QContainerWidgetScreen;
import com.github.edg_thexu.qtcraft_api.core.QSizePolicy;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QWheelEvent;
import com.github.edg_thexu.qtcraft_api.core.geometry.QPoint;
import com.github.edg_thexu.qtcraft_api.core.geometry.QSize;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.slots.SlotKeyConsumer;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.button.QPushButton;
import com.github.edg_thexu.qtcraft_api.core.widget.button.QRadioButton;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QMainWindow;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QComboBox;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QLineEdit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import oshi.util.tuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static coffee.awesome_storage.utils.Util.getStorageEntity;
import static coffee.awesome_storage.utils.Util.getStorageItems;
//@SuppressWarnings("all")
public class MagicStorageScreen extends QContainerWidgetScreen<MagicStorageMenu> {

    private FloatingWindow storageWin;
    private FloatingWindow craftWin;
    private boolean storageOnly;
    private StoragePanel storagePanel;
    private CraftPanel craftPanel;
    private long nextRefresh;
    private long lastPeriodicRefresh;
    private List<String> lastAccessors = new ArrayList<>();

    private static int storageX = 200, storageY = 30, storageW = 220, storageH = 220;
    private static int craftX = 200, craftY = 30, craftW = 220, craftH = 220;

    public MagicStorageScreen(MagicStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected QMainWindow createRootWindow() {
        QMainWindow win = new QMainWindow();
        win.setGeometry(0, 0, width, height);

        // Player inventory widget from slot source, positioned at top-left
        QWidget invWidget = menu.getSlotSource().getRoot();
        if (invWidget != null) {
            invWidget.setParent(win);
            QSize hint = invWidget.sizeHint();
            invWidget.setGeometry(5, 5, Math.max(hint.width(), 170), hint.height());
        }

        // Determine mode
        MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
        storageOnly = be != null && be.getBlockState().getBlock() instanceof StorageOnlyBlock;

        if (!storageOnly) {
            // Craft window
            craftPanel = new CraftPanel();
            craftWin = new FloatingWindow("Crafting");
            craftWin.setWidget(craftPanel);
            craftWin.setGeometry(craftX, craftY, craftW, craftH);
            craftWin.setParent(win);
            craftPanel.updateLayout();
            craftPanel.refresh();
            craftPanel.stationsRow.refresh();
        }else{
            // Storage window
            storagePanel = new StoragePanel();
            storageWin = new FloatingWindow("Storage");
            storageWin.setWidget(storagePanel);
            storageWin.setGeometry(storageX, storageY, storageW, storageH);
            storageWin.setParent(win);
            storagePanel.updateLayout();
        }

        return win;
    }

    @Override
    protected void init() {
        super.init();
        if (rootWindow() != null) {
            rootWindow().setGeometry(0, 0, width, height);
        }
    }

    private static void renderSlot(QPainter p, int x, int y, int size, ItemStack stack, boolean highlight) {
        p.fillRect(x, y, size, size, new QColor(0xFF333333));
        if (highlight) p.fillRect(x, y, size, size, new QColor(0x55FFFFFF));
        if (!stack.isEmpty()) {
            p.renderItemStack(stack, x + (size - 16) / 2, y + (size - 16) / 2);
            if (stack.getCount() > 1) {
                p.renderItemDecorations(stack, x + (size - 16) / 2, y + (size - 16) / 2);
            }
        }
    }

    // ========================================================================
    // Storage Panel
    // ========================================================================
    private class StoragePanel extends QWidget {
        private final QLineEdit searchField;
        private final QComboBox sortCombo;
        private final QComboBox categoryCombo;
        private final QComboBox stackCombo;
        ItemGridWidget itemGrid;
        private QLabel capacityLabel;

        StoragePanel() {
            QVBoxLayout vl = new QVBoxLayout(this);
            vl.setSpacing(1);
            vl.setContentsMargins(3, 3, 3, 3);

            QHBoxLayout searchRow = new QHBoxLayout();
            searchField = new QLineEdit();
            searchField.setPlaceholderText("Search...");
            searchField.setFixedHeight(16);
            searchField.connect(searchField.TEXT_CHANGED, this, new SlotKeyConsumer<>("ss", (self, v) -> refresh()));
            searchRow.addWidget(searchField, 1);
            vl.addLayout(searchRow);

            // Filter row: sort / category / stack as dropdowns
            QHBoxLayout filterRow = new QHBoxLayout();
            filterRow.setSpacing(2);

            sortCombo = new QComboBox();
            sortCombo.addItem("Default"); sortCombo.addItem("By ID");
            sortCombo.addItem("By Name"); sortCombo.addItem("By Count");
            sortCombo.setFixedHeight(16);
            sortCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
            sortCombo.connect(sortCombo.CURRENT_INDEX_CHANGED, this, new SlotKeyConsumer<>("sc", (self, idx) -> { if (itemGrid != null) refresh(); }));
            filterRow.addWidget(sortCombo, 1);

            categoryCombo = new QComboBox();
            for (String l : new String[]{"All", "Weapon", "Tool", "Material", "Block", "Misc"}) categoryCombo.addItem(l);
            categoryCombo.setFixedHeight(16);
            categoryCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
            categoryCombo.connect(categoryCombo.CURRENT_INDEX_CHANGED, this, new SlotKeyConsumer<>("cc", (self, idx) -> { if (itemGrid != null) refresh(); }));
            filterRow.addWidget(categoryCombo, 1);

            stackCombo = new QComboBox();
            stackCombo.addItem("All"); stackCombo.addItem("Stackable"); stackCombo.addItem("Non-stackable");
            stackCombo.setFixedHeight(16);
            stackCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
            stackCombo.connect(stackCombo.CURRENT_INDEX_CHANGED, this, new SlotKeyConsumer<>("cs", (self, idx) -> { if (itemGrid != null) refresh(); }));
            filterRow.addWidget(stackCombo, 1);

            vl.addLayout(filterRow);

            QSmoothScrollArea area = new QSmoothScrollArea();
            itemGrid = new ItemGridWidget();
            itemGrid.setClickHandler(this::onItemClick);
            area.setWidget(itemGrid);
            area.setWidgetResizable(true);
            vl.addWidget(area, 1);

            capacityLabel = new QLabel(Component.literal("Capacity: 0/0"));
            vl.addWidget(capacityLabel);

            refresh();
        }

        void refresh() {
            List<ItemStack> items = getStorageItems(minecraft.player);
            if (items == null) return;
            String search = searchField.text().toLowerCase();
            String catFilter = categoryCombo.currentText();
            String sortText = sortCombo.currentText();
            String stackText = stackCombo.currentText();

            List<ItemStack> filtered = new ArrayList<>();
            for (ItemStack s : items) {
                if (s.isEmpty()) continue;
                if (!search.isEmpty() && !s.getDisplayName().getString().toLowerCase().contains(search)) continue;
                if (!catFilter.equals("All")) {
                    String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
                    if (catFilter.equals("Weapon") && !id.contains("sword") && !id.contains("bow") && !id.contains("crossbow") && !id.contains("trident")) continue;
                    if (catFilter.equals("Tool") && !id.contains("pickaxe") && !id.contains("axe") && !id.contains("shovel") && !id.contains("hoe")) continue;
                    if (catFilter.equals("Block") && !(s.getItem() instanceof BlockItem)) continue;
                    if (catFilter.equals("Material") && (s.getItem() instanceof BlockItem)) continue;
                }
                if (stackText.equals("Stackable") && !s.isStackable()) continue;
                if (stackText.equals("Non-stackable") && s.isStackable()) continue;
                filtered.add(s);
            }
            switch (sortText) {
                case "By ID" -> filtered.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i.getItem()).toString()));
                case "By Name" -> filtered.sort(Comparator.comparing(i -> i.getDisplayName().getString()));
                case "By Count" -> filtered.sort(Comparator.comparingInt(ItemStack::getCount).reversed());
            }
            itemGrid.setItems(filtered);
            MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
            if (be != null) {
                capacityLabel.setText(Component.literal("容量: " + be.getUsedSlots() + "/" + be.getTotalSlots()));
            }
        }


        void onItemClick(ItemStack stack, int index) {
            if (menu.getCarried().isEmpty() && !stack.isEmpty()) {
                int storageIdx = itemGrid.getStorageIndex(index);
                if (storageIdx >= 0) {
                    PacketDistributor.sendToServer(new MagicStoragePacket(storageIdx + 10000, new ItemStack(net.minecraft.world.item.Items.WOODEN_AXE)));
                    getStorageEntity(minecraft.player).setChanged();
                    scheduleRefresh();
                }
            }
        }
    }

    // ========================================================================
    // Craft Panel
    // ========================================================================
    private class CraftPanel extends QWidget {
        private final QLineEdit searchField;
        private final QComboBox sortCombo;
        private final QComboBox categoryCombo;
        private final QComboBox stackCombo;
        private final StationsRowWidget stationsRow;
        ItemGridWidget craftableGrid;
        private QLabel capacityLabel;
        private final CraftInfoPanel infoPanel;
        private boolean showCraftableOnly = true;

        List<ItemStack> results = new ArrayList<>();
        List<Pair<ItemStack, RecipeHolder<?>>> cachedResults = new ArrayList<>();
        Map<RecipeHolder<?>, AbstractMagicCraftRecipeAdapter> recipeMap = new HashMap<>();
        Map<Item, Integer> haveIngredients = new HashMap<>();
        RecipeHolder<?> selectedRecipe;
        AbstractMagicCraftRecipeAdapter selectedAdapter;

        CraftPanel() {
            QHBoxLayout mainLayout = new QHBoxLayout(this);
            mainLayout.setSpacing(3);
            mainLayout.setContentsMargins(3, 3, 3, 3);

            QWidget leftSide = new QWidget();
            QVBoxLayout leftLayout = new QVBoxLayout(leftSide);
            leftLayout.setSpacing(1);

            QHBoxLayout funcRow = new QHBoxLayout();
            funcRow.setSpacing(2);
            QPushButton showCraftBtn = new QPushButton(Component.literal("Craftable"));
            showCraftBtn.setFixedHeight(16);
            showCraftBtn.setOnClick(() -> { showCraftableOnly = true; refresh(); });
            funcRow.addWidget(showCraftBtn);
            QPushButton showAllBtn = new QPushButton(Component.literal("All"));
            showAllBtn.setFixedHeight(16);
            showAllBtn.setOnClick(() -> { showCraftableOnly = false; refresh(); });
            funcRow.addWidget(showAllBtn);
            searchField = new QLineEdit();
            searchField.setPlaceholderText("Search...");
            searchField.setFixedHeight(16);
            searchField.connect(searchField.TEXT_CHANGED, this, new SlotKeyConsumer<>("cs", (self, v) -> refresh()));
            funcRow.addWidget(searchField, 1);
            leftLayout.addLayout(funcRow);

            // Filter row: sort / category / stack as dropdowns
            QHBoxLayout filterRow = new QHBoxLayout();
            filterRow.setSpacing(2);

            sortCombo = new QComboBox();
            sortCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
            sortCombo.addItem("Default"); sortCombo.addItem("By ID");
            sortCombo.addItem("By Name"); sortCombo.addItem("By Count");
            sortCombo.setFixedHeight(16);
            sortCombo.connect(sortCombo.CURRENT_INDEX_CHANGED, this, new SlotKeyConsumer<>("csc", (self, idx) -> { if (craftableGrid != null) refresh(); }));
            filterRow.addWidget(sortCombo, 1);

            categoryCombo = new QComboBox();
            categoryCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
            for (String l : new String[]{"All", "Weapon", "Tool", "Material", "Block", "Misc"}) categoryCombo.addItem(l);
            categoryCombo.setFixedHeight(16);
            categoryCombo.connect(categoryCombo.CURRENT_INDEX_CHANGED, this, new SlotKeyConsumer<>("ccc", (self, idx) -> { if (craftableGrid != null) refresh(); }));
            filterRow.addWidget(categoryCombo, 1);

            stackCombo = new QComboBox();
            stackCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
            stackCombo.addItem("All"); stackCombo.addItem("Stackable"); stackCombo.addItem("Non-stackable");
            stackCombo.setFixedHeight(16);
            stackCombo.connect(stackCombo.CURRENT_INDEX_CHANGED, this, new SlotKeyConsumer<>("cst", (self, idx) -> { if (craftableGrid != null) refresh(); }));
            filterRow.addWidget(stackCombo, 1);

            leftLayout.addLayout(filterRow);

            stationsRow = new StationsRowWidget();
            stationsRow.setFixedHeight(16);
            leftLayout.addWidget(stationsRow);

            QSmoothScrollArea area = new QSmoothScrollArea();
            craftableGrid = new ItemGridWidget();
            craftableGrid.setClickHandler(this::onCraftClick);
            area.setWidget(craftableGrid);
            area.setWidgetResizable(true);
            leftLayout.addWidget(area, 1);

            capacityLabel = new QLabel(Component.literal("Capacity: 0/0"));
            leftLayout.addWidget(capacityLabel);

            mainLayout.addWidget(leftSide, 1);

            QSmoothScrollArea scrollArea = new QSmoothScrollArea();
            infoPanel = new CraftInfoPanel(this);
            infoPanel.setFixedWidth(75);
            scrollArea.setFixedWidth(85);
            scrollArea.setWidget(infoPanel);
            scrollArea.setWidgetResizable(true);
            mainLayout.addWidget(scrollArea);

            try { reloadRecipes(); } catch (Exception e) { e.printStackTrace(); }
            try { refresh(); } catch (Exception e) { e.printStackTrace(); }
        }

        void reloadRecipes() {
            MagicStorageBlockEntity storage = getStorageEntity(minecraft.player);
            if (storage == null || minecraft.level == null) return;
            results.clear();
            recipeMap.clear();
            List<Block> accessors = storage.getBlock_accessors().stream()
                    .map(s -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s))).toList();
            for (var entry : BuiltInRegistries.RECIPE_TYPE.entrySet()) {
                RecipeType<?> rt = entry.getValue();
                if (CraftConfig.ENABLED_RECIPES.containsKey(rt)) {
                    boolean accept = CraftConfig.ENABLED_RECIPES.get(rt).stream().allMatch(b -> accessors.contains(b));
                    if (accept) {
                        AbstractMagicCraftRecipeAdapter<RecipeInput, Recipe<RecipeInput>> adapter =
                                AdapterManager.Adapters.containsKey(rt)
                                        ? (AbstractMagicCraftRecipeAdapter<RecipeInput, Recipe<RecipeInput>>) (Object) AdapterManager.Adapters.get(rt)
                                        : new CommonRecipeAdapter(rt);
                        var recipes = minecraft.level.getRecipeManager().getAllRecipesFor(adapter.getRecipe());
                        for (var r : recipes) adapter.loadRecipe(r, results, recipeMap);
                    }
                }
            }
        }

        void refresh() {
            List<ItemStack> have = getStorageItems(minecraft.player);
            if (have == null) return;
            haveIngredients.clear();
            for (ItemStack s : have) haveIngredients.put(s.getItem(), haveIngredients.getOrDefault(s.getItem(), 0) + s.getCount());

            List<Pair<ItemStack, RecipeHolder<?>>> craftable = new ArrayList<>();
            List<Pair<ItemStack, RecipeHolder<?>>> partial = new ArrayList<>();
            for (Map.Entry<RecipeHolder<?>, AbstractMagicCraftRecipeAdapter> e : recipeMap.entrySet()) {
                var adapter = e.getValue();
                NonNullList<Ingredient> ings = adapter.getIngredients(e.getKey());
                Map<Item, Integer> temp = new HashMap<>(haveIngredients);
                boolean can = true, has = false;
                for (Ingredient ing : ings) {
                    if (ing.isEmpty()) continue;
                    boolean found = false;
                    for (ItemStack is : ing.getItems()) {
                        int req = is.getCount();
                        Integer avail = temp.get(is.getItem());
                        if (avail != null && avail >= req) {
                            temp.put(is.getItem(), avail - req);
                            found = true;
                            has = true;
                            break;
                        }
                    }
                    if (!found) can = false;
                }
                Pair<ItemStack, RecipeHolder<?>> p = new Pair<>(adapter.getResult(e.getKey()), e.getKey());
                if (can) craftable.add(p);
                else if (has) partial.add(p);
            }
            craftable.sort(Comparator.comparing(a -> a.getA().getDisplayName().getString()));
            partial.sort(Comparator.comparing(a -> a.getA().getDisplayName().getString()));
            cachedResults = new ArrayList<>(craftable);
            if (!showCraftableOnly) cachedResults.addAll(partial);

            String search = searchField.text().toLowerCase();
            String catFilter = categoryCombo.currentText();
            String sortText = sortCombo.currentText();
            String stackText = stackCombo.currentText();
            List<ItemStack> display = new ArrayList<>();
            for (Pair<ItemStack, RecipeHolder<?>> p : cachedResults) {
                ItemStack s = p.getA();
                if (!search.isEmpty() && !s.getDisplayName().getString().toLowerCase().contains(search)) continue;
                if (!catFilter.equals("All")) {
                    String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
                    if (catFilter.equals("Weapon") && !id.contains("sword") && !id.contains("bow") && !id.contains("crossbow") && !id.contains("trident")) continue;
                    if (catFilter.equals("Tool") && !id.contains("pickaxe") && !id.contains("axe") && !id.contains("shovel") && !id.contains("hoe")) continue;
                    if (catFilter.equals("Block") && !(s.getItem() instanceof BlockItem)) continue;
                    if (catFilter.equals("Material") && (s.getItem() instanceof BlockItem)) continue;
                }
                if (stackText.equals("Stackable") && !s.isStackable()) continue;
                if (stackText.equals("Non-stackable") && s.isStackable()) continue;
                display.add(s);
            }
            switch (sortText) {
                case "By ID" -> display.sort(Comparator.comparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
                case "By Name" -> display.sort(Comparator.comparing(s -> s.getDisplayName().getString()));
                case "By Count" -> display.sort(Comparator.comparingInt(s -> -s.getCount()));
            }
            craftableGrid.setItems(display);
            stationsRow.refresh();
            updateCapacity();
        }

        void updateCapacity() {
            MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
            if (be != null) {
                capacityLabel.setText(Component.literal("Capacity:" + be.getUsedSlots() + "/" + be.getTotalSlots()));
            }
        }

        void onCraftClick(ItemStack stack, int index) {
            if (index >= 0 && index < cachedResults.size()) {
                var pair = cachedResults.get(index);
                selectedRecipe = pair.getB();
                selectedAdapter = recipeMap.get(selectedRecipe);
                if (selectedRecipe != null && selectedAdapter != null) {
                    infoPanel.showRecipe(selectedRecipe, selectedAdapter, pair.getA(), haveIngredients);
                }
            }
        }
    }

    // ========================================================================
    // Stations Row
    // ========================================================================
    private class StationsRowWidget extends QWidget {
        private final List<ItemStack> stations = new ArrayList<>();
        private int scrollOffset;

        public int getScrollOffset() { return scrollOffset; }
        public int getStationCount() { return stations.size(); }

        void refresh() {
            stations.clear();
            MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
            if (be != null) {
                for (String s : be.getBlock_accessors()) {
                    Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s));
                    if (b != null) stations.add(new ItemStack(b));
                }
            }
            stations.add(ItemStack.EMPTY);
            update();
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter p = event.painter();
            if (p == null) return;
            p.fillRect(0, 0, width(), height(), new QColor(0xCC333333));
            int slot = 14, gap = 1;
            p.enableClip(0, 0, width(), height());
            p.push();
            p.translate(-scrollOffset, 0);
            for (int i = 0; i < stations.size(); i++) {
                int x = i * (slot + gap);
                renderSlot(p, x, 2, slot, stations.get(i), false);
                if (stations.get(i).isEmpty()) {
                    p.setColor(new QColor(0xFF888888));
                    p.drawText("+", x + slot / 2 - 3, 2 + slot / 2 - 4);
                }
            }
            p.pop();
            p.disableClip();
        }

        @Override
        protected void wheelEvent(QWheelEvent event) {
            int max = Math.max(0, stations.size() * 34 - width());
            scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) (event.delta() * 0.8)));
            update();
            event.accept();
        }
    }

    // ========================================================================
    // Item Grid
    // ========================================================================
    private class ItemGridWidget extends QWidget {
        private List<ItemStack> items = new ArrayList<>();
        private List<Integer> storageIndices = new ArrayList<>();
        private int hoverIndex = -1;
        private int cols = 8;
        private int slotSize = 18;
        private java.util.function.BiConsumer<ItemStack, Integer> clickHandler;

        ItemGridWidget() {
            setFocusPolicy(com.github.edg_thexu.qtcraft_api.core.widget.QWidget.FocusPolicy.NoFocus);
        }

        void setItems(List<ItemStack> items) {
            this.items = items;
            // Map each display item to its index in the full getStoredItems() list
            storageIndices.clear();
            MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
            List<ItemStack> full = be != null ? be.getStoredItems() : new ArrayList<>();
            if (full != null) {
                for (ItemStack display : items) {
                    int idx = -1;
                    for (int i = 0; i < full.size(); i++) {
                        if (ItemStack.isSameItemSameComponents(display, full.get(i))) {
                            idx = i;
                            break;
                        }
                    }
                    storageIndices.add(idx);
                }
            }
            updateCols();
            update();
        }

        int getItemCount() { return items.size(); }
        ItemStack getItemAt(int i) { return (i >= 0 && i < items.size()) ? items.get(i) : ItemStack.EMPTY; }
        int getCols() { return cols; }

        int getStorageIndex(int displayIndex) {
            if (displayIndex >= 0 && displayIndex < storageIndices.size()) {
                return storageIndices.get(displayIndex);
            }
            return -1;
        }

        void setClickHandler(java.util.function.BiConsumer<ItemStack, Integer> h) { this.clickHandler = h; }

        private void updateCols() {
            int newCols = Math.max(1, (width() + 2) / slotSize);
            if (newCols != cols) {
                cols = newCols;
                markDirty();
            }
        }

        @Override
        protected void resizeEvent(com.github.edg_thexu.qtcraft_api.core.events.QResizeEvent event) {
            updateCols();
            super.resizeEvent(event);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter p = event.painter();
            if (p == null) return;
            updateCols();
            for (int i = 0; i < items.size(); i++) {
                int col = i % cols, row = i / cols;
                renderSlot(p, col * slotSize, row * slotSize, slotSize, items.get(i), i == hoverIndex);
            }
        }

        @Override
        protected void mousePressEvent(QMouseEvent event) {
            event.accept();
            if (event.button() == QMouseEvent.Button.Left) {
                updateCols();
                int col = event.x() / slotSize, row = event.y() / slotSize;
                int idx = row * cols + col;
                if (idx >= 0 && idx < items.size() && !items.get(idx).isEmpty() && clickHandler != null) {
                    clickHandler.accept(items.get(idx), idx);
                }
            }
        }

        @Override
        protected void mouseMoveEvent(QMouseEvent event) {
            updateCols();
            int col = event.x() / slotSize, row = event.y() / slotSize;
            int idx = row * cols + col;
            int prev = hoverIndex;
            hoverIndex = (idx >= 0 && idx < items.size()) ? idx : -1;
            if (prev != hoverIndex) { markDirty(); update(); }
        }

        @Override
        public QSize sizeHint() {
            int rows = Math.max(1, (items.size() + cols - 1) / cols);
            return new QSize(cols * slotSize, rows * slotSize);
        }
    }

    // ========================================================================
    // Craft Info Panel
    // ========================================================================
    private class CraftInfoPanel extends QWidget {
        private final CraftPanel parent;
        private ItemStack outputItem = ItemStack.EMPTY;
        private List<ItemStack> ingredients = new ArrayList<>();
        private List<ItemStack> requiredStations = new ArrayList<>();
        private List<ItemStack> storageMaterials = new ArrayList<>();
        private boolean hasRecipe;

        CraftInfoPanel(CraftPanel parent) { this.parent = parent; setMinimumSize(180, 200); }

        @SuppressWarnings("unchecked")
        void showRecipe(RecipeHolder<?> recipe, AbstractMagicCraftRecipeAdapter adapter, ItemStack output, Map<Item, Integer> have) {
            outputItem = output;
            ingredients.clear();
            requiredStations.clear();
            storageMaterials.clear();
            var casted = (AbstractMagicCraftRecipeAdapter<RecipeInput, Recipe<RecipeInput>>) adapter;
            NonNullList<Ingredient> ings = casted.getIngredients((RecipeHolder<Recipe<RecipeInput>>) (Object) recipe);
            // Merge ingredients by item
            java.util.Map<Item, Integer> merged = new java.util.LinkedHashMap<>();
            for (Ingredient ing : ings) {
                if (ing.getItems().length > 0) {
                    ItemStack first = ing.getItems()[0];
                    merged.merge(first.getItem(), first.getCount(), Integer::sum);
                }
            }
            for (java.util.Map.Entry<Item, Integer> e : merged.entrySet()) {
                ItemStack stack = new ItemStack(e.getKey(), e.getValue());
                ingredients.add(stack);
            }
            // Only show items in storage that match recipe ingredients
            Set<Item> relevantItems = new HashSet<>();
            for (Ingredient ing : ings) {
                for (ItemStack is : ing.getItems()) {
                    relevantItems.add(is.getItem());
                }
            }
            for (Map.Entry<Item, Integer> e : have.entrySet()) {
                if (relevantItems.contains(e.getKey())) {
                    storageMaterials.add(new ItemStack(e.getKey(), Math.min(e.getValue(), 99)));
                }
            }
            if (CraftConfig.ENABLED_RECIPES.containsKey(adapter.getRecipe())) {
                for (Block b : CraftConfig.ENABLED_RECIPES.get(adapter.getRecipe())) {
                    requiredStations.add(new ItemStack(b));
                }
            }
            hasRecipe = true;
            markDirty();
            update();
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter p = event.painter();
            if (p == null) return;
            p.fillRect(0, 0, width(), height(), new QColor(0xCC2A2A2A));
            p.drawRect(0, 0, width(), height(), new QColor(0xFF555555));
            if (!hasRecipe) { p.setColor(new QColor(0xFF888888)); p.drawText("Select an item", 8, 20); return; }

            int y = 6, ss = 18;
            p.setColor(new QColor(0xFFFFAA00));
            p.drawText("Output:", 6, y); y += 11;
            renderSlot(p, 6, y, 20, outputItem, false); y += 38;
            p.setColor(QColor.WHITE);
            p.drawText("Ingredients:", 6, y); y += 11;
            int ix = 6;
            for (ItemStack ing : ingredients) {
                boolean miss = !parent.haveIngredients.containsKey(ing.getItem()) || parent.haveIngredients.get(ing.getItem()) < ing.getCount();
                renderSlot(p, ix, y, ss, ing, false);
                if (miss) p.fillRect(ix, y, ss, ss, new QColor(0x44FF0000));
                ix += ss + 2;
                if (ix > width() - ss) { ix = 6; y += ss + 2; }
            }
            if (!ingredients.isEmpty()) {
                if(ix > 6) {
                    y += ss + 6;
                }else{
                    y += 4;
                }
            }

            p.drawText("Stations:", 6, y); y += 11; ix = 6;
            for (ItemStack st : requiredStations) { renderSlot(p, ix, y, ss, st, false); ix += ss + 2; if (ix > width() - ss) { ix = 6; y += ss + 2; } }
            if (!requiredStations.isEmpty()) y += ss + 6;

            p.drawText("In Storage:", 6, y); y += 11; ix = 6;
            // Recalculate storage materials on every paint to reflect changes
            storageMaterials.clear();
            for (Map.Entry<Item, Integer> e : parent.haveIngredients.entrySet()) {
                boolean isIngredient = false;
                for (ItemStack ing : this.ingredients) {
                    if (e.getKey() == ing.getItem()) { isIngredient = true; break; }
                }
                if (isIngredient) {
                    storageMaterials.add(new ItemStack(e.getKey(), Math.min(e.getValue(), 99)));
                }
            }
            for (ItemStack ms : storageMaterials) {
                renderSlot(p, ix, y, ss, ms, false);
                ix += ss + 2; if (ix > width() - ss) { ix = 6; y += ss + 2; }
            }
        }

        @Override
        protected void mousePressEvent(QMouseEvent event) {
            if (event.button() == QMouseEvent.Button.Left && hasRecipe && parent.selectedRecipe != null && parent.selectedAdapter != null) {
                int outY = 6 + 11;
                if (event.x() >= 6 && event.x() <= 38 && event.y() >= outY && event.y() <= outY + 32) {
                    PacketDistributor.sendToServer(new MagicCraftPacket(parent.selectedRecipe.id(),
                            BuiltInRegistries.RECIPE_TYPE.getKey(parent.selectedAdapter.getRecipe())));
                    var be = Util.getStorageEntity(minecraft.player);
                    if (be != null) be.setChanged();
                    scheduleRefresh();
                    event.accept();
                }
            }
        }

        @Override
        public QSize sizeHint() { return new QSize(180, 300); }
    }

    // ========================================================================
    // Screen Rendering
    // ========================================================================
    private void scheduleRefresh() {
        nextRefresh = System.currentTimeMillis() + 150;
    }

    private void scheduleReloadRecipes() {
        lastAccessors = new ArrayList<>();
        scheduleRefresh();
    }


    private void refreshPanels() {
        if (storagePanel != null) storagePanel.refresh();
        if (craftPanel != null) {
            // Detect block_accessors changes (server sync via onDataPacket)
            MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
            if (be != null) {
                List<String> current = be.getBlock_accessors();
                if (!current.equals(lastAccessors)) {
                    lastAccessors = new ArrayList<>(current);
                    try { craftPanel.reloadRecipes(); } catch (Exception e) { e.printStackTrace(); }
                }
            }
            craftPanel.refresh();
            craftPanel.stationsRow.refresh();
            craftPanel.infoPanel.markDirty();
            craftPanel.infoPanel.update();
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        if (nextRefresh != 0 && now > nextRefresh) {
            nextRefresh = 0;
            refreshPanels();
        }
        if (now - lastPeriodicRefresh > 300) {
            lastPeriodicRefresh = now;
            refreshPanels();
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        if (storageWin != null) {
            storageX = storageWin.x(); storageY = storageWin.y();
            storageW = storageWin.width(); storageH = storageWin.height();
        }
        if (craftWin != null) {
            craftX = craftWin.x(); craftY = craftWin.y();
            craftW = craftWin.width(); craftH = craftWin.height();
        }
        super.removed();
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, width, height, 0xCC000000, 0xCC000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hasShiftDown()) scheduleRefresh();

        if (button == 0 && craftPanel != null && craftWin != null && craftWin.isVisible()) {
            StationsRowWidget srw = craftPanel.stationsRow;
            QPoint lt = srw.mapToGlobal(QPoint.ZERO);
            QPoint rb = srw.mapToGlobal(new QPoint(srw.width(), srw.height()));
            if (mouseX > lt.x() && mouseX < rb.x() && mouseY > lt.y() && mouseY < rb.y()) {
                int slot = 14, gap = 1, scroll = srw.getScrollOffset();
                int idx = ((int) mouseX - lt.x() + scroll) / (slot + gap);
                if (idx >= 0 && idx < srw.getStationCount()) {
                    if (idx < srw.getStationCount() - 1) {
                        PacketDistributor.sendToServer(new MagicStoragePacket(20000 + idx, ItemStack.EMPTY));
                    } else {
                        ItemStack held = menu.getCarried();
                        if (!held.isEmpty() && held.getItem() instanceof BlockItem bi && CraftConfig.isEnabledBlock(bi.getBlock())) {
                            PacketDistributor.sendToServer(new MagicStoragePacket(1, held));
                        }
                    }
                    var be = Util.getStorageEntity(minecraft.player);
                    if (be != null) be.setChanged();
                    scheduleReloadRecipes();
                    return true;
                }
            }
            // Craftable grid click detection
            ItemGridWidget cg = craftPanel.craftableGrid;
            QPoint glt = cg.mapToGlobal(QPoint.ZERO);
            int gridCols = cg.getCols(), gSlot = 18;
            QPoint grb = cg.mapToGlobal(new QPoint(cg.width(), cg.height()));
            if (mouseX >= glt.x() && mouseX < grb.x() && mouseY >= glt.y() && mouseY < grb.y()) {
                int col = (int) (mouseX - glt.x()) / gSlot;
                int row = (int) (mouseY - glt.y()) / gSlot;
                int idx = row * gridCols + col;
                if (idx >= 0 && idx < cg.getItemCount()) {
                    if (!cg.getItemAt(idx).isEmpty()) {
                        craftPanel.onCraftClick(cg.getItemAt(idx), idx);
                        return true;
                    }
                }
            }
        }

        if (button == 0 && storagePanel != null && storageWin != null && storageWin.isVisible()) {
            QPoint glt = storagePanel.itemGrid.mapToGlobal(QPoint.ZERO);
            int gSlot = 18, gCols = storagePanel.itemGrid.getCols();
            QPoint grb = storagePanel.itemGrid.mapToGlobal(new QPoint(storagePanel.itemGrid.width(), storagePanel.itemGrid.height()));
            if (mouseX >= glt.x() && mouseX < grb.x() && mouseY >= glt.y() && mouseY < grb.y()) {
                int col = (int) (mouseX - glt.x()) / gSlot;
                int row = (int) (mouseY - glt.y()) / gSlot;
                int idx = row * gCols + col;
                if (idx >= 0 && idx < storagePanel.itemGrid.getItemCount()) {
                    ItemStack stack = storagePanel.itemGrid.getItemAt(idx);
                    if (!stack.isEmpty() && menu.getCarried().isEmpty()) {
                        int storageIdx = storagePanel.itemGrid.getStorageIndex(idx);
                        if (storageIdx >= 0) {
                            PacketDistributor.sendToServer(new MagicStoragePacket(storageIdx + 10000, new ItemStack(net.minecraft.world.item.Items.WOODEN_AXE)));
                            getStorageEntity(minecraft.player).setChanged();
                            scheduleRefresh();
                            return true;
                        }
                    }
                }
            }
        }

        // Normal QTCraft widget dispatch
        if (widgetDelegate.mouseClicked(mouseX, mouseY, button)) return true;
        // Store action into storage window (carrying item, click anywhere)
        if (!menu.getCarried().isEmpty() && storageWin != null && storageWin.isVisible()
                && mouseX >= storageWin.x() && mouseX <= storageWin.x() + storageWin.width()
                && mouseY >= storageWin.y() && mouseY <= storageWin.y() + storageWin.height()) {
            PacketDistributor.sendToServer(new MagicStoragePacket(0, menu.getCarried()));
            getStorageEntity(minecraft.player).setChanged();
            scheduleRefresh();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {}
}
