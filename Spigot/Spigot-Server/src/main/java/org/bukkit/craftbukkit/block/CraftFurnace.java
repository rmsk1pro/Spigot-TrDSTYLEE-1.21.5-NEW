package org.bukkit.craftbukkit.block;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.world.level.block.BlockFurnace;
import net.minecraft.world.level.block.entity.TileEntityFurnace;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Furnace;
import org.bukkit.craftbukkit.inventory.CraftInventoryFurnace;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Recipe;

public abstract class CraftFurnace<T extends TileEntityFurnace> extends CraftContainer<T> implements Furnace {

    public CraftFurnace(World world, T tileEntity) {
        super(world, tileEntity);
    }

    protected CraftFurnace(CraftFurnace<T> state, Location location) {
        super(state, location);
    }

    @Override
    public FurnaceInventory getSnapshotInventory() {
        return new CraftInventoryFurnace(this.getSnapshot());
    }

    @Override
    public FurnaceInventory getInventory() {
        if (!this.isPlaced()) {
            return this.getSnapshotInventory();
        }

        return new CraftInventoryFurnace(this.getTileEntity());
    }

    @Override
    public short getBurnTime() {
        return (short) this.getSnapshot().litTimeRemaining;
    }

    @Override
    public void setBurnTime(short burnTime) {
        this.getSnapshot().litTimeRemaining = burnTime;
        // SPIGOT-844: Allow lighting and relighting using this API
        this.data = this.data.setValue(BlockFurnace.LIT, burnTime > 0);
    }

    @Override
    public short getCookTime() {
        return (short) this.getSnapshot().cookingTimer;
    }

    @Override
    public void setCookTime(short cookTime) {
        this.getSnapshot().cookingTimer = cookTime;
    }

    @Override
    public int getCookTimeTotal() {
        return this.getSnapshot().cookingTotalTime;
    }

    @Override
    public void setCookTimeTotal(int cookTimeTotal) {
        this.getSnapshot().cookingTotalTime = cookTimeTotal;
    }

    @Override
    public Map<CookingRecipe<?>, Integer> getRecipesUsed() {
        ImmutableMap.Builder<CookingRecipe<?>, Integer> recipesUsed = ImmutableMap.builder();
        this.getSnapshot().recipesUsed.reference2IntEntrySet().fastForEach(entrySet -> {
            Recipe recipe = Bukkit.getRecipe(CraftNamespacedKey.fromMinecraft(entrySet.getKey().location()));
            if (recipe instanceof CookingRecipe<?> cookingRecipe) {
                recipesUsed.put(cookingRecipe, entrySet.getValue());
            }
        });

        return recipesUsed.build();
    }

    @Override
    public abstract CraftFurnace<T> copy();

    @Override
    public abstract CraftFurnace<T> copy(Location location);
}
