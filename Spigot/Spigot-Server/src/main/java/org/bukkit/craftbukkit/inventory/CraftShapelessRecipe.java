package org.bukkit.craftbukkit.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.ShapelessRecipes;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

public class CraftShapelessRecipe extends ShapelessRecipe implements CraftRecipe {
    // TODO: Could eventually use this to add a matches() method or some such
    private ShapelessRecipes recipe;

    public CraftShapelessRecipe(NamespacedKey key, ItemStack result) {
        super(key, result);
    }

    public CraftShapelessRecipe(NamespacedKey key, ItemStack result, ShapelessRecipes recipe) {
        this(key, result);
        this.recipe = recipe;
    }

    public static CraftShapelessRecipe fromBukkitRecipe(ShapelessRecipe recipe) {
        if (recipe instanceof CraftShapelessRecipe) {
            return (CraftShapelessRecipe) recipe;
        }
        CraftShapelessRecipe ret = new CraftShapelessRecipe(recipe.getKey(), recipe.getResult());
        ret.setGroup(recipe.getGroup());
        ret.setCategory(recipe.getCategory());
        for (RecipeChoice ingred : recipe.getChoiceList()) {
            ret.addIngredient(ingred);
        }
        return ret;
    }

    @Override
    public void addToCraftingManager() {
        List<org.bukkit.inventory.RecipeChoice> ingred = this.getChoiceList();
        List<RecipeItemStack> data = new ArrayList<>(ingred.size());
        for (org.bukkit.inventory.RecipeChoice i : ingred) {
            data.add(toNMS(i, true));
        }

        MinecraftServer.getServer().getRecipeManager().addRecipe(new RecipeHolder<>(CraftRecipe.toMinecraft(this.getKey()), new ShapelessRecipes(this.getGroup(), CraftRecipe.getCategory(this.getCategory()), CraftItemStack.asNMSCopy(this.getResult()), data)));
    }
}
