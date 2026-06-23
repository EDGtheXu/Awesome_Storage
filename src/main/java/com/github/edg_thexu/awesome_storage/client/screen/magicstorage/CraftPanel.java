package com.github.edg_thexu.awesome_storage.client.screen.magicstorage;

import com.github.edg_thexu.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import com.github.edg_thexu.awesome_storage.api.adapter.AdapterManager;
import com.github.edg_thexu.awesome_storage.api.adapter.CommonRecipeAdapter;
import com.github.edg_thexu.awesome_storage.config.CraftConfig;
import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicCraftPacket;
import com.github.edg_thexu.awesome_storage.core.network.c2s.MagicStoragePacket;
import com.github.edg_thexu.awesome_storage.utils.Util;
import com.github.edg_thexu.qtcraft_api.core.QSizePolicy;
import com.github.edg_thexu.qtcraft_api.core.events.QMouseEvent;
import com.github.edg_thexu.qtcraft_api.core.events.QPaintEvent;
import com.github.edg_thexu.qtcraft_api.core.geometry.QSize;
import com.github.edg_thexu.qtcraft_api.core.layouts.QHBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QLayout;
import com.github.edg_thexu.qtcraft_api.core.layouts.QVBoxLayout;
import com.github.edg_thexu.qtcraft_api.core.painting.QColor;
import com.github.edg_thexu.qtcraft_api.core.painting.QPainter;
import com.github.edg_thexu.qtcraft_api.core.signal_slot.slots.SlotKeyConsumer;
import com.github.edg_thexu.qtcraft_api.core.widget.QWidget;
import com.github.edg_thexu.qtcraft_api.core.widget.button.QPushButton;
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
    private final StationsRowWidget stationsRow;
    ItemGridWidget craftableGrid;
    private final QLabel capacityLabel;
    final CraftInfoPanel infoPanel;
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

    CraftPanel(MagicStorageScreen parent) {
        this.parent = parent;
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
        showCraftBtn.setOnClick(() -> {
            showCraftableOnly = true;
            refresh();
        });
        funcRow.addWidget(showCraftBtn);
        QPushButton showAllBtn = new QPushButton(Component.literal("All"));
        showAllBtn.setFixedHeight(16);
        showAllBtn.setOnClick(() -> {
            showCraftableOnly = false;
            refresh();
        });
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
        filterBar = new FilterBar(filterRow, this, "c", () -> {
            if (craftableGrid != null) refresh();
        });
        leftLayout.addLayout(filterRow);

        stationsRow = new StationsRowWidget(parent);
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
        infoPanel.setFixedWidth(125);
        scrollArea.setFixedWidth(135);
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
                var be = Util.getStorageEntity(Minecraft.getInstance().player);
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
        QPushButton maxB = new QPushButton(Component.literal("Max")), rstB = new QPushButton(Component.literal("Reset"));
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
        rvl.addLayout(optRow);


        takeLabelRef = takeLabel;
        mainLayout.addWidget(rightSide);

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

    // ========================================================================
    // Craft Info Panel
    // ========================================================================
    static class CraftInfoPanel extends QWidget {
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

        private boolean isExcluded(ItemStack stack) {
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
                markDirty();
                update();
            }
        }

        private void clampedAdd(int delta) {
            craftQuantity = Math.max(1, Math.min(getMaxCraftable(), craftQuantity + delta));
        }


        private void doTake() {
            List<ItemStack> stored = getStorageItems(Minecraft.getInstance().player);
            if (stored == null || outputItem.isEmpty()) return;
            // Server-side takeItem(index) uses its own getStoredItems() list and matches by
            // isSameItemSameComponents — we just need the correct index for the item type.
            for (int i = 0; i < stored.size(); i++) {
                if (stored.get(i).getItem() == outputItem.getItem()) {
                    PacketDistributor.sendToServer(new MagicStoragePacket(i + 10000, stored.get(i).copy()));
                    var be = Util.getStorageEntity(Minecraft.getInstance().player);
                    if (be != null) be.setChanged();
                    parent.scheduleRefresh();
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
                Set<Block> seen = new HashSet<>();
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
                p.setColor(new QColor(0xFF888888));
                p.drawText("Select an item", 8, 20);
                return;
            }

            hoverSlots.clear();
            hoverSlotX.clear();
            hoverSlotY.clear();
            hoverSlotSize.clear();

            int y = 6, ss = 18, cw = width();

            p.setColor(new QColor(0xFFFFAA00));
            p.drawText("Output:", 6, y);
            y += 11;
            MagicStorageScreen.renderSlot(p, 6, y, 20, outputItem, false);
            hoverSlots.add(outputItem);
            hoverSlotX.add(6);
            hoverSlotY.add(y);
            hoverSlotSize.add(20);
            y += 38;

            p.setColor(QColor.WHITE);
            p.drawText("Ingredients:", 6, y);
            y += 11;
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
                MagicStorageScreen.renderSlot(p, ix, y, ss, ing, false);
                hoverSlots.add(ing);
                hoverSlotX.add(ix);
                hoverSlotY.add(y);
                hoverSlotSize.add(ss);
                if (miss) p.fillRect(ix, y, ss, ss, new QColor(0x44FF0000));
                ix += ss + 2;
                if (ix > cw - ss) {
                    ix = 6;
                    y += ss + 2;
                }
            }
            if (!ingredients.isEmpty()) y += (ix > 6 ? ss + 6 : 4);

            p.drawText("Stations:", 6, y);
            y += 11;
            ix = 6;
            for (ItemStack st : requiredStations) {
                MagicStorageScreen.renderSlot(p, ix, y, ss, st, false);
                hoverSlots.add(st);
                hoverSlotX.add(ix);
                hoverSlotY.add(y);
                hoverSlotSize.add(ss);
                ix += ss + 2;
                if (ix > cw - ss) {
                    ix = 6;
                    y += ss + 2;
                }
            }
            if (!requiredStations.isEmpty()) y += ss + 6;

            // In Storage: show each component-group that matches any ingredient — red overlay if excluded
            p.drawText("In Storage:", 6, y);
            y += 11;
            ix = 6;
            storageItems.clear();
            // Collect matching entries and sort by stable key (registry ID) to prevent position flickering
            List<Map.Entry<ItemStack, Integer>> matched = new ArrayList<>();
            for (Map.Entry<ItemStack, Integer> e : parent.haveIngredients.entrySet()) {
                for (Ingredient ing : ings) {
                    if (!ing.isEmpty() && ing.test(e.getKey())) {
                        matched.add(e);
                        break;
                    }
                }
            }
            matched.sort(Comparator.<Map.Entry<ItemStack, Integer>, String>comparing(
                            e -> BuiltInRegistries.ITEM.getKey(e.getKey().getItem()).toString())
                    .thenComparing(e -> e.getKey().getDisplayName().getString())
                    .thenComparing(e -> e.getKey().getComponentsPatch().hashCode()));
            int drawn = 0;
            for (Map.Entry<ItemStack, Integer> e : matched) {
                if (drawn >= 8) break;
                ItemStack display = e.getKey().copy();
                display.setCount(Math.min(e.getValue(), 9999));
                boolean excl = isExcluded(e.getKey());
                MagicStorageScreen.renderSlot(p, ix, y, ss, display, false, excl);
                hoverSlots.add(display);
                hoverSlotX.add(ix);
                hoverSlotY.add(y);
                hoverSlotSize.add(ss);
                storageItems.add(display);
                storageSlotX[drawn] = ix;
                storageSlotY[drawn] = y;
                ix += ss + 2;
                if (ix > cw - ss) {
                    ix = 6;
                    y += ss + 2;
                }
                drawn++;
            }
            if (drawn > 0) y += ss + 6;
            else y += 4;
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
                    update();
                    event.accept();
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
        public QSize sizeHint() {
            return new QSize(100, 300);
        }
    }
}
