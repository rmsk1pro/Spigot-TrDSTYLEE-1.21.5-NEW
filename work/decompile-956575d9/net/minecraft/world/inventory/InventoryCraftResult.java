package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class InventoryCraftResult implements IInventory, RecipeCraftingHolder {

    private final NonNullList<ItemStack> itemStacks;
    @Nullable
    private RecipeHolder<?> recipeUsed;

    public InventoryCraftResult() {
        this.itemStacks = NonNullList.<ItemStack>withSize(1, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.itemStacks) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int i) {
        return this.itemStacks.get(0);
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        return ContainerUtil.takeItem(this.itemStacks, 0);
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return ContainerUtil.takeItem(this.itemStacks, 0);
    }

    @Override
    public void setItem(int i, ItemStack itemstack) {
        this.itemStacks.set(0, itemstack);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(EntityHuman entityhuman) {
        return true;
    }

    @Override
    public void clearContent() {
        this.itemStacks.clear();
    }

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> recipeholder) {
        this.recipeUsed = recipeholder;
    }

    @Nullable
    @Override
    public RecipeHolder<?> getRecipeUsed() {
        return this.recipeUsed;
    }
}
