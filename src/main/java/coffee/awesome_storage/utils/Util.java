package coffee.awesome_storage.utils;

import coffee.awesome_storage.mix_util.IPlayer;
import coffee.awesome_storage.block.MagicStorageBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Util {
    public static List<ItemStack> getStorageItems(Player player) {
        var entity = (MagicStorageBlockEntity) ((IPlayer) player).awesomeStorage$getContainer();
        return entity.getItems();
    }

    public static MagicStorageBlockEntity getStorageEntity(Player player) {
        if (((IPlayer) player).awesomeStorage$getContainer() instanceof MagicStorageBlockEntity entity)
            return entity;
        return null;
    }

    public static void renderItemStack(GuiGraphics guiGraphics, ItemStack it, int x, int y, boolean overLay) {
        var minecraft = Minecraft.getInstance();
        guiGraphics.pose().pushPose();
        if (overLay) guiGraphics.setColor(1F, 0.5F, 0.5F, 1F);
        guiGraphics.renderItem(it, x, y);
        guiGraphics.setColor(1F, 1, 1, 1F);
        guiGraphics.renderItemDecorations(minecraft.font, it, x, y);

        guiGraphics.pose().popPose();
    }

    /**
     * 获取已有物品的原料数量
     *
     * @param have 已有物品
     * @return 物品对应数量
     */
    public static Map<Item, Integer> getHaveIngredients(List<ItemStack> have) {
        Map<Item, Integer> haveIngredients = new HashMap<>();
        for (ItemStack stack : have) {
            Item item = stack.getItem();
            int count = stack.getCount();
            haveIngredients.put(item, haveIngredients.getOrDefault(item, 0) + count);
        }
        return haveIngredients;
    }

    /**

     * 常规物品判断能否合成
     *
     * @param haveIngredients 已有物品的原料数量
     * @param ingredients     物品的原料
     * @return 能否合成
     */
    public static boolean canCraftSimple(Map<Item, Integer> haveIngredients, NonNullList<Ingredient> ingredients) {
        boolean canCraft = true;
        for (Ingredient ingredient : ingredients) {
            boolean ingredientFound = false;
            if (ingredient.isEmpty()) continue;
            for (ItemStack ingredientStack : ingredient.getItems()) {
                Item ingredientItem = ingredientStack.getItem();
                int requiredCount = ingredientStack.getCount();

                // 如果已有原料中有这个原料，并且数量足够
                if (haveIngredients.containsKey(ingredientItem) && haveIngredients.get(ingredientItem) >= requiredCount) {
                    // 减少对应原料的数量
                    haveIngredients.put(ingredientItem, haveIngredients.get(ingredientItem) - requiredCount);
                    ingredientFound = true;
                    break; // 找到后可以退出内层循环
                }
            }
            // 如果没有找到足够的原料，则无法合成
            if (!ingredientFound) {
                canCraft = false;
                break; // 退出外层循环
            }
        }
        return canCraft;
    }

    /**
     *  todo: 加入适配器
     *  默认适配器
     * @param have 已有物品
     * @param ingredients 物品的原料
     * @return 能否合成
     */
    public static boolean canCraft(List<ItemStack> have, NonNullList<Ingredient> ingredients) {
        Map<Item, Integer> haveIngredients = getHaveIngredients(have);
        return canCraftSimple(haveIngredients, ingredients);
    }

    /**
     * 物品合成
     * @param have 已有物品 原料
     * @param ingredients 物品的原料
     */
    public static void doCraft(List<ItemStack> have, NonNullList<Ingredient> ingredients) {
        // 移除原料
        for (Ingredient ingredient : ingredients) {
            if(ingredient.isEmpty()) continue;
            for (ItemStack ingredientStack : ingredient.getItems()) {
                Item ingredientItem = ingredientStack.getItem();
                int requiredCount = ingredientStack.getCount();

                for(int i = 0; i < have.size(); i++){
                    ItemStack stack = have.get(i);
                    // todo 适配器匹配
                    if(stack.getItem() == ingredientItem){
                        int count = stack.getCount();
                        if(count >= requiredCount){
                            stack.shrink(requiredCount);
                            break;
                        }else{
                            requiredCount -= count;
                            have.remove(i);
                        }
                    }
                }
            }
        }

    }

    /**
     * 合并堆叠物品
     * @param items 物品列表
     */
    public static void unionItemStacks(List<ItemStack> items){
        for (int i = 0; i < items.size(); i++) {
            ItemStack current = items.get(i);
            if (current.isEmpty()) {
                continue; // 跳过空槽
            }
            for (int j = i + 1; j < items.size(); j++) {
                ItemStack next = items.get(j);
                if (!next.isEmpty() && current.is(next.getItem()) && current.getCount() < current.getMaxStackSize()) {
                    // 合并堆叠
                    int transfer = Math.min(next.getCount(), current.getMaxStackSize() - current.getCount());
                    current.grow(transfer); // 增加当前物品的数量
                    next.shrink(transfer);  // 减少下一个物品的数量
                    if (next.isEmpty()) {
                        items.set(j, ItemStack.EMPTY); // 如果下一个物品被完全合并，设置为空
                    }
                }
            }
        }
    }

    public static void tryAddItemStackToItemStacks(ItemStack item, List<ItemStack> items){
        // 尝试将新物品堆叠到已有的相同物品槽中
        for (ItemStack current : items) {
            if (!current.isEmpty() && current.is(item.getItem()) && current.getCount() < current.getMaxStackSize()) {
                // 计算可以堆叠的数量
                int transfer = Math.min(item.getCount(), current.getMaxStackSize() - current.getCount());
                current.grow(transfer); // 增加当前物品的数量
                item.shrink(transfer);  // 减少新物品的数量
                if (item.isEmpty()) {
                    break; // 如果新物品被完全堆叠，退出循环
                }
            }
        }
    }

}