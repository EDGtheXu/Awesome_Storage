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

    private static int storageX = 200, storageY = 30, storageW = 420, storageH = 520;
    private static int craftX = 200, craftY = 30, craftW = 420, craftH = 520;

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
                p.setColor(QColor.WHITE);
                String countStr = stack.getCount() >= 1000 ? (stack.getCount() / 1000) + "k" : String.valueOf(stack.getCount());
                p.renderItemDecorations(stack, x + (size - 16) / 2, y + (size - 16) / 2);
            }
        }
    }

    // ========================================================================
    // Storage Panel
    // ========================================================================
    private class StoragePanel extends QWidget {
        private final QLineEdit searchField;
        private final List<QRadioButton> sortButtons;
        private final List<QRadioButton> categoryButtons;
        private final List<QRadioButton> stackButtons;
        private ItemGridWidget itemGrid;
        private QLabel capacityLabel;
        private int sortMode;
        private String categoryFilter = "All";
        private int stackFilter;

        StoragePanel() {
            QVBoxLayout vl = new QVBoxLayout(this);
            vl.setSpacing(1);
            vl.setContentsMargins(3, 3, 3, 3);

            QHBoxLayout searchRow = new QHBoxLayout();
            searchField = new QLineEdit();
            searchField.setPlaceholderText("Search...");
            searchField.setFixedHeight(16);
            searchField.connect(QLineEdit.TEXT_CHANGED, this, new SlotKeyConsumer<>("ss", (self, v) -> refresh()));
            searchRow.addWidget(searchField, 1);
            vl.addLayout(searchRow);

            QHBoxLayout sortRow = new QHBoxLayout();
            sortRow.setSpacing(1);
            sortButtons = new ArrayList<>();
            for (String label : new String[]{"Default", "By ID", "By Name", "By Count"}) {
                QRadioButton rb = new QRadioButton(Component.literal(label), "ss");
                rb.setFixedHeight(14);
                int idx = sortButtons.size();
                int fi = idx;
                rb.connect(QRadioButton.TOGGLED, this, new SlotKeyConsumer<>("so" + idx, (self, checked) -> {
                    if (checked && itemGrid != null) { sortMode = fi; refresh(); }
                }));
                sortButtons.add(rb);
                sortRow.addWidget(rb);
            }
            sortRow.addStretch(1);
            vl.addLayout(sortRow);

            QHBoxLayout catRow = new QHBoxLayout();
            catRow.setSpacing(1);
            categoryButtons = new ArrayList<>();
            for (String label : new String[]{"All", "Weapon", "Tool", "Material", "Block", "Misc"}) {
                QRadioButton rb = new QRadioButton(Component.literal(label), "sc");
                rb.setFixedHeight(14);
                String fl = label;
                rb.connect(QRadioButton.TOGGLED, this, new SlotKeyConsumer<>("ca" + label, (self, checked) -> {
                    if (checked && itemGrid != null) { categoryFilter = fl; refresh(); }
                }));
                categoryButtons.add(rb);
                catRow.addWidget(rb);
            }
            vl.addLayout(catRow);

            QHBoxLayout stackRow = new QHBoxLayout();
            stackRow.setSpacing(1);
            stackButtons = new ArrayList<>();
            for (String label : new String[]{"All", "Stackable", "Non-stackable"}) {
                QRadioButton rb = new QRadioButton(Component.literal(label), "sk");
                rb.setFixedHeight(14);
                int idx = stackButtons.size();
                int fi = idx;
                rb.connect(QRadioButton.TOGGLED, this, new SlotKeyConsumer<>("st" + idx, (self, checked) -> {
                    if (checked && itemGrid != null) { stackFilter = fi; refresh(); }
                }));
                stackButtons.add(rb);
                stackRow.addWidget(rb);
            }
            vl.addLayout(stackRow);

            QSmoothScrollArea area = new QSmoothScrollArea();
            itemGrid = new ItemGridWidget();
            itemGrid.setClickHandler(this::onItemClick);
            area.setWidget(itemGrid);
            area.setWidgetResizable(true);
            vl.addWidget(area, 1);

            capacityLabel = new QLabel(Component.literal("Capacity: 0/0"));
            vl.addWidget(capacityLabel);

            sortButtons.get(0).setChecked(true);
            categoryButtons.get(0).setChecked(true);
            stackButtons.get(0).setChecked(true);

            refresh();
        }

        void refresh() {
            List<ItemStack> items = getStorageItems(minecraft.player);
            if (items == null) return;
            String search = searchField.text().toLowerCase();

            List<ItemStack> filtered = new ArrayList<>();
            for (ItemStack s : items) {
                if (s.isEmpty()) continue;
                if (!search.isEmpty() && !s.getDisplayName().getString().toLowerCase().contains(search)) continue;
                if (!categoryFilter.equals("All")) {
                    String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
                    if (categoryFilter.equals("Weapon") && !id.contains("sword") && !id.contains("bow") && !id.contains("crossbow") && !id.contains("trident")) continue;
                    if (categoryFilter.equals("Tool") && !id.contains("pickaxe") && !id.contains("axe") && !id.contains("shovel") && !id.contains("hoe")) continue;
                    if (categoryFilter.equals("Block") && !(s.getItem() instanceof BlockItem)) continue;
                    if (categoryFilter.equals("Material") && (s.getItem() instanceof BlockItem)) continue;
                }
                if (stackFilter == 1 && !s.isStackable()) continue;
                if (stackFilter == 2 && s.isStackable()) continue;
                filtered.add(s);
            }
            switch (sortMode) {
                case 1 -> filtered.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i.getItem()).toString()));
                case 2 -> filtered.sort(Comparator.comparing(i -> i.getDisplayName().getString()));
                case 3 -> filtered.sort(Comparator.comparingInt(ItemStack::getCount).reversed());
            }
            itemGrid.setItems(filtered);
            MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
            if (be != null) {
                capacityLabel.setText(Component.literal("Items: " + be.getTotalItemCount()));
            }
        }

        void onItemClick(ItemStack stack, int index) {
            if (menu.getCarried().isEmpty() && !stack.isEmpty()) {
                PacketDistributor.sendToServer(new MagicStoragePacket(index + 10000, new ItemStack(net.minecraft.world.item.Items.WOODEN_AXE)));
                getStorageEntity(minecraft.player).setChanged();
                scheduleRefresh();
            }
        }
    }

    // ========================================================================
    // Craft Panel
    // ========================================================================
    private class CraftPanel extends QWidget {
        private final QLineEdit searchField;
        private final List<QRadioButton> sortButtons;
        private final List<QRadioButton> categoryButtons;
        private final List<QRadioButton> stackButtons;
        private final StationsRowWidget stationsRow;
        private ItemGridWidget craftableGrid;
        private QLabel capacityLabel;
        private final CraftInfoPanel infoPanel;
        private boolean showCraftableOnly = true;
        private int sortMode;
        private String categoryFilter = "All";
        private int stackFilter;

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
            searchField.connect(QLineEdit.TEXT_CHANGED, this, new SlotKeyConsumer<>("cs", (self, v) -> refresh()));
            funcRow.addWidget(searchField, 1);
            leftLayout.addLayout(funcRow);

            QHBoxLayout sortRow = new QHBoxLayout();
            sortRow.setSpacing(1);
            sortButtons = new ArrayList<>();
            for (String label : new String[]{"Default", "By ID", "By Name", "By Count"}) {
                QRadioButton rb = new QRadioButton(Component.literal(label), "css");
                rb.setFixedHeight(14);
                int idx = sortButtons.size();
                int fi = idx;
                rb.connect(QRadioButton.TOGGLED, this, new SlotKeyConsumer<>("cso" + idx, (self, checked) -> {
                    if (checked && craftableGrid != null) { sortMode = fi; refresh(); }
                }));
                sortButtons.add(rb);
                sortRow.addWidget(rb);
            }
            sortButtons.get(0).setChecked(true);
            leftLayout.addLayout(sortRow);

            QHBoxLayout catRow = new QHBoxLayout();
            catRow.setSpacing(1);
            categoryButtons = new ArrayList<>();
            for (String label : new String[]{"All", "Weapon", "Tool", "Material", "Block", "Misc"}) {
                QRadioButton rb = new QRadioButton(Component.literal(label), "ccs");
                rb.setFixedHeight(14);
                String fl = label;
                rb.connect(QRadioButton.TOGGLED, this, new SlotKeyConsumer<>("cca" + label, (self, checked) -> {
                    if (checked && craftableGrid != null) { categoryFilter = fl; refresh(); }
                }));
                categoryButtons.add(rb);
                catRow.addWidget(rb);
            }
            leftLayout.addLayout(catRow);

            QHBoxLayout stackRow = new QHBoxLayout();
            stackRow.setSpacing(1);
            stackButtons = new ArrayList<>();
            for (String label : new String[]{"All", "Stackable", "Non-stackable"}) {
                QRadioButton rb = new QRadioButton(Component.literal(label), "cck");
                rb.setFixedHeight(14);
                int idx = stackButtons.size();
                int fi = idx;
                rb.connect(QRadioButton.TOGGLED, this, new SlotKeyConsumer<>("cst" + idx, (self, checked) -> {
                    if (checked && craftableGrid != null) { stackFilter = fi; refresh(); }
                }));
                stackButtons.add(rb);
                stackRow.addWidget(rb);
            }
            leftLayout.addLayout(stackRow);

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

            infoPanel = new CraftInfoPanel(this);
            infoPanel.setFixedWidth(180);
            mainLayout.addWidget(infoPanel);

            sortButtons.get(0).setChecked(true);
            categoryButtons.get(0).setChecked(true);
            stackButtons.get(0).setChecked(true);

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
            List<ItemStack> display = new ArrayList<>();
            for (Pair<ItemStack, RecipeHolder<?>> p : cachedResults) {
                ItemStack s = p.getA();
                if (!search.isEmpty() && !s.getDisplayName().getString().toLowerCase().contains(search)) continue;
                if (!categoryFilter.equals("All")) {
                    String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
                    if (categoryFilter.equals("Weapon") && !id.contains("sword") && !id.contains("bow") && !id.contains("crossbow") && !id.contains("trident")) continue;
                    if (categoryFilter.equals("Tool") && !id.contains("pickaxe") && !id.contains("axe") && !id.contains("shovel") && !id.contains("hoe")) continue;
                    if (categoryFilter.equals("Block") && !(s.getItem() instanceof BlockItem)) continue;
                    if (categoryFilter.equals("Material") && (s.getItem() instanceof BlockItem)) continue;
                }
                if (stackFilter == 1 && !s.isStackable()) continue;
                if (stackFilter == 2 && s.isStackable()) continue;
                display.add(s);
            }
            craftableGrid.setItems(display);
            stationsRow.refresh();
            updateCapacity();
        }

        void updateCapacity() {
            MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
            if (be != null) {
                capacityLabel.setText(Component.literal("Items: " + be.getTotalItemCount()));
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
        protected void mousePressEvent(QMouseEvent event) {
            if (event.button() == QMouseEvent.Button.Left) {
                int slot = 32, gap = 2;
                int idx = (event.x() + scrollOffset) / (slot + gap);
                if (idx >= 0 && idx < stations.size()) {
                    if (idx < stations.size() - 1) {
                        PacketDistributor.sendToServer(new MagicStoragePacket(20000 + idx, ItemStack.EMPTY));
                    } else {
                        ItemStack held = menu.getCarried();
                        if (!held.isEmpty() && held.getItem() instanceof BlockItem bi && CraftConfig.isEnabledBlock(bi.getBlock())) {
                            PacketDistributor.sendToServer(new MagicStoragePacket(1, held));
                        }
                    }
                    var be = Util.getStorageEntity(minecraft.player);
                    if (be != null) be.setChanged();
                    scheduleRefresh();
                }
                event.accept();
            }
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
        private int hoverIndex = -1;
        private int cols = 8;
        private int slotSize = 18;
        private java.util.function.BiConsumer<ItemStack, Integer> clickHandler;

        ItemGridWidget() {
            setFocusPolicy(com.github.edg_thexu.qtcraft_api.core.widget.QWidget.FocusPolicy.NoFocus);
        }

        void setItems(List<ItemStack> items) {
            this.items = items;
            updateCols();
            update();
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
            if (event.button() == QMouseEvent.Button.Left) {
                updateCols();
                int col = event.x() / slotSize, row = event.y() / slotSize;
                int idx = row * cols + col;
                if (idx >= 0 && idx < items.size() && !items.get(idx).isEmpty() && clickHandler != null) {
                    clickHandler.accept(items.get(idx), idx);
                }
                event.accept();
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
            for (Ingredient ing : ings) {
                if (ing.getItems().length > 0) ingredients.add(ing.getItems()[0].copy());
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
            renderSlot(p, 6, y, 32, outputItem, false); y += 38;

            p.drawText("Ingredients:", 6, y); y += 11;
            int ix = 6;
            for (ItemStack ing : ingredients) {
                boolean miss = !parent.haveIngredients.containsKey(ing.getItem()) || parent.haveIngredients.get(ing.getItem()) < ing.getCount();
                renderSlot(p, ix, y, ss, ing, false);
                if (miss) p.fillRect(ix, y, ss, ss, new QColor(0x44FF0000));
                ix += ss + 2;
                if (ix > width() - ss) { ix = 6; y += ss + 2; }
            }
            if (!ingredients.isEmpty()) y += ss + 6;

            p.drawText("Stations:", 6, y); y += 11; ix = 6;
            for (ItemStack st : requiredStations) { renderSlot(p, ix, y, ss, st, false); ix += ss + 2; if (ix > width() - ss) { ix = 6; y += ss + 2; } }
            if (!requiredStations.isEmpty()) y += ss + 6;

            p.drawText("In Storage:", 6, y); y += 11; ix = 6;
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

    private void refreshPanels() {
        if (storagePanel != null) storagePanel.refresh();
        if (craftPanel != null) { craftPanel.refresh(); craftPanel.stationsRow.refresh(); }
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
        // Shift-click on inventory slots triggers quick-move to storage
        if (hasShiftDown()) scheduleRefresh();
        // Store action: check BEFORE widget delegate so it can't be blocked
        if (!menu.getCarried().isEmpty() && storageWin != null && storageWin.isVisible()) {
            int sx = storageWin.x(), sy = storageWin.y(), sw = storageWin.width(), sh = storageWin.height();
            if (mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + sh) {
                PacketDistributor.sendToServer(new MagicStoragePacket(0, menu.getCarried()));
                getStorageEntity(minecraft.player).setChanged();
                scheduleRefresh();
                return true;
            }
        }
        if (widgetDelegate.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {}
}
