package net.minecraft.world;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;

public interface IInventory extends Clearable, Iterable<ItemStack> {

    float DEFAULT_DISTANCE_BUFFER = 4.0F;

    int getContainerSize();

    boolean isEmpty();

    ItemStack getItem(int i);

    ItemStack removeItem(int i, int j);

    ItemStack removeItemNoUpdate(int i);

    void setItem(int i, ItemStack itemstack);

    default int getMaxStackSize() {
        return 99;
    }

    default int getMaxStackSize(ItemStack itemstack) {
        return Math.min(this.getMaxStackSize(), itemstack.getMaxStackSize());
    }

    void setChanged();

    boolean stillValid(EntityHuman entityhuman);

    default void startOpen(EntityHuman entityhuman) {}

    default void stopOpen(EntityHuman entityhuman) {}

    default boolean canPlaceItem(int i, ItemStack itemstack) {
        return true;
    }

    default boolean canTakeItem(IInventory iinventory, int i, ItemStack itemstack) {
        return true;
    }

    default int countItem(Item item) {
        int i = 0;

        for (ItemStack itemstack : this) {
            if (itemstack.getItem().equals(item)) {
                i += itemstack.getCount();
            }
        }

        return i;
    }

    default boolean hasAnyOf(Set<Item> set) {
        return this.hasAnyMatching((itemstack) -> {
            return !itemstack.isEmpty() && set.contains(itemstack.getItem());
        });
    }

    default boolean hasAnyMatching(Predicate<ItemStack> predicate) {
        for (ItemStack itemstack : this) {
            if (predicate.test(itemstack)) {
                return true;
            }
        }

        return false;
    }

    static boolean stillValidBlockEntity(TileEntity tileentity, EntityHuman entityhuman) {
        return stillValidBlockEntity(tileentity, entityhuman, 4.0F);
    }

    static boolean stillValidBlockEntity(TileEntity tileentity, EntityHuman entityhuman, float f) {
        World world = tileentity.getLevel();
        BlockPosition blockposition = tileentity.getBlockPos();

        return world == null ? false : (world.getBlockEntity(blockposition) != tileentity ? false : entityhuman.canInteractWithBlock(blockposition, (double) f));
    }

    default Iterator<ItemStack> iterator() {
        return new IInventory.a(this);
    }

    public static class a implements Iterator<ItemStack> {

        private final IInventory container;
        private int index;
        private final int size;

        public a(IInventory iinventory) {
            this.container = iinventory;
            this.size = iinventory.getContainerSize();
        }

        public boolean hasNext() {
            return this.index < this.size;
        }

        public ItemStack next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            } else {
                return this.container.getItem(this.index++);
            }
        }
    }
}
