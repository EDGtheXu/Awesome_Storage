package com.github.edg_thexu.awesome_storage.core.manager;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class QueuedRecipe {

    public static final String TAG_ID = "RecipeId";
    public static final String TAG_TYPE = "RecipeType";
    public static final String TAG_QUANTITY = "Quantity";
    public static final String TAG_PROGRESS = "Progress";
    public static final String TAG_TOTAL_TIME = "TotalTime";
    public static final String TAG_PLAYER = "Player";

    public ResourceLocation recipeId;
    public ResourceLocation recipeTypeId;
    public int quantity;
    public int progress;
    public int totalCookTime;
    public UUID playerId;

    public QueuedRecipe(ResourceLocation recipeId, ResourceLocation recipeTypeId, int quantity, int totalCookTime) {
        this(recipeId, recipeTypeId, quantity, totalCookTime, null);
    }

    public QueuedRecipe(ResourceLocation recipeId, ResourceLocation recipeTypeId, int quantity, int totalCookTime, UUID playerId) {
        this.recipeId = recipeId;
        this.recipeTypeId = recipeTypeId;
        this.quantity = quantity;
        this.progress = 0;
        this.totalCookTime = totalCookTime;
        this.playerId = playerId;
    }

    public boolean isDone() {
        return quantity <= 0;
    }

    public float getProgressRatio() {
        return totalCookTime > 0 ? (float) progress / totalCookTime : 1f;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ID, recipeId.toString());
        tag.putString(TAG_TYPE, recipeTypeId.toString());
        tag.putInt(TAG_QUANTITY, quantity);
        tag.putInt(TAG_PROGRESS, progress);
        tag.putInt(TAG_TOTAL_TIME, totalCookTime);
        if (playerId != null) tag.putUUID(TAG_PLAYER, playerId);
        return tag;
    }

    public static QueuedRecipe load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.parse(tag.getString(TAG_ID));
        ResourceLocation type = ResourceLocation.parse(tag.getString(TAG_TYPE));
        int qty = tag.getInt(TAG_QUANTITY);
        int total = tag.getInt(TAG_TOTAL_TIME);
        QueuedRecipe r = new QueuedRecipe(id, type, qty, total);
        r.progress = tag.getInt(TAG_PROGRESS);
        if (tag.contains(TAG_PLAYER)) r.playerId = tag.getUUID(TAG_PLAYER);
        return r;
    }
}
