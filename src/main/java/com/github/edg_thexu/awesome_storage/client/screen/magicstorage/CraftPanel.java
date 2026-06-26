package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.api.adapter.CommonRecipeAdapter;
import com.github.edg_thexu.awesome_storage.client.widget.CapacityBar;
import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicCraftPacket;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicStoragePacket;
import com.github.edg_thexu.awesome_storage.core.network.c2s.QueueActionPacket;
import com.github.edg_thexu.awesome_storage.utils.RecipeFavoriteSystem;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.geometry.QPoint;
import com.github.edg_thexu.qtcraft_api.core.geometry.QSize;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.slots.SlotKeyConsumer;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.slots.SlotKeyRunner;
import com.github.edg_thexu.qtcraft_api.core.QObject;
import com.github.edg_thexu.qtcraft_api.core.widget.QAction;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.button.QPushButton;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QMenu;
import com.github.edg_thexu.qtcraft_api.core.widget.container.QSmoothScrollArea;
import com.github.edg_thexu.qtcraft_api.core.widget.info.QLabel;
import com.github.edg_thexu.qtcraft_api.core.widget.input.QLineEdit;
import com.github.edg_thexu.qtcraft_api.util.WidgetTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

// ========================================================================
// Craft Panel
// ========================================================================
class CraftPanel extends QWidget {
    private final MagicStorageScreen parent;
    private final QLineEdit searchField;
    private final FilterBar filterBar;
    private final QWidget filterBarWidget = new QWidget();;
    private final StationsRowWidget stationsRow;
    ItemGridWidget craftableGrid;
    private final CapacityBar capacityBar;
    final CraftInfoPanel infoPanel;
    private final QSmoothScrollArea craftableScrollArea;
    private boolean showCraftableOnly = true;
    private boolean showFavoritesOnly = false;
    private final List<Integer> craftDisplayIndex = new ArrayList<>();
    private final Set<RecipeHolder<?>> craftableSet = new HashSet<>();
    private final QLabel qtyLabelCtrl;
    private final QPushButton craftBtnCtrl;
    private final com.github.edg_thexu.qtcraft_api.core.widget.info.QItemWidget takeItemCtrl;
    private final QLabel takeLabelRef;
    private final QPushButton showMenuBtn;
    private final FavoriteButton favBtn;

    private static boolean ifShowFilter = true;
    private static boolean ifShowStations = true;
    private static boolean ifShowLeftMenu = true;


    List<ItemStack> results = new ArrayList<>();
    List<Pair<ItemStack, RecipeHolder<?>>> cachedResults = new ArrayList<>();
    Map<RecipeHolder<?>, AbstractMagicCraftRecipeAdapter> recipeMap = new HashMap<>();
    Map<ItemStack, Integer> haveIngredients = new HashMap<>();
    RecipeHolder<?> selectedRecipe;
    AbstractMagicCraftRecipeAdapter selectedAdapter;

    CraftPanel(MagicStorageScreen parent) {
        this.parent = parent;
        stationsRow = new StationsRowWidget(parent);

        QHBoxLayout mainLayout = new QHBoxLayout(this);
        mainLayout.setSpacing(3);
        mainLayout.setContentsMargins(3, 3, 3, 3);

        QWidget leftSide = new QWidget();
        QVBoxLayout leftLayout = new QVBoxLayout(leftSide);
        leftLayout.setSpacing(1);

        QHBoxLayout funcRow = new QHBoxLayout();
        funcRow.setSpacing(2);
        showMenuBtn = new QPushButton(Component.translatable("awesome_storage.magic_storage_screen.craftable"));
        showMenuBtn.setFixedHeight(16);
        QMenu menu = new QMenu() {
            @Override
            protected void paintEvent(QPaintEvent event) {
                event.painter().push();
                event.painter().translate(0, 0, 300);
                super.paintEvent(event);
                event.painter().pop();
            }
        };
        menu.setVisible(false);
        {
            QAction favAct = new QAction(Component.translatable("awesome_storage.magic_storage_screen.favorites_only").getString());
            favAct.setCheckable(true);
            favAct.connect(QAction.TRIGGERED, this, new SlotKeyRunner<>("favAct", (self) -> {
                showFavoritesOnly = favAct.isChecked();
                showMenuBtn.setText(Component.translatable(showFavoritesOnly ? "awesome_storage.magic_storage_screen.favorites_only" : (showCraftableOnly ? "awesome_storage.magic_storage_screen.craftable" : "awesome_storage.magic_storage_screen.all")));
                refresh();
            }));
            menu.addAction(favAct);

            menu.addSeparator();

            QMenu filterMenu = new QMenu(Component.translatable("awesome_storage.magic_storage_screen.craftable").getString() + "...") {
                @Override
                protected void paintEvent(QPaintEvent event) {
                    event.painter().push();
                    event.painter().translate(0, 0, 300);
                    super.paintEvent(event);
                    event.painter().pop();
                }
            };
            QAction craftAct = new QAction(Component.translatable("awesome_storage.magic_storage_screen.craftable").getString());
            QAction allAct = new QAction(Component.translatable("awesome_storage.magic_storage_screen.all").getString());
            craftAct.setCheckable(true);
            craftAct.setChecked(true);
            craftAct.connect(QAction.TRIGGERED, this, new SlotKeyRunner<>("craftAct", (self) -> {
                showCraftableOnly = true;
                craftAct.setChecked(true);
                allAct.setChecked(false);
                showMenuBtn.setText(Component.translatable("awesome_storage.magic_storage_screen.craftable"));
                menu.dismiss();
                refresh();
            }));
            filterMenu.addAction(craftAct);
            allAct.setCheckable(true);
            allAct.connect(QAction.TRIGGERED, this, new SlotKeyRunner<>("allAct", (self) -> {
                showCraftableOnly = false;
                craftAct.setChecked(false);
                allAct.setChecked(true);
                showMenuBtn.setText(Component.translatable("awesome_storage.magic_storage_screen.all"));
                menu.dismiss();
                refresh();
            }));
            filterMenu.addAction(allAct);
            menu.addMenu(filterMenu);

            QMenu displayMenu = new QMenu(Component.translatable("awesome_storage.magic_storage_screen.display").getString() + "...") {
                @Override
                protected void paintEvent(QPaintEvent event) {
                    event.painter().push();
                    event.painter().translate(0, 0, 300);
                    super.paintEvent(event);
                    event.painter().pop();
                }
            };
            QAction hideFilter = new QAction(Component.translatable("awesome_storage.magic_storage_screen.filter").getString());
            hideFilter.setCheckable(true);
            hideFilter.setChecked(ifShowFilter);
            hideFilter.connect(QAction.TRIGGERED, this, new SlotKeyRunner<>("hideFilter", (self) -> {
                ifShowFilter = hideFilter.isChecked();
                this.filterBarWidget.setVisible(ifShowFilter);
                menu.dismiss();
            }));
            displayMenu.addAction(hideFilter);
            QAction hideStations = new QAction(Component.translatable("awesome_storage.magic_storage_screen.stations").getString());
            hideStations.setCheckable(true);
            hideStations.setChecked(ifShowStations);
            hideStations.connect(QAction.TRIGGERED, this, new SlotKeyRunner<>("hideStations", (self) -> {
                ifShowStations = hideStations.isChecked();
                this.stationsRow.setVisible(ifShowStations);
                menu.dismiss();
            }));
            displayMenu.addAction(hideStations);
            QAction hideLeftMenu = new QAction(Component.translatable("awesome_storage.magic_storage_screen.leftmenu").getString());
            hideLeftMenu.setCheckable(true);
            hideLeftMenu.setChecked(ifShowLeftMenu);
            hideLeftMenu.connect(QAction.TRIGGERED, this, new SlotKeyRunner<>("hideStations", (self) -> {
                ifShowLeftMenu = hideLeftMenu.isChecked();
                parent.craftWin.leftMenu.setVisible(ifShowLeftMenu);
                menu.dismiss();
            }));
            displayMenu.addAction(hideLeftMenu);
            menu.addMenu(displayMenu);

            this.filterBarWidget.setVisible(ifShowFilter);
            this.stationsRow.setVisible(ifShowStations);
            parent.craftWin.leftMenu.setVisible(ifShowLeftMenu);

        }
        showMenuBtn.setOnClick(() -> {
            if(menu.isVisible()) {
                menu.dismiss();
            } else {
                menu.popup(showMenuBtn.mapToGlobal(QPoint.ZERO).x(), showMenuBtn.mapToGlobal(QPoint.ZERO).y() + showMenuBtn.height(), this);
            }
        });
        funcRow.addWidget(showMenuBtn);
        searchField = new QLineEdit();
        searchField.setPlaceholderText(Component.translatable("awesome_storage.magic_storage_screen.search").getString());
        searchField.setFixedHeight(16);
        searchField.connect(QLineEdit.TEXT_CHANGED, this, new SlotKeyConsumer<>("cs", (self, v) -> refresh()));
        funcRow.addWidget(searchField, 1);
        leftLayout.addLayout(funcRow);

        // Filter row: sort / category / stack / mod

        QHBoxLayout filterRow = new QHBoxLayout(filterBarWidget);
        filterRow.setSpacing(2);
        filterBar = new FilterBar(filterRow, this, "c", () -> {
            if (craftableGrid != null) refresh();
        });
        leftLayout.addWidget(filterBarWidget);


        stationsRow.setFixedHeight(16);
        leftLayout.addWidget(stationsRow);

        QSmoothScrollArea area = new QSmoothScrollArea();
        craftableGrid = new ItemGridWidget();
        craftableGrid.setClickHandler(this::onCraftClick);
        area.setWidget(craftableGrid);
        area.setWidgetResizable(true);
        leftLayout.addWidget(area, 1);
        craftableScrollArea = area;

        capacityBar = new CapacityBar();
        leftLayout.addWidget(capacityBar);

        mainLayout.addWidget(leftSide, 1);

        QSmoothScrollArea scrollArea = new QSmoothScrollArea();
        infoPanel = new CraftInfoPanel(this);
        infoPanel.setFixedWidth(125);
        scrollArea.setFixedWidth(135);
        scrollArea.setWidget(infoPanel);
        scrollArea.setWidgetResizable(true);

        // Control buttons below info panel
        qtyLabelCtrl = new QLabel(Component.literal("x1   "));
        qtyLabelCtrl.setTextColor(new QColor(0xFFFFAA00));
        craftBtnCtrl = new QPushButton(Component.translatable("awesome_storage.magic_storage_screen.craft"));
        craftBtnCtrl.setFixedHeight(14);
        craftBtnCtrl.setOnClick(() -> {
            if (!infoPanel.hasRecipe || infoPanel.parent.selectedRecipe == null || infoPanel.parent.selectedAdapter == null)
                return;
            // Only allow crafting if recipe is in the craftable list
            if (!craftableSet.contains(infoPanel.parent.selectedRecipe)) return;
            int count = Math.max(1, infoPanel.craftQuantity);
            var recipeId = infoPanel.parent.selectedRecipe.id();
            var adapterId = BuiltInRegistries.RECIPE_TYPE.getKey(infoPanel.parent.selectedAdapter.getRecipe());
            int cookTime = infoPanel.parent.selectedAdapter.getCookTime(infoPanel.parent.selectedRecipe);
            if (cookTime <= 0) {
                // Instant craft
                List<ItemStack> excluded = new ArrayList<>(infoPanel.excludedItems);
                for (int i = 0; i < count; i++)
                    PacketDistributor.sendToServer(new MagicCraftPacket(recipeId, adapterId, excluded));
            } else {
                // Add to queue
                PacketDistributor.sendToServer(QueueActionPacket.add(recipeId, adapterId, count));
            }
            var be = Util.getStorageEntity(Minecraft.getInstance().player);
            if (be != null) be.setChanged();
            updateTakeLabel();
            scheduleRefresh();
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
            @Override
            protected void mousePressEvent(QMouseEvent e) {
                e.accept();
                infoPanel.doTake();
            }
        };
        takeItemCtrl.setFixedSize(18, 18);
        takeL.addWidget(takeItemCtrl);
        takeL.addWidget(takeLabel);

        QPushButton p1 = new QPushButton(Component.literal("+1")), p10 = new QPushButton(Component.literal("+10")), p100 = new QPushButton(Component.literal("+100"));
        QPushButton m1 = new QPushButton(Component.literal("-1")), m10 = new QPushButton(Component.literal("-10")), m100 = new QPushButton(Component.literal("-100"));
        QPushButton maxB = new QPushButton(Component.translatable("awesome_storage.magic_storage_screen.max")), rstB = new QPushButton(Component.translatable("awesome_storage.magic_storage_screen.reset"));
        for (QPushButton b : new QPushButton[]{p1, p10, p100, m1, m10, m100, maxB, rstB}) {
            b.setFixedHeight(12);
        }
        p1.setOnClick(() -> {
            infoPanel.clampedAdd(1);
            qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));
        });
        p10.setOnClick(() -> {
            infoPanel.clampedAdd(10);
            qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));
        });
        p100.setOnClick(() -> {
            infoPanel.clampedAdd(100);
            qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));
        });
        m1.setOnClick(() -> {
            infoPanel.craftQuantity = Math.max(1, infoPanel.craftQuantity - 1);
            qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));
        });
        m10.setOnClick(() -> {
            infoPanel.craftQuantity = Math.max(1, infoPanel.craftQuantity - 10);
            qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));
        });
        m100.setOnClick(() -> {
            infoPanel.craftQuantity = Math.max(1, infoPanel.craftQuantity - 100);
            qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));
        });
        maxB.setOnClick(() -> {
            infoPanel.craftQuantity = infoPanel.getMaxCraftable();
            qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));
        });
        rstB.setOnClick(() -> {
            infoPanel.craftQuantity = 1;
            qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));
        });

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
        QHBoxLayout incRow = new QHBoxLayout();
        incRow.setSpacing(2);
        incRow.addWidget(p1);
        incRow.addWidget(p10);
        incRow.addWidget(p100);
        rvl.addLayout(incRow);

        // Row: -1 -10 -100
        QHBoxLayout decRow = new QHBoxLayout();
        decRow.setSpacing(2);
        decRow.addWidget(m1);
        decRow.addWidget(m10);
        decRow.addWidget(m100);
        rvl.addLayout(decRow);

        // Row: Max Reset
        QHBoxLayout optRow = new QHBoxLayout();
        optRow.setSpacing(2);
        optRow.addWidget(maxB);
        optRow.addWidget(rstB);
        favBtn = new FavoriteButton();
        favBtn.setFixedSize(16, 12);
        favBtn.setOnClick(() -> {
            if (selectedRecipe != null) {
                RecipeFavoriteSystem.getInstance().toggleFavorite(selectedRecipe.id());
                favBtn.updateState(RecipeFavoriteSystem.getInstance().isFavorited(selectedRecipe.id()));
                if (showFavoritesOnly) refresh();
            }
        });
        optRow.addWidget(favBtn);
        rvl.addLayout(optRow);


        takeLabelRef = takeLabel;
        mainLayout.addWidget(rightSide);

        RecipeFavoriteSystem.getInstance().loadFavorites();
        try {
            reloadRecipes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void reloadRecipes() {
        MagicStorageBlockEntity storage = getStorageEntity(Minecraft.getInstance().player);
        if (storage == null || Minecraft.getInstance().level == null) return;
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
                    var recipes = Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(adapter.getRecipe());
                    for (var r : recipes) adapter.loadRecipe(r, results, recipeMap);
                }
            }
        }
    }

    void refresh() {
        List<ItemStack> have = getStorageItems(Minecraft.getInstance().player);
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

        // Subtract ingredients reserved by the crafting queue
        MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
        if (be != null && be.getQueueManager() != null) {
            var reserved = new HashMap<Ingredient, Integer>();
            // Collect all queued recipes (pending + active slots)
            var qm = be.getQueueManager();
            for (var qr : qm.getPendingQueue()) {
                addReservedIngredients(reserved, qr.recipeId, qr.recipeTypeId);
            }
            for (var slot : qm.getSlots()) {
                if (!slot.isIdle()) {
                    var qr = slot.current();
                    if (qr != null) addReservedIngredients(reserved, qr.recipeId, qr.recipeTypeId);
                }
            }
            // Subtract reserved amounts from haveIngredients
            for (var e : reserved.entrySet()) {
                Ingredient ing = e.getKey();
                int need = e.getValue();
                for (var hi : new ArrayList<>(haveIngredients.entrySet())) {
                    if (ing.test(hi.getKey())) {
                        int subtract = Math.min(need, hi.getValue());
                        haveIngredients.put(hi.getKey(), hi.getValue() - subtract);
                        need -= subtract;
                        if (need <= 0) break;
                    }
                }
            }
            // Clean up zero entries
            haveIngredients.values().removeIf(v -> v <= 0);
        }

        List<Pair<ItemStack, RecipeHolder<?>>> craftable = new ArrayList<>();
        List<Pair<ItemStack, RecipeHolder<?>>> partial = new ArrayList<>();
        craftableSet.clear();
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
            if (can) {
                craftable.add(p);
                craftableSet.add(e.getKey());
            }
            else if (has) partial.add(p);
        }
        craftable.sort(Comparator.comparing(a -> a.getA().getDisplayName().getString()));
        partial.sort(Comparator.comparing(a -> a.getA().getDisplayName().getString()));
        cachedResults = new ArrayList<>(craftable);
        if (!showCraftableOnly) cachedResults.addAll(partial);

        List<ItemStack> resultItems = cachedResults.stream().map(p -> p.getA()).collect(Collectors.toList());
        if (showFavoritesOnly) {
            resultItems = resultItems.stream().filter(rs -> {
                for (var cp : cachedResults) {
                    if (ItemStack.isSameItemSameComponents(cp.getA(), rs)
                            && RecipeFavoriteSystem.getInstance().isFavorited(cp.getB().id())) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toList());
        }
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
                    found = ci;
                    break;
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
        MagicStorageBlockEntity be = Util.getStorageEntity(Minecraft.getInstance().player);
        if (be != null) {
            capacityBar.setSlots(be.getUsedSlots(), be.getTotalSlots());
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

                if (favBtn != null) {
                    favBtn.updateState(RecipeFavoriteSystem.getInstance().isFavorited(selectedRecipe.id()));
                }
                if (qtyLabelCtrl != null) qtyLabelCtrl.setText(Component.literal("x" + infoPanel.craftQuantity));
                int totalH = countByItem(stack.getItem());
                if (takeItemCtrl != null) {
                    var newStack = stack.copy();
                    newStack.setCount(1);
                    takeItemCtrl.setItemStack(newStack);
                    takeItemCtrl.setVisible(true);
                }
                if (takeLabelRef != null) takeLabelRef.setText(Component.literal("x" + totalH));
            }
        }
    }

    void scheduleRefresh() {
        parent.scheduleRefresh();
    }

    private void addReservedIngredients(Map<Ingredient, Integer> reserved, net.minecraft.resources.ResourceLocation recipeId, net.minecraft.resources.ResourceLocation recipeTypeId) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var optRecipe = level.getRecipeManager().byKey(recipeId);
        if (optRecipe.isEmpty()) return;
        var rt = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.get(recipeTypeId);
        if (rt == null) return;
        var adapter = AdapterManager.Adapters.get(rt);
        if (adapter == null) return;
        @SuppressWarnings("unchecked") NonNullList<Ingredient> ingredients = adapter.getIngredients((net.minecraft.world.item.crafting.RecipeHolder) optRecipe.get());
        for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) continue;
            reserved.merge(ing, 1, Integer::sum);
        }
    }

    // ========================================================================
    // Favorite Button — star icon, yellow when favorited
    // ========================================================================
    private static class FavoriteButton extends QWidget {
        private boolean isFav;
        private Runnable onClick;

        FavoriteButton() {
            setFocusPolicy(FocusPolicy.NoFocus);
        }

        void setOnClick(Runnable r) { this.onClick = r; }

        void updateState(boolean fav) {
            this.isFav = fav;
            update();
        }

        @Override
        protected void mousePressEvent(QMouseEvent event) {
            event.accept();
            if (onClick != null) onClick.run();
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPainter p = event.painter();
            if (p == null) return;
            int w = width(), h = height();
            if (isFav) {
                p.fillRect(0, 0, w, h, new QColor(0xFFFFDD00));
            } else if (isHovered()) {
                p.fillRect(0, 0, w, h, new QColor(0xFF555555));
            }
            p.setColor(isFav ? new QColor(0xFF333300) : new QColor(0xFF888888));
            p.drawText("★", (w - p.textWidth("★")) / 2, (h - p.textHeight()) / 2);
        }
    }

    // ========================================================================
    // Craft Info Panel — QWidget layout based
    // ========================================================================
    static class CraftInfoPanel extends QWidget {
        private final CraftPanel parent;
        private ItemStack outputItem = ItemStack.EMPTY;
        private final List<ItemStack> ingredients = new ArrayList<>();
        private final List<ItemStack> requiredStations = new ArrayList<>();
        private boolean hasRecipe;
        int craftQuantity = 1;
        int cookTime;

        NonNullList<Ingredient> ings = NonNullList.create();
        final Set<ItemStack> excludedItems = new HashSet<>();
        private int cycleIndex = 0;
        private long lastCycleTime = 0;

        private InfoSlot outputSlot;
        private final QLabel cookTimeLabel;
        private final QWidget ingredientGrid;
        private final QWidget stationGrid;
        private final QWidget storageGrid;
        private final QLabel extraLabel;
        private final QWidget extraGrid;

        CraftInfoPanel(CraftPanel parent) {
            this.parent = parent;
            setFixedSize(180, 300);
            setMinimumSize(180, 200);

            QVBoxLayout layout = new QVBoxLayout(this);
            layout.setSpacing(2);
            layout.setContentsMargins(6, 6, 6, 6);

            addHeader(layout, "awesome_storage.magic_storage_screen.output");
            outputSlot = new InfoSlot(ItemStack.EMPTY);
            outputSlot.setFixedSize(20, 20);
            layout.addWidget(outputSlot);

            cookTimeLabel = new QLabel();
            cookTimeLabel.setTextColor(new QColor(0xFF8888FF));
            cookTimeLabel.setVisible(false);
            layout.addWidget(cookTimeLabel);

            addHeader(layout, "awesome_storage.magic_storage_screen.ingredients");
            ingredientGrid = new QWidget();
            layout.addWidget(ingredientGrid);

            addHeader(layout, "awesome_storage.magic_storage_screen.stations");
            stationGrid = new QWidget();
            layout.addWidget(stationGrid);

            addHeader(layout, "awesome_storage.magic_storage_screen.in_storage");
            storageGrid = new QWidget();
            layout.addWidget(storageGrid);

            extraLabel = new QLabel();
            extraLabel.setTextColor(new QColor(0xFFFFAA00));
            extraLabel.setVisible(false);
            layout.addWidget(extraLabel);
            extraGrid = new QWidget();
            extraGrid.setVisible(false);
            layout.addWidget(extraGrid);

            layout.addStretch(1);
        }

        private void addHeader(QVBoxLayout layout, String key) {
            QLabel label = new QLabel(Component.translatable(key).append(":"));
            label.setTextColor(new QColor(0xFFFFAA00));
            layout.addWidget(label);
        }

        // ====================================================================
        // Info slot widget
        // ====================================================================
        private static class InfoSlot extends QWidget {
            ItemStack stack;
            boolean overlay;
            Runnable onClick;
            ItemStack tooltipStack;

            InfoSlot(ItemStack stack) { this(stack, false, null); }
            InfoSlot(ItemStack stack, boolean overlay, Runnable onClick) {
                this.stack = stack;
                this.overlay = overlay;
                this.onClick = onClick;
                this.tooltipStack = stack;
                setFixedSize(18, 18);
            }
            void setStack(ItemStack s) { this.stack = s; this.tooltipStack = s; markDirty(); update(); }

            @Override
            protected void mousePressEvent(QMouseEvent event) {
                if (onClick != null && event.button() == QMouseEvent.Button.Left) {
                    onClick.run();
                    event.accept();
                }
            }
            @Override
            public WidgetTooltip toolTip() {
                return tooltipStack.isEmpty() ? null : WidgetTooltip.create(tooltipStack);
            }
            @Override
            protected void paintEvent(QPaintEvent event) {
                QPainter p = event.painter();
                if (p == null) return;
                if (stack.isEmpty()) { p.fillRect(0, 0, width(), height(), new QColor(0xCC333333)); return; }
                MagicStorageScreen.renderSlot(p, 0, 0, width(), stack, isHovered(), overlay);
            }
        }

        // ====================================================================
        // Layout helpers
        // ====================================================================
        private void buildWrappedGrid(QWidget parent, List<ItemStack> items, boolean overlay,
                                      java.util.function.Consumer<Integer> onClick, int maxItems) {
            for (QObject child : new ArrayList<>(parent.children())) {
                if (child instanceof QWidget w) w.destroy();
            }
            int cols = Math.max(1, (width() - 12) / 20);
            int rows = 0;
            for (int i = 0; i < Math.min(items.size(), maxItems); i++) {
                int idx = i, col = i % cols, row = i / cols;
                InfoSlot slot = new InfoSlot(items.get(i), overlay,
                        onClick != null ? () -> onClick.accept(idx) : null);
                slot.setParent(parent);
                slot.move(col * 20, row * 20);
                rows = row + 1;
            }
            parent.setFixedHeight(rows * 20);
            parent.markDirty();
        }

        // ====================================================================
        // Logic methods unchanged
        // ====================================================================
        private int getMaxCraftable() {
            LinkedHashMap<Item, Integer> needPerCraft = new LinkedHashMap<>();
            LinkedHashMap<Item, Ingredient> ingByItem = new LinkedHashMap<>();
            for (Ingredient ing : ings) {
                if (ing.getItems().length == 0) continue;
                Item item = ing.getItems()[0].getItem();
                needPerCraft.merge(item, 1, Integer::sum);
                ingByItem.putIfAbsent(item, ing);
            }
            int maxP = Integer.MAX_VALUE;
            for (Map.Entry<Item, Integer> e : needPerCraft.entrySet()) {
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

        boolean isExcluded(ItemStack stack) {
            for (ItemStack ex : excludedItems) {
                if (ItemStack.isSameItemSameComponents(stack, ex)) return true;
            }
            return false;
        }

        void rebuildIngredients() {
            ingredients.clear();
            LinkedHashMap<Item, Integer> merged = new LinkedHashMap<>();
            LinkedHashMap<Item, Ingredient> ingByItem = new LinkedHashMap<>();
            for (Ingredient ing : ings) {
                if (ing.getItems().length > 0) {
                    Item item = ing.getItems()[0].getItem();
                    merged.merge(item, 1, Integer::sum);
                    ingByItem.putIfAbsent(item, ing);
                }
            }
            for (Map.Entry<Item, Integer> e : merged.entrySet()) {
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
                if (hasRecipe) {
                    buildWrappedGrid(ingredientGrid, ingredients, false, null, Integer.MAX_VALUE);
                    markDirty(); update();
                }
            }
        }

        void clampedAdd(int delta) {
            craftQuantity = Math.max(1, Math.min(getMaxCraftable(), craftQuantity + delta));
        }

        void doTake() {
            List<ItemStack> stored = getStorageItems(Minecraft.getInstance().player);
            if (stored == null || outputItem.isEmpty()) return;
            for (int i = 0; i < stored.size(); i++) {
                if (ItemStack.isSameItemSameComponents(stored.get(i), outputItem)) {
                    PacketDistributor.sendToServer(new MagicStoragePacket(i + 10000, stored.get(i).copy()));
                    var be = Util.getStorageEntity(Minecraft.getInstance().player);
                    if (be != null) be.setChanged();
                    parent.scheduleRefresh();
                    break;
                }
            }
        }

        @SuppressWarnings("unchecked")
        void showRecipe(RecipeHolder<?> recipe, AbstractMagicCraftRecipeAdapter adapter, ItemStack output,
                        Map<ItemStack, Integer> have) {
            outputItem = output;
            ingredients.clear();
            requiredStations.clear();
            excludedItems.clear();
            craftQuantity = 1;

            var casted = (AbstractMagicCraftRecipeAdapter) adapter;
            ings = casted.getIngredients((RecipeHolder) recipe);
            rebuildIngredients();

            requiredStations.clear();
            if (CraftConfig.ENABLED_RECIPES.containsKey(adapter.getRecipe())) {
                Set<Block> seen = new HashSet<>();
                for (Block b : CraftConfig.ENABLED_RECIPES.get(adapter.getRecipe())) {
                    if (seen.add(b)) requiredStations.add(new ItemStack(b));
                }
            }
            cookTime = adapter.getCookTime(recipe);
            hasRecipe = true;

            outputSlot.setStack(outputItem);

            if (cookTime > 0) {
                cookTimeLabel.setText(Component.literal("Time: " + (cookTime / 20) + "s"));
                cookTimeLabel.setVisible(true);
            } else cookTimeLabel.setVisible(false);

            buildWrappedGrid(ingredientGrid, ingredients, false, null, Integer.MAX_VALUE);
            buildWrappedGrid(stationGrid, requiredStations, false, null, Integer.MAX_VALUE);

            // In Storage
            List<ItemStack> matched = new ArrayList<>();
            for (Map.Entry<ItemStack, Integer> e : parent.haveIngredients.entrySet()) {
                for (Ingredient ing : ings) {
                    if (!ing.isEmpty() && ing.test(e.getKey())) { matched.add(e.getKey()); break; }
                }
            }
            matched.sort(Comparator.<ItemStack, String>comparing(
                    s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
                    .thenComparing(s -> s.getDisplayName().getString())
                    .thenComparing(s -> s.getComponentsPatch().hashCode()));
            List<ItemStack> finalMatched = matched;
            buildWrappedGrid(storageGrid, matched, false, idx -> {
                if (idx < 0 || idx >= finalMatched.size()) return;
                ItemStack clicked = finalMatched.get(idx);
                boolean removed = excludedItems.removeIf(ex -> ItemStack.isSameItemSameComponents(ex, clicked));
                if (!removed) excludedItems.add(clicked.copy());
                // Rebuild with overlay indicators
                List<ItemStack> updated = new ArrayList<>(finalMatched);
                buildWrappedGrid(storageGrid, updated, true, null, 8);
                for (int i = 0; i < updated.size() && i < 8; i++) {
                    QWidget child = storageGrid.widgetChildren().get(i);
                    if (child instanceof InfoSlot is) is.overlay = isExcluded(updated.get(i));
                }
                storageGrid.markDirty(); storageGrid.update();
            }, 8);

            // Extra info from adapter (tools, etc.)
            List<ItemStack> extraItems = adapter.getExtraInfoItems((RecipeHolder) recipe);
            if (!extraItems.isEmpty()) {
                String labelKey = adapter.getExtraInfoLabel((RecipeHolder) recipe);
                extraLabel.setText(Component.translatable(labelKey.isEmpty() ? "awesome_storage.craft_info.extra" : labelKey).append(":"));
                extraLabel.setVisible(true);
                buildWrappedGrid(extraGrid, extraItems, false, null, Integer.MAX_VALUE);
                extraGrid.setVisible(true);
            } else {
                extraLabel.setVisible(false);
                extraGrid.setVisible(false);
            }

            markDirty(); update();
        }

        @Override
        public QWidget childAt(int px, int py) { return super.childAt(px, py); }
        @Override
        public QSize sizeHint() { return new QSize(100, 300); }
    }
}
