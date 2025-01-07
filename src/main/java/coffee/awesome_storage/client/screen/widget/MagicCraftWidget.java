package coffee.awesome_storage.client.screen.widget;

import coffee.awesome_storage.Awesome_storage;
import coffee.awesome_storage.api.adapter.AbstractMagicCraftRecipeAdapter;
import coffee.awesome_storage.api.adapter.AdapterManager;
import coffee.awesome_storage.api.adapter.CommonRecipeAdapter;
import coffee.awesome_storage.block.MagicStorageBlockEntity;
import coffee.awesome_storage.client.screen.MagicStorageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import oshi.util.tuples.Pair;

import java.util.*;
import java.util.function.BiPredicate;

import static coffee.awesome_storage.config.CraftConfig.ENABLED_RECIPES;
import static coffee.awesome_storage.utils.Util.getStorageEntity;
import static coffee.awesome_storage.utils.Util.getStorageItems;

@OnlyIn(Dist.CLIENT)
public class MagicCraftWidget extends AbstractFloatWidget {

    List<ItemStack> results = new ArrayList<>();

    List<ItemStack> cachedItems = new ArrayList<>();
    List<Pair<ItemStack, Recipe >> cachedResults;
    List<Pair<ItemStack,Recipe >> cachedAppend;

    int selectedIndex = -1;
    long reloadingTime = 0;
    Map<Recipe,AbstractMagicCraftRecipeAdapter> recipeMap = new HashMap<>();

    public Map<Item, Integer> haveIngredients = new HashMap<>();
    public ItemStack selectedItem;
    public Recipe  selectedRecipe;
    public AbstractMagicCraftRecipeAdapter<Container,Recipe<Container>> selectedAdapter;
    public Map<Ingredient, Integer> realIngredients = new HashMap<>();

    List<AbstractMagicCraftRecipeAdapter<Container,Recipe<Container>>> craftss = new ArrayList<>();
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
//        reloadRecipes();
        refreshItems();
    }

    public void reloadRecipes(){
        craftss.clear();
        MagicStorageBlockEntity storage = getStorageEntity(Minecraft.getInstance().player);
        results.clear();
        recipeMap.clear();

        List<Block> accessors = storage.getBlock_accessors().stream().map(s->BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(s))).toList();

        for(var Entry : BuiltInRegistries.RECIPE_TYPE.entrySet()){
            RecipeType recipeType = Entry.getValue();
            if(ENABLED_RECIPES.containsKey(recipeType)){

                List<Block> require = ENABLED_RECIPES.get(recipeType);
                boolean accept = true;
                for(var block : require){
                    if(!accessors.contains(block)){
                        accept = false;
                        break;
                    }
                }
                if(accept){
                    if(AdapterManager.Adapters.containsKey(recipeType)){
                        var adapter = AdapterManager.Adapters.get(recipeType);
                        loadRecipeType(adapter);
                    }else{// 不应该出现这种情况
                        Awesome_storage.LOGGER.error("exception: recipeType not found, replace with CommonRecipeAdapter");
                        var adapter2 = new CommonRecipeAdapter(recipeType);
                        loadRecipeType((AbstractMagicCraftRecipeAdapter<Container, Recipe<Container>>) adapter2);
                    }
                }
            }
        }

        storage.getBlock_accessors().forEach(s->{
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(s));
//            if(ENABLED_RECIPES.containsKey(block)){
//                RecipeType<?> recipeType = ENABLED_RECIPES.get(block);
//                if(AdapterManager.Adapters.containsKey(recipeType)){
//                    var adapter = AdapterManager.Adapters.get(recipeType);
//                    loadRecipeType(adapter);
//               }
//               else{// 不应该出现这种情况
//                    Awesome_storage.LOGGER.error("exception: recipeType not found, replace with CommonRecipeAdapter");
//                    var adapter2 = new CommonRecipeAdapter<>(ENABLED_RECIPES.get(block));
////                    if(!AdapterManager.Adapters.containsKey(recipeType)){
////                        AdapterManager.Adapters.put(recipeType,adapter2);
////                    }
//                    loadRecipeType(adapter2);
//                }
//            }
        });
    }

    public void loadRecipeType(AbstractMagicCraftRecipeAdapter<Container,Recipe<Container>> adapter){
        var recipes = Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(adapter.getRecipe());
        recipes.forEach(recipe-> {
            adapter.loadRecipe(recipe,results, recipeMap);
        });
    }


    @Override
    protected BiPredicate<ItemStack,Integer> overlay(){
        return (e,i)->this.cachedResults.size() - cachedAppend.size() <= i;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float v) {
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, v);
        reloadingTime++;
        if(reloadingTime > 10 && reloadingTime < 1000){
            reloadingTime = 1000;
            reloadRecipes();
            refreshItems();
        }
    }


    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
    // 计算选中的配方对应的原料Map<Ingredient, Integer>
        if(hoverIt!=null){
            selectedIndex = hoverIndex;
            selectedItem = hoverIt;
            selectedRecipe = cachedResults.get(selectedIndex).getB();
            AbstractMagicCraftRecipeAdapter<Container,Recipe<Container>> adapter = recipeMap.get(selectedRecipe);
            selectedAdapter = adapter;
            if(selectedRecipe!=null) {
                var ingredients = adapter.getIngredients(selectedRecipe);
                // 合并相同物品
                realIngredients.clear();
                for (Object ingredient1 : ingredients) {
                    Ingredient ingredient = (Ingredient) ingredient1;
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
        super.mouseMoved(mouseX, mouseY);
    }
    public void refreshItems(){


        List<Pair<ItemStack,Recipe >> res = new ArrayList<>();
        List<Pair<ItemStack,Recipe >> append = new ArrayList<>();
        List<ItemStack> have = getStorageItems(Minecraft.getInstance().player);

        // 将have中的ItemStack转换为Map<Item, Integer>
        haveIngredients.clear();
        for (ItemStack stack : have) {
            Item item = stack.getItem();
            int count = stack.getCount();
            haveIngredients.put(item, haveIngredients.getOrDefault(item, 0) + count);
        }

        // 获取所有配方（假设recipeMap是一个Map，存储所有配方）
        for (Map.Entry<Recipe , AbstractMagicCraftRecipeAdapter> pRecipe : recipeMap.entrySet()) {
            Recipe  recipe = pRecipe.getKey();
            var adapter = pRecipe.getValue();
            NonNullList<Ingredient> ingredients = adapter.getIngredients(recipe);

            // 创建一个临时的原料副本
            Map<Item, Integer> tempIngredients = new HashMap<>(haveIngredients);

            // 检查是否能够合成当前配方
            boolean canCraft = true;
            boolean contain = false;
            for (Ingredient ingredient : ingredients) {
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
                    append.add(new Pair<>(adapter.getResult(recipe),recipe));
                    break;
                }
            }

            // 如果能够合成，则添加结果到res中
            if (canCraft) {
                res.add(new Pair<>(adapter.getResult(recipe),recipe));
            }
        }

        res.sort(Comparator.comparing(a -> a.getA().getDisplayName().getString()));
        append.sort(Comparator.comparing(a -> a.getA().getDisplayName().getString()));
        res.addAll(append);
        cachedAppend = append;
        cachedResults = res;
        List<ItemStack> real = new ArrayList<>();
        res.forEach(l->real.add((l.getA())));
        cachedItems = real;
    }

    @Override
    protected List<ItemStack> getItems() {
        return cachedItems;
    }



    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

}
