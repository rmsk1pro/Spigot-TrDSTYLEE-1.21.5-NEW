package net.minecraft.world.level.block.entity;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IWorldInventory;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerBrewingStand;
import net.minecraft.world.inventory.IContainerProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewer;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockBrewingStand;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityBrewingStand extends TileEntityContainer implements IWorldInventory {

    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;
    private static final int[] SLOTS_FOR_UP = new int[]{3};
    private static final int[] SLOTS_FOR_DOWN = new int[]{0, 1, 2, 3};
    private static final int[] SLOTS_FOR_SIDES = new int[]{0, 1, 2, 4};
    public static final int FUEL_USES = 20;
    public static final int DATA_BREW_TIME = 0;
    public static final int DATA_FUEL_USES = 1;
    public static final int NUM_DATA_VALUES = 2;
    private static final short DEFAULT_BREW_TIME = 0;
    private static final byte DEFAULT_FUEL = 0;
    private NonNullList<ItemStack> items;
    public int brewTime;
    private boolean[] lastPotionCount;
    private Item ingredient;
    public int fuel;
    protected final IContainerProperties dataAccess;

    public TileEntityBrewingStand(BlockPosition blockposition, IBlockData iblockdata) {
        super(TileEntityTypes.BREWING_STAND, blockposition, iblockdata);
        this.items = NonNullList.<ItemStack>withSize(5, ItemStack.EMPTY);
        this.dataAccess = new IContainerProperties() {
            @Override
            public int get(int i) {
                int j;

                switch (i) {
                    case 0:
                        j = TileEntityBrewingStand.this.brewTime;
                        break;
                    case 1:
                        j = TileEntityBrewingStand.this.fuel;
                        break;
                    default:
                        j = 0;
                }

                return j;
            }

            @Override
            public void set(int i, int j) {
                switch (i) {
                    case 0:
                        TileEntityBrewingStand.this.brewTime = j;
                        break;
                    case 1:
                        TileEntityBrewingStand.this.fuel = j;
                }

            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    protected IChatBaseComponent getDefaultName() {
        return IChatBaseComponent.translatable("container.brewing");
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonnulllist) {
        this.items = nonnulllist;
    }

    public static void serverTick(World world, BlockPosition blockposition, IBlockData iblockdata, TileEntityBrewingStand tileentitybrewingstand) {
        ItemStack itemstack = tileentitybrewingstand.items.get(4);

        if (tileentitybrewingstand.fuel <= 0 && itemstack.is(TagsItem.BREWING_FUEL)) {
            tileentitybrewingstand.fuel = 20;
            itemstack.shrink(1);
            setChanged(world, blockposition, iblockdata);
        }

        boolean flag = isBrewable(world.potionBrewing(), tileentitybrewingstand.items);
        boolean flag1 = tileentitybrewingstand.brewTime > 0;
        ItemStack itemstack1 = tileentitybrewingstand.items.get(3);

        if (flag1) {
            --tileentitybrewingstand.brewTime;
            boolean flag2 = tileentitybrewingstand.brewTime == 0;

            if (flag2 && flag) {
                doBrew(world, blockposition, tileentitybrewingstand.items);
            } else if (!flag || !itemstack1.is(tileentitybrewingstand.ingredient)) {
                tileentitybrewingstand.brewTime = 0;
            }

            setChanged(world, blockposition, iblockdata);
        } else if (flag && tileentitybrewingstand.fuel > 0) {
            --tileentitybrewingstand.fuel;
            tileentitybrewingstand.brewTime = 400;
            tileentitybrewingstand.ingredient = itemstack1.getItem();
            setChanged(world, blockposition, iblockdata);
        }

        boolean[] aboolean = tileentitybrewingstand.getPotionBits();

        if (!Arrays.equals(aboolean, tileentitybrewingstand.lastPotionCount)) {
            tileentitybrewingstand.lastPotionCount = aboolean;
            IBlockData iblockdata1 = iblockdata;

            if (!(iblockdata.getBlock() instanceof BlockBrewingStand)) {
                return;
            }

            for (int i = 0; i < BlockBrewingStand.HAS_BOTTLE.length; ++i) {
                iblockdata1 = (IBlockData) iblockdata1.setValue(BlockBrewingStand.HAS_BOTTLE[i], aboolean[i]);
            }

            world.setBlock(blockposition, iblockdata1, 2);
        }

    }

    private boolean[] getPotionBits() {
        boolean[] aboolean = new boolean[3];

        for (int i = 0; i < 3; ++i) {
            if (!((ItemStack) this.items.get(i)).isEmpty()) {
                aboolean[i] = true;
            }
        }

        return aboolean;
    }

    private static boolean isBrewable(PotionBrewer potionbrewer, NonNullList<ItemStack> nonnulllist) {
        ItemStack itemstack = nonnulllist.get(3);

        if (itemstack.isEmpty()) {
            return false;
        } else if (!potionbrewer.isIngredient(itemstack)) {
            return false;
        } else {
            for (int i = 0; i < 3; ++i) {
                ItemStack itemstack1 = nonnulllist.get(i);

                if (!itemstack1.isEmpty() && potionbrewer.hasMix(itemstack1, itemstack)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static void doBrew(World world, BlockPosition blockposition, NonNullList<ItemStack> nonnulllist) {
        ItemStack itemstack = nonnulllist.get(3);
        PotionBrewer potionbrewer = world.potionBrewing();

        for (int i = 0; i < 3; ++i) {
            nonnulllist.set(i, potionbrewer.mix(itemstack, nonnulllist.get(i)));
        }

        itemstack.shrink(1);
        ItemStack itemstack1 = itemstack.getItem().getCraftingRemainder();

        if (!itemstack1.isEmpty()) {
            if (itemstack.isEmpty()) {
                itemstack = itemstack1;
            } else {
                InventoryUtils.dropItemStack(world, (double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ(), itemstack1);
            }
        }

        nonnulllist.set(3, itemstack);
        world.levelEvent(1035, blockposition, 0);
    }

    @Override
    protected void loadAdditional(NBTTagCompound nbttagcompound, HolderLookup.a holderlookup_a) {
        super.loadAdditional(nbttagcompound, holderlookup_a);
        this.items = NonNullList.<ItemStack>withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerUtil.loadAllItems(nbttagcompound, this.items, holderlookup_a);
        this.brewTime = nbttagcompound.getShortOr("BrewTime", (short) 0);
        if (this.brewTime > 0) {
            this.ingredient = ((ItemStack) this.items.get(3)).getItem();
        }

        this.fuel = nbttagcompound.getByteOr("Fuel", (byte) 0);
    }

    @Override
    protected void saveAdditional(NBTTagCompound nbttagcompound, HolderLookup.a holderlookup_a) {
        super.saveAdditional(nbttagcompound, holderlookup_a);
        nbttagcompound.putShort("BrewTime", (short) this.brewTime);
        ContainerUtil.saveAllItems(nbttagcompound, this.items, holderlookup_a);
        nbttagcompound.putByte("Fuel", (byte) this.fuel);
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack itemstack) {
        if (i == 3) {
            PotionBrewer potionbrewer = this.level != null ? this.level.potionBrewing() : PotionBrewer.EMPTY;

            return potionbrewer.isIngredient(itemstack);
        } else {
            return i == 4 ? itemstack.is(TagsItem.BREWING_FUEL) : (itemstack.is(Items.POTION) || itemstack.is(Items.SPLASH_POTION) || itemstack.is(Items.LINGERING_POTION) || itemstack.is(Items.GLASS_BOTTLE)) && this.getItem(i).isEmpty();
        }
    }

    @Override
    public int[] getSlotsForFace(EnumDirection enumdirection) {
        return enumdirection == EnumDirection.UP ? TileEntityBrewingStand.SLOTS_FOR_UP : (enumdirection == EnumDirection.DOWN ? TileEntityBrewingStand.SLOTS_FOR_DOWN : TileEntityBrewingStand.SLOTS_FOR_SIDES);
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemstack, @Nullable EnumDirection enumdirection) {
        return this.canPlaceItem(i, itemstack);
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemstack, EnumDirection enumdirection) {
        return i == 3 ? itemstack.is(Items.GLASS_BOTTLE) : true;
    }

    @Override
    protected Container createMenu(int i, PlayerInventory playerinventory) {
        return new ContainerBrewingStand(i, playerinventory, this, this.dataAccess);
    }
}
