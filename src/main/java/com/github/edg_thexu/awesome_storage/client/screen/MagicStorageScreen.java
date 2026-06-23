package com.github.edg_thexu.awesome_storage.client.screen;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.api.adapter.CommonRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.filter.FilterRuleRegistry;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.block.StorageOnlyBlock;
import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import com.github.edg_thexu.awesome_storage.core.menu.MagicStorageMenu;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicCraftPacket;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicStoragePacket;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.client.screen.QContainerWidgetScreen;
import com.github.edg_thexu.qtcraft_api.core.QSizePolicy;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QWheelEvent;
import com.github.edg_thexu.qtcraft_api.core.geometry.QSize;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.slots.SlotKeyConsumer;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.button.QPushButton;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QMainWindow;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QComboBox;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QLineEdit;
import com.github.edg_thexu.qtcraft_api.util.WidgetTooltip;
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

import static com.github.edg_thexu.awesome_storage.utils.Util.getStorageEntity;
import static com.github.edg_thexu.awesome_storage.utils.Util.getStorageItems;
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
        renderSlot(p, x, y, size, stack, highlight, false);
    }

    private static void renderSlot(QPainter p, int x, int y, int size, ItemStack stack, boolean highlight, boolean overlay) {
        p.fillRect(x, y, size, size, new QColor(0xFF333333));
        if (overlay) p.fillRect(x, y, size, size, new QColor(0x55FF0000));
        if (highlight) p.fillRect(x, y, size, size, new QColor(0x55FFFFFF));
        if (!stack.isEmpty()) {
            p.renderItemStack(stack, x + (size - 16) / 2, y + (size - 16) / 2);
            p.renderItemDecorations(stack, x + (size - 16) / 2, y + (size - 16) / 2);
        }
    }

    // ========================================================================
    // Shared filter bar (sort / category / stack / mod + filter + sort logic)
    // ========================================================================
    private static class FilterBar {
        final QComboBox sortCombo;
        final QComboBox categoryCombo;
        final QComboBox stackCombo;
        final QComboBox modCombo;

        FilterBar(QHBoxLayout row, Object owner, String keyPrefix, Runnable onRefresh) {
            sortCombo = new QComboBox();
            sortCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
            sortCombo.addItem("Default");
            for (var rule : FilterRuleRegistry.getSortRules()) sortCombo.addItem(rule.name());
            sortCombo.setFixedHeight(16);
            sortCombo.connect(QComboBox.CURRENT_INDEX_CHANGED, owner, new SlotKeyConsumer<>(keyPrefix + "s", (self, idx) -> onRefresh.run()));
            row.addWidget(sortCombo, 1);

            categoryCombo = new QComboBox();
            categoryCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
            categoryCombo.addItem("All");
            for (var rule : FilterRuleRegistry.getCategoryRules()) categoryCombo.addItem(rule.name());
            categoryCombo.setFixedHeight(16);
            categoryCombo.connect(QComboBox.CURRENT_INDEX_CHANGED, owner, new SlotKeyConsumer<>(keyPrefix + "c", (self, idx) -> onRefresh.run()));
            row.addWidget(categoryCombo, 1);

            stackCombo = new QComboBox();
            stackCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
            stackCombo.addItem("All"); stackCombo.addItem("Stackable"); stackCombo.addItem("Non-stackable");
            stackCombo.setFixedHeight(16);
            stackCombo.connect(QComboBox.CURRENT_INDEX_CHANGED, owner, new SlotKeyConsumer<>(keyPrefix + "t", (self, idx) -> onRefresh.run()));
            row.addWidget(stackCombo, 1);

            modCombo = new QComboBox();
            modCombo.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Fixed));
            modCombo.addItem("All Mods");
            java.util.TreeSet<String> allMods = new java.util.TreeSet<>();
            for (var item : BuiltInRegistries.ITEM) {
                allMods.add(BuiltInRegistries.ITEM.getKey(item).getNamespace());
            }
            for (String mod : allMods) modCombo.addItem(mod);
            modCombo.setFixedHeight(16);
            modCombo.connect(QComboBox.CURRENT_INDEX_CHANGED, owner, new SlotKeyConsumer<>(keyPrefix + "m", (self, idx) -> onRefresh.run()));
            row.addWidget(modCombo, 1);
        }

        List<ItemStack> apply(List<ItemStack> items, String search) {
            String catFilter = categoryCombo.currentText();
            String sortText = sortCombo.currentText();
            String stackText = stackCombo.currentText();
            String modFilter = modCombo.currentText();

            List<ItemStack> filtered = new ArrayList<>();
            for (ItemStack s : items) {
                if (s.isEmpty()) continue;
                if (!search.isEmpty() && !s.getDisplayName().getString().toLowerCase().contains(search)) continue;
                // Category filter (registered rules)
                if (!catFilter.equals("All")) {
                    boolean match = false;
                    for (var rule : FilterRuleRegistry.getCategoryRules()) {
                        if (catFilter.equals(rule.name()) && rule.predicate().test(s)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) continue;
                }
                if (stackText.equals("Stackable") && !s.isStackable()) continue;
                if (stackText.equals("Non-stackable") && s.isStackable()) continue;
                if (!modFilter.equals("All Mods") && !BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals(modFilter)) continue;
                filtered.add(s);
            }
            // Sort by registered rule
            if (!sortText.equals("Default")) {
                for (var rule : FilterRuleRegistry.getSortRules()) {
                    if (sortText.equals(rule.name())) {
                        filtered.sort(rule.comparator());
                        break;
                    }
                }
            }
            return filtered;
        }
    }

    // ========================================================================
    // Storage Panel
    // ========================================================================
    private class StoragePanel extends QWidget {
        private final QLineEdit searchField;
        private final FilterBar filterBar;
        ItemGridWidget itemGrid;
        private final QLabel capacityLabel;
        private final QSmoothScrollArea scrollArea;

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

            QHBoxLayout filterRow = new QHBoxLayout();
            filterRow.setSpacing(2);
            filterBar = new FilterBar(filterRow, this, "s", this::refresh);
            vl.addLayout(filterRow);

            scrollArea = new QSmoothScrollArea();
            itemGrid = new ItemGridWidget();
            itemGrid.setClickHandler(this::onItemClick);
            scrollArea.setWidget(itemGrid);
            scrollArea.setWidgetResizable(true);
            vl.addWidget(scrollArea, 1);

            capacityLabel = new QLabel(Component.literal("Capacity: 0/0"));
            vl.addWidget(capacityLabel);

            refresh();
        }

        void refresh() {
            List<ItemStack> items = getStorageItems(minecraft.player);
            if (items == null) return;
            List<ItemStack> filtered = filterBar.apply(items, searchField.text().toLowerCase());
            itemGrid.setItems(filtered);
            scrollArea.updateLayout();
            scrollArea.markDirty();
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
        private final FilterBar filterBar;
        private final StationsRowWidget stationsRow;
        ItemGridWidget craftableGrid;
        private final QLabel capacityLabel;
        private final CraftInfoPanel infoPanel;
        private final QSmoothScrollArea craftableScrollArea;
        private boolean showCraftableOnly = true;
        private final List<Integer> craftDisplayIndex = new ArrayList<>();
        private final QLabel qtyLabelCtrl;
        private final QPushButton craftBtnCtrl;
        private final com.github.edg_thexu.qtcraft_api.core.widget.info.QItemWidget takeItemCtrl;
        private final QLabel takeLabelRef;


        List<ItemStack> results = new ArrayList<>();
        List<Pair<ItemStack, RecipeHolder<?>>> cachedResults = new ArrayList<>();
        Map<RecipeHolder<?>, AbstractMagicCraftRecipeAdapter> recipeMap = new HashMap<>();
        Map<ItemStack, Integer> haveIngredients = new HashMap<>();
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

            // Filter row: sort / category / stack / mod
            QHBoxLayout filterRow = new QHBoxLayout();
            filterRow.setSpacing(2);
            filterBar = new FilterBar(filterRow, this, "c", () -> { if (craftableGrid != null) refresh(); });
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
            craftableScrollArea = area;

            capacityLabel = new QLabel(Component.literal("Capacity: 0/0"));
            leftLayout.addWidget(capacityLabel);

            mainLayout.addWidget(leftSide, 1);

            QSmoothScrollArea scrollArea = new QSmoothScrollArea();
            infoPanel = new CraftInfoPanel(this);
            infoPanel.setFixedWidth(120);
            scrollArea.setFixedWidth(130);
            scrollArea.setWidget(infoPanel);
            scrollArea.setWidgetResizable(true);

            // Control buttons below info panel
            qtyLabelCtrl = new QLabel(Component.literal("x1   "));
            qtyLabelCtrl.setTextColor(new QColor(0xFFFFAA00));
            craftBtnCtrl = new QPushButton(Component.literal("Craft"));
            craftBtnCtrl.setFixedHeight(14);
            craftBtnCtrl.setOnClick(() -> {
                if (infoPanel.hasRecipe && infoPanel.parent.selectedRecipe != null && infoPanel.parent.selectedAdapter != null) {
                    int count = Math.max(1, infoPanel.craftQuantity);
                    List<ItemStack> excluded = new ArrayList<>(infoPanel.excludedItems);
                    for (int i = 0; i < count; i++)
                        PacketDistributor.sendToServer(new MagicCraftPacket(infoPanel.parent.selectedRecipe.id(), BuiltInRegistries.RECIPE_TYPE.getKey(infoPanel.parent.selectedAdapter.getRecipe()), excluded));
                    var be = Util.getStorageEntity(minecraft.player);
                    if (be != null) be.setChanged();
                    updateTakeLabel();
                    scheduleRefresh();
                }
            });
            // Take: rendered item + count
            QWidget takeArea = new QWidget();
            QVBoxLayout takeL = new QVBoxLayout(takeArea);
            takeL.setSpacing(0);
            takeL.setContentsMargins(0, 0, 0, 0);
            QLabel takeLabel = new QLabel(Component.literal("x0"));
            takeLabel.setTextColor(new QColor(0xFFFFAA00));
            takeLabel.setFixedHeight(10);
            takeItemCtrl = new com.github.edg_thexu.qtcraft_api.core.widget.info.QItemWidget() {
                @Override protected void mousePressEvent(QMouseEvent e) { e.accept(); infoPanel.doTake(); }
            };
            takeItemCtrl.setFixedSize(18, 18);
            takeL.addWidget(takeItemCtrl);
            takeL.addWidget(takeLabel);

            QPushButton p1 = new QPushButton(Component.literal("+1")), p10 = new QPushButton(Component.literal("+10")), p100 = new QPushButton(Component.literal("+100"));
            QPushButton m1 = new QPushButton(Component.literal("-1")), m10 = new QPushButton(Component.literal("-10")), m100 = new QPushButton(Component.literal("-100"));
            QPushButton maxB = new QPushButton(Component.literal("Max")), rstB = new QPushButton(Component.literal("Reset"));
            for (QPushButton b : new QPushButton[]{p1, p10, p100, m1, m10, m100, maxB, rstB}) b.setFixedHeight(12);
            p1.setOnClick(() -> {infoPanel.clampedAdd(1); qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));});
            p10.setOnClick(() -> {infoPanel.clampedAdd(10); qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));});
            p100.setOnClick(() -> {infoPanel.clampedAdd(100); qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));});
            m1.setOnClick(() -> { infoPanel.craftQuantity = Math.max(1, infoPanel.craftQuantity - 1); qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));});
            m10.setOnClick(() -> { infoPanel.craftQuantity = Math.max(1, infoPanel.craftQuantity - 10); qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));});
            m100.setOnClick(() -> { infoPanel.craftQuantity = Math.max(1, infoPanel.craftQuantity - 100); qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));});
            maxB.setOnClick(() -> { infoPanel.craftQuantity = infoPanel.getMaxCraftable(); qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));});
            rstB.setOnClick(() -> { infoPanel.craftQuantity = 1; qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));});

            // Right side: scroll area + controls
            QWidget rightSide = new QWidget();
            QVBoxLayout rvl = new QVBoxLayout(rightSide);
            rvl.setSpacing(1);

            // Take item widget at bottom (above the scroll)
            rvl.addWidget(scrollArea, 1);

            // Top row: left = qty+craft, right = take
            QHBoxLayout topRow = new QHBoxLayout();
            topRow.addWidget(qtyLabelCtrl);
            topRow.addWidget(craftBtnCtrl, 0, QLayout.ALIGN_CENTER);
            topRow.addWidget(takeArea, 1);
            rvl.addLayout(topRow);

            // Row: +1 +10 +100
            QHBoxLayout incRow = new QHBoxLayout(); incRow.setSpacing(2);
            incRow.addWidget(p1); incRow.addWidget(p10); incRow.addWidget(p100);
            rvl.addLayout(incRow);

            // Row: -1 -10 -100
            QHBoxLayout decRow = new QHBoxLayout(); decRow.setSpacing(2);
            decRow.addWidget(m1); decRow.addWidget(m10); decRow.addWidget(m100);
            rvl.addLayout(decRow);

            // Row: Max Reset
            QHBoxLayout optRow = new QHBoxLayout(); optRow.setSpacing(2);
            optRow.addWidget(maxB); optRow.addWidget(rstB);
            rvl.addLayout(optRow);


            takeLabelRef = takeLabel;
            mainLayout.addWidget(rightSide);

            try { reloadRecipes(); } catch (Exception e) { e.printStackTrace(); }
            try { refresh(); } catch (Exception e) { e.printStackTrace(); }
        }

        void reloadRecipes() {
            MagicStorageBlockEntity storage = getStorageEntity(minecraft.player);
            if (storage == null || minecraft.level == null) return;
            results.clear();
            recipeMap.clear();
            Set<Block> accessors = storage.getBlock_accessors().stream()
                    .map(s -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s))).collect(Collectors.toSet());
            for (var entry : BuiltInRegistries.RECIPE_TYPE.entrySet()) {
                RecipeType<?> rt = entry.getValue();
                if (CraftConfig.ENABLED_RECIPES.containsKey(rt)) {
                    boolean accept = accessors.containsAll(CraftConfig.ENABLED_RECIPES.get(rt));
                    if (accept) {
                        AbstractMagicCraftRecipeAdapter<RecipeInput, Recipe<RecipeInput>> adapter =
                                AdapterManager.Adapters.containsKey(rt)
                                        ? AdapterManager.Adapters.get(rt)
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
            for (ItemStack s : have) {
                boolean merged = false;
                for (Map.Entry<ItemStack, Integer> entry : haveIngredients.entrySet()) {
                    if (ItemStack.isSameItemSameComponents(entry.getKey(), s)) {
                        haveIngredients.put(entry.getKey(), entry.getValue() + s.getCount());
                        merged = true;
                        break;
                    }
                }
                if (!merged) haveIngredients.put(s.copy(), s.getCount());
            }

            List<Pair<ItemStack, RecipeHolder<?>>> craftable = new ArrayList<>();
            List<Pair<ItemStack, RecipeHolder<?>>> partial = new ArrayList<>();
            for (Map.Entry<RecipeHolder<?>, AbstractMagicCraftRecipeAdapter> e : recipeMap.entrySet()) {
                var adapter = e.getValue();
                NonNullList<Ingredient> ings = adapter.getIngredients(e.getKey());
                boolean can = true, has = false;
                for (Ingredient ing : ings) {
                    if (ing.isEmpty()) continue;
                    int req = ing.getItems().length == 0 ? 1 : ing.getItems()[0].getCount();
                    int avail = 0;
                    for (Map.Entry<ItemStack, Integer> entry : haveIngredients.entrySet()) {
                        if (ing.test(entry.getKey())) avail += entry.getValue();
                    }
                    if (avail >= req) has = true;
                    else can = false;
                }
                Pair<ItemStack, RecipeHolder<?>> p = new Pair<>(adapter.getResult(e.getKey()), e.getKey());
                if (can) craftable.add(p);
                else if (has) partial.add(p);
            }
            craftable.sort(Comparator.comparing(a -> a.getA().getDisplayName().getString()));
            partial.sort(Comparator.comparing(a -> a.getA().getDisplayName().getString()));
            cachedResults = new ArrayList<>(craftable);
            if (!showCraftableOnly) cachedResults.addAll(partial);

            List<ItemStack> resultItems = cachedResults.stream().map(p -> p.getA()).collect(Collectors.toList());
            List<ItemStack> filtered = filterBar.apply(resultItems, searchField.text().toLowerCase());
            List<ItemStack> display = new ArrayList<>();
            for (ItemStack fs : filtered) {
                for (int ci = 0; ci < cachedResults.size(); ci++) {
                    if (ItemStack.isSameItemSameComponents(fs, cachedResults.get(ci).getA())) {
                        display.add(cachedResults.get(ci).getA());
                        break;
                    }
                }
            }
            // Map display indices to cachedResults indices, and compute craftable overlays
            craftDisplayIndex.clear();
            List<Boolean> overlays = new ArrayList<>();
            for (ItemStack ds : display) {
                int found = -1;
                for (int ci = 0; ci < cachedResults.size(); ci++) {
                    if (ItemStack.isSameItemSameComponents(ds, cachedResults.get(ci).getA())) {
                        found = ci; break;
                    }
                }
                craftDisplayIndex.add(found);
                // Red overlay for items that are in the "partial" (non-craftable) portion
                overlays.add(found >= craftable.size());
            }
            craftableGrid.setItems(display, overlays);
            // Force scroll area to recalculate scrollbar bounds
            if (craftableScrollArea != null) {
                craftableScrollArea.updateLayout();
                craftableScrollArea.markDirty();
            }
            stationsRow.refresh();
            updateCapacity();
            updateTakeLabel();
        }

        void updateCapacity() {
            MagicStorageBlockEntity be = Util.getStorageEntity(minecraft.player);
            if (be != null) {
                capacityLabel.setText(Component.literal("Capacity:" + be.getUsedSlots() + "/" + be.getTotalSlots()));
            }
        }

        private int countByItem(Item item) {
            int total = 0;
            for (Map.Entry<ItemStack, Integer> e : haveIngredients.entrySet()) {
                if (e.getKey().getItem() == item) total += e.getValue();
            }
            return total;
        }

        void updateTakeLabel() {
            if (takeLabelRef != null && infoPanel != null && infoPanel.outputItem != null) {
                int total = countByItem(infoPanel.outputItem.getItem());
                takeLabelRef.setText(Component.literal("x" + total));
                if (takeItemCtrl != null) {
                    var newStack = infoPanel.outputItem.copy();
                    newStack.setCount(1);
                    takeItemCtrl.setItemStack(newStack);
                }
            }
        }

        void onCraftClick(ItemStack stack, int index) {
            int realIdx = (index >= 0 && index < craftDisplayIndex.size()) ? craftDisplayIndex.get(index) : -1;
            if (realIdx >= 0 && realIdx < cachedResults.size()) {
                var pair = cachedResults.get(realIdx);
                selectedRecipe = pair.getB();
                selectedAdapter = recipeMap.get(selectedRecipe);
                if (selectedRecipe != null && selectedAdapter != null) {
                    infoPanel.showRecipe(selectedRecipe, selectedAdapter, pair.getA(), haveIngredients);

                    if (qtyLabelCtrl != null) qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));
                    int totalH = countByItem(stack.getItem());
                    if (takeItemCtrl != null) { var newStack = stack.copy(); newStack.setCount(1); takeItemCtrl.setItemStack(newStack); takeItemCtrl.setVisible(true); }
                    if (takeLabelRef != null) takeLabelRef.setText(Component.literal("x" + totalH));
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
        protected void mousePressEvent(QMouseEvent event) {
            if (event.button() == QMouseEvent.Button.Left) {
                int slot = 14, gap = 1;
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
                    // Schedule recipe reload — it will trigger after server syncs accessors
                    lastAccessors = new ArrayList<>();
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
        private final List<Integer> storageIndices = new ArrayList<>();
        private List<Boolean> overlayFlags = new ArrayList<>();
        private int hoverIndex = -1;

        @Override
        public WidgetTooltip toolTip() {
            if (hoverIndex >= 0 && hoverIndex < items.size() && !items.get(hoverIndex).isEmpty()) {
                return WidgetTooltip.create(items.get(hoverIndex));
            }
            return null;
        }
        private int cols = 8;
        private final int slotSize = 18;
        private java.util.function.BiConsumer<ItemStack, Integer> clickHandler;

        ItemGridWidget() {
            setFocusPolicy(com.github.edg_thexu.qtcraft_api.core.widget.QWidget.FocusPolicy.NoFocus);
        }

        void setItems(List<ItemStack> items) { setItems(items, null); }

        void setItems(List<ItemStack> items, List<Boolean> overlays) {
            this.items = items;
            this.overlayFlags = overlays != null ? overlays : new ArrayList<>();
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
                boolean over = i < overlayFlags.size() && overlayFlags.get(i);
                renderSlot(p, col * slotSize, row * slotSize, slotSize, items.get(i), i == hoverIndex, over);
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
        private final List<ItemStack> ingredients = new ArrayList<>();
        private final List<ItemStack> requiredStations = new ArrayList<>();
        private boolean hasRecipe;
        private int craftQuantity = 1;

        // The raw ingredients list for proper Ingredient.test() matching
        private NonNullList<Ingredient> ings = NonNullList.create();
        // Item stacks excluded from crafting (matched by isSameItemSameComponents)
        private final Set<ItemStack> excludedItems = new HashSet<>();
        // Items currently displayed in "In Storage" section and their slot positions
        private final List<ItemStack> storageItems = new ArrayList<>();
        private final int[] storageSlotX = new int[8];
        private final int[] storageSlotY = new int[8];
        // Tooltip tracking for all rendered slots
        private ItemStack hoveredStack = ItemStack.EMPTY;
        private final List<ItemStack> hoverSlots = new ArrayList<>();
        private final List<Integer> hoverSlotX = new ArrayList<>();
        private final List<Integer> hoverSlotY = new ArrayList<>();
        private final List<Integer> hoverSlotSize = new ArrayList<>();
        // Cycled ingredient display
        private int cycleIndex = 0;
        private long lastCycleTime = 0;


        CraftInfoPanel(CraftPanel parent) {
            this.parent = parent;
            setMinimumSize(180, 200);
            setFixedSize(180, 300);

        }

        private int getMaxCraftable() {
            // Group ingredient slots by first item (same as ingredient display)
            java.util.LinkedHashMap<Item, Integer> needPerCraft = new java.util.LinkedHashMap<>();
            java.util.LinkedHashMap<Item, Ingredient> ingByItem = new java.util.LinkedHashMap<>();
            for (Ingredient ing : ings) {
                if (ing.getItems().length == 0) continue;
                Item item = ing.getItems()[0].getItem();
                needPerCraft.merge(item, 1, Integer::sum);
                ingByItem.putIfAbsent(item, ing);
            }
            int maxP = Integer.MAX_VALUE;
            for (java.util.Map.Entry<Item, Integer> e : needPerCraft.entrySet()) {
                Ingredient ing = ingByItem.get(e.getKey());
                int need = e.getValue();
                int avail = 0;
                for (Map.Entry<ItemStack, Integer> h : parent.haveIngredients.entrySet()) {
                    if (!ing.test(h.getKey())) continue;
                    if (isExcluded(h.getKey())) continue;
                    avail += h.getValue();
                }
                if (need > 0) maxP = Math.min(maxP, avail / need);
            }
            return Math.max(1, maxP);
        }

        private boolean isExcluded(ItemStack stack) {
            for (ItemStack ex : excludedItems) {
                if (ItemStack.isSameItemSameComponents(stack, ex)) return true;
            }
            return false;
        }

        void rebuildIngredients() {
            ingredients.clear();
            java.util.LinkedHashMap<Item, Integer> merged = new java.util.LinkedHashMap<>();
            java.util.LinkedHashMap<Item, Ingredient> ingByItem = new java.util.LinkedHashMap<>();
            for (Ingredient ing : ings) {
                if (ing.getItems().length > 0) {
                    Item item = ing.getItems()[0].getItem();
                    merged.merge(item, 1, Integer::sum);
                    ingByItem.putIfAbsent(item, ing);
                }
            }
            for (java.util.Map.Entry<Item, Integer> e : merged.entrySet()) {
                Ingredient ing = ingByItem.get(e.getKey());
                ItemStack[] variants = ing.getItems();
                ItemStack display = variants[cycleIndex % variants.length].copy();
                display.setCount(e.getValue());
                ingredients.add(display);
            }
        }

        void tickCycles() {
            long now = System.currentTimeMillis();
            if (now - lastCycleTime > 1000) {
                cycleIndex++;
                lastCycleTime = now;
                rebuildIngredients();
                markDirty();
                update();
            }
        }

        private void clampedAdd(int delta) {
            craftQuantity = Math.max(1, Math.min(getMaxCraftable(), craftQuantity + delta));
        }


        private void doTake() {
            List<ItemStack> stored = getStorageItems(minecraft.player);
            if (stored == null || outputItem.isEmpty()) return;
            // Server-side takeItem(index) uses its own getStoredItems() list and matches by
            // isSameItemSameComponents — we just need the correct index for the item type.
            for (int i = 0; i < stored.size(); i++) {
                if (stored.get(i).getItem() == outputItem.getItem()) {
                    PacketDistributor.sendToServer(new MagicStoragePacket(i + 10000, stored.get(i).copy()));
                    var be = Util.getStorageEntity(minecraft.player);
                    if (be != null) be.setChanged();
                    scheduleRefresh();
                    break;
                }
            }
        }


        @SuppressWarnings("unchecked")
        void showRecipe(RecipeHolder<?> recipe, AbstractMagicCraftRecipeAdapter adapter, ItemStack output, Map<ItemStack, Integer> have) {
            outputItem = output;
            ingredients.clear();
            requiredStations.clear();
            excludedItems.clear();
            craftQuantity = 1;

            var casted = (AbstractMagicCraftRecipeAdapter<RecipeInput, Recipe<RecipeInput>>) adapter;
            ings = casted.getIngredients((RecipeHolder<Recipe<RecipeInput>>) (Object) recipe);
            rebuildIngredients();
            if (CraftConfig.ENABLED_RECIPES.containsKey(adapter.getRecipe())) {
                java.util.Set<Block> seen = new java.util.HashSet<>();
                for (Block b : CraftConfig.ENABLED_RECIPES.get(adapter.getRecipe())) {
                    if (seen.add(b)) requiredStations.add(new ItemStack(b));
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

            if (!hasRecipe) {
                p.setColor(new QColor(0xFF888888)); p.drawText("Select an item", 8, 20);
                return;
            }

            hoverSlots.clear();
            hoverSlotX.clear();
            hoverSlotY.clear();
            hoverSlotSize.clear();

            int y = 6, ss = 18, cw = width();

            p.setColor(new QColor(0xFFFFAA00));
            p.drawText("Output:", 6, y); y += 11;
            renderSlot(p, 6, y, 20, outputItem, false);
            hoverSlots.add(outputItem); hoverSlotX.add(6); hoverSlotY.add(y); hoverSlotSize.add(20);
            y += 38;

            p.setColor(QColor.WHITE);
            p.drawText("Ingredients:", 6, y); y += 11;
            int ix = 6;
            for (ItemStack ing : ingredients) {
                int need = ing.getCount() * Math.max(1, craftQuantity);
                // Find the corresponding Ingredient for Ingredient.test() matching
                Ingredient matchIng = null;
                for (Ingredient i : ings) {
                    if (!i.isEmpty() && i.getItems().length > 0 && i.getItems()[0].getItem() == ing.getItem()) {
                        matchIng = i;
                        break;
                    }
                }
                int have = 0;
                if (matchIng != null) {
                    for (Map.Entry<ItemStack, Integer> e : parent.haveIngredients.entrySet()) {
                        if (isExcluded(e.getKey())) continue;
                        if (matchIng.test(e.getKey())) have += e.getValue();
                    }
                }
                boolean miss = have < need;
                renderSlot(p, ix, y, ss, ing, false);
                hoverSlots.add(ing); hoverSlotX.add(ix); hoverSlotY.add(y); hoverSlotSize.add(ss);
                if (miss) p.fillRect(ix, y, ss, ss, new QColor(0x44FF0000));
                ix += ss + 2;
                if (ix > cw - ss) { ix = 6; y += ss + 2; }
            }
            if (!ingredients.isEmpty()) y += (ix > 6 ? ss + 6 : 4);

            p.drawText("Stations:", 6, y); y += 11; ix = 6;
            for (ItemStack st : requiredStations) {
                renderSlot(p, ix, y, ss, st, false);
                hoverSlots.add(st); hoverSlotX.add(ix); hoverSlotY.add(y); hoverSlotSize.add(ss);
                ix += ss + 2; if (ix > cw - ss) { ix = 6; y += ss + 2; }
            }
            if (!requiredStations.isEmpty()) y += ss + 6;

            // In Storage: show each component-group that matches any ingredient — red overlay if excluded
            p.drawText("In Storage:", 6, y); y += 11; ix = 6;
            storageItems.clear();
            // Collect matching entries and sort by stable key (registry ID) to prevent position flickering
            java.util.List<java.util.Map.Entry<ItemStack, Integer>> matched = new java.util.ArrayList<>();
            for (java.util.Map.Entry<ItemStack, Integer> e : parent.haveIngredients.entrySet()) {
                for (Ingredient ing : ings) {
                    if (!ing.isEmpty() && ing.test(e.getKey())) {
                        matched.add(e);
                        break;
                    }
                }
            }
            matched.sort(java.util.Comparator.<java.util.Map.Entry<ItemStack, Integer>, String>comparing(
                            e -> BuiltInRegistries.ITEM.getKey(e.getKey().getItem()).toString())
                    .thenComparing(e -> e.getKey().getDisplayName().getString())
                    .thenComparing(e -> e.getKey().getComponentsPatch().hashCode()));
            int drawn = 0;
            for (java.util.Map.Entry<ItemStack, Integer> e : matched) {
                if (drawn >= 8) break;
                ItemStack display = e.getKey().copy();
                display.setCount(Math.min(e.getValue(), 9999));
                boolean excl = isExcluded(e.getKey());
                renderSlot(p, ix, y, ss, display, false, excl);
                hoverSlots.add(display); hoverSlotX.add(ix); hoverSlotY.add(y); hoverSlotSize.add(ss);
                storageItems.add(display);
                storageSlotX[drawn] = ix;
                storageSlotY[drawn] = y;
                ix += ss + 2; if (ix > cw - ss) { ix = 6; y += ss + 2; }
                drawn++;
            }
            if (drawn > 0) y += ss + 6; else y += 4;
            y += 4;

        }

        @Override
        protected void mousePressEvent(QMouseEvent event) {
            if (!hasRecipe || event.button() != QMouseEvent.Button.Left) return;
            // Toggle exclusion on "In Storage" item click (matched by isSameItemSameComponents)
            int mx = event.x(), my = event.y();
            for (int i = 0; i < storageItems.size(); i++) {
                int sx = storageSlotX[i], sy = storageSlotY[i];
                if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                    ItemStack clicked = storageItems.get(i);
                    boolean removed = excludedItems.removeIf(ex -> ItemStack.isSameItemSameComponents(ex, clicked));
                    if (!removed) {
                        excludedItems.add(clicked.copy());
                    }
                    markDirty();
                    update(); event.accept();
                    return;
                }
            }
        }

        @Override
        public WidgetTooltip toolTip() {
            if (!hoveredStack.isEmpty()) {
                return WidgetTooltip.create(hoveredStack);
            }
            return null;
        }

        @Override
        protected void mouseMoveEvent(QMouseEvent event) {
            super.mouseMoveEvent(event);
            int mx = event.x(), my = event.y();
            ItemStack prev = hoveredStack;
            hoveredStack = ItemStack.EMPTY;
            for (int i = 0; i < hoverSlots.size(); i++) {
                int sx = hoverSlotX.get(i), sy = hoverSlotY.get(i), sz = hoverSlotSize.get(i);
                if (mx >= sx && mx < sx + sz && my >= sy && my < sy + sz) {
                    hoveredStack = hoverSlots.get(i);
                    break;
                }
            }
            if (!ItemStack.isSameItemSameComponents(prev, hoveredStack)) {
                markDirty();
                update();
            }
        }

        @Override
        public QWidget childAt(int px, int py) {
            return super.childAt(px, py);
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
            if (craftPanel != null) craftPanel.infoPanel.tickCycles();
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
        // Let QTCraft widgets handle clicks first (grid, stations row, etc.)
        if (widgetDelegate.mouseClicked(mouseX, mouseY, button)) return true;
        // Store action into storage window (carrying item, click anywhere on storage window)
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
