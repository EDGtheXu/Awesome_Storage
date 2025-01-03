package coffee.awesome_storage.client.screen.widget;

import coffee.awesome_storage.block.MagicStorageBlockEntity;
import coffee.awesome_storage.client.screen.MagicStorageScreen;
import coffee.awesome_storage.recipe.MagicCraftRecipeLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


import java.util.*;
import java.util.function.Predicate;

import static coffee.awesome_storage.Util.Util.getStorageEntity;
import static coffee.awesome_storage.Util.Util.getStorageItems;
import static coffee.awesome_storage.config.CraftConfig.ENABLED_RECIPES;

@OnlyIn(Dist.CLIENT)
public class MagicCraftWidget extends AbstractFloatWidget {

    List<ItemStack> results = new ArrayList<>();
    List<ItemStack> cachedResults;
    List<ItemStack> cachedAppend;
    int selectedIndex = -1;
    long reloadingTime = 0;
    Map<ItemStack,RecipeHolder<?>> recipeMap = new HashMap<>();

    public Map<Item, Integer> haveIngredients = new HashMap<>();
    public ItemStack selectedItem;
    public RecipeHolder<?> selectedRecipe;
    public Map<Ingredient, Integer> realIngredients = new HashMap<>();



    public final MagicCraftDisplayWidget displayWidget;
    public final MagicCraftAccessWidget accessWidget;

    public MagicCraftWidget(MagicStorageScreen screen, int x, int y, int width, int height, Component message) {
        super(screen, x, y, width, height, message);
//        reloadRecipes();
        displayWidget = (MagicCraftDisplayWidget) new MagicCraftDisplayWidget(this, screen.getGuiLeft()+25, screen.getGuiTop()+30, width, height, message)
                .setNoRenderButton()
        ;
        accessWidget = (MagicCraftAccessWidget) new MagicCraftAccessWidget(this, screen.getGuiLeft()+10, screen.getGuiTop()+55, screen.width - 100, 20, message)
                .setNoRenderButton()
        ;

    }

    public void reloadRecipes(){
        var allRecipes = Minecraft.getInstance().level.getRecipeManager();
        List<MagicCraftRecipeLoader<RecipeInput>> craftss = new ArrayList<>();
        MagicStorageBlockEntity storage = getStorageEntity(Minecraft.getInstance().player);
        results.clear();
        recipeMap.clear();
        storage.getBlock_accessors().forEach(s->{
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s));
            if(ENABLED_RECIPES.containsKey(block)){
                craftss.add(new MagicCraftRecipeLoader<>(ENABLED_RECIPES.get(block)));
            }
        });

        craftss.forEach(r->{
            try {
                var recipes2 = allRecipes.getAllRecipesFor(r.getRecipe());
                recipes2.forEach(e-> {
                    try {
                        r.loadResults(e,results,recipeMap);
                    }catch (Exception e1){
                        e1.printStackTrace();
                    }
                });
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }
    @Override
    protected  Predicate<ItemStack> overlay(){
        return e->this.cachedAppend.contains(e);
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float v) {
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, v);
        reloadingTime++;
        if(reloadingTime > 10 && reloadingTime < 1000){
            reloadingTime = 1000;
            reloadRecipes();
        }
//        this.displayWidget.renderWidget(pGuiGraphics, pMouseX, pMouseY, 0);
    }



    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
    // 计算选中的配方对应的原料Map<Ingredient, Integer>
        if(hoverIt!=null){
            selectedIndex = hoverIndex;
            selectedItem = cachedResults.get(selectedIndex);
            selectedRecipe = recipeMap.get(selectedItem);
            if(selectedRecipe!=null) {
                var ingredients = selectedRecipe.value().getIngredients();
                // 合并相同物品
                realIngredients.clear();
                for (Ingredient ingredient : ingredients) {
                    if (realIngredients.containsKey(ingredient)) {
                        // 如果已存在，则将值加
                        if(ingredient.getItems().length>0)
                            realIngredients.put(ingredient, realIngredients.get(ingredient) + ingredient.getItems()[0].getCount());
                    } else {
                        // 如果不存在，则插入新的原料
                        if(ingredient.getItems().length>0){
                            realIngredients.put(ingredient, ingredient.getItems()[0].getCount());
                        }
                    }
                }
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {

    }

    @Override
    protected List<ItemStack> getItems() {
        List<ItemStack> res = new ArrayList<>();
        List<ItemStack> have = getStorageItems(Minecraft.getInstance().player);
        List<ItemStack> append = new ArrayList<>();

        // 将have中的ItemStack转换为Map<Item, Integer>
        haveIngredients.clear();
        for (ItemStack stack : have) {
            Item item = stack.getItem();
            int count = stack.getCount();
            haveIngredients.put(item, haveIngredients.getOrDefault(item, 0) + count);
        }

        // 获取所有配方（假设recipeMap是一个Map，存储所有配方）
        for (RecipeHolder<?> recipe : recipeMap.values()) {
            // 创建一个临时的原料副本
            Map<Item, Integer> tempIngredients = new HashMap<>(haveIngredients);

            // 检查是否能够合成当前配方
            boolean canCraft = true;
            boolean contain = false;
            for (Ingredient ingredient : recipe.value().getIngredients()) {
                // 检查配方原料是否在have中
                boolean ingredientFound = false;
                var items = ingredient.getItems();
                if(items.length==0) ingredientFound = true;

                for (ItemStack ingredientStack : items) {

                    Item ingredientItem = ingredientStack.getItem();
                    int requiredCount = ingredientStack.getCount();
                    if (tempIngredients.containsKey(ingredientItem) ) {
                        contain = true;
                        if(tempIngredients.get(ingredientItem) >= requiredCount){
                            // 减少临时原料数量
                            tempIngredients.put(ingredientItem, tempIngredients.get(ingredientItem) - requiredCount);
                            ingredientFound = true;
                            break;
                        }
                    }
                }

                // 如果某个原料不满足条件，则无法合成
                if (!ingredientFound) {
                    canCraft = false;
                }
                // 记录材料包含的物品
                if(!canCraft && contain) {
                    append.add(recipe.value().getResultItem(null));
                    break;
                }
            }

            // 如果能够合成，则添加结果到res中
            if (canCraft) {
                res.add(recipe.value().getResultItem(null));
            }
        }

        res.sort(Comparator.comparing(a -> a.getDisplayName().getString()));
        append.sort(Comparator.comparing(a -> a.getDisplayName().getString()));
        res.addAll(append);
        cachedAppend = append;
        cachedResults = res;
        return res;
    }



    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public List<AbstractWidget> children() {

        return List.of(displayWidget,lastBt,nextBt);
    }

}
