package net.minecraft.world.item.crafting;

import net.minecraft.core.IRegistry;
import net.minecraft.core.registries.BuiltInRegistries;

public class RecipeBookCategories {

    public static final RecipeBookCategory CRAFTING_BUILDING_BLOCKS = register("crafting_building_blocks");
    public static final RecipeBookCategory CRAFTING_REDSTONE = register("crafting_redstone");
    public static final RecipeBookCategory CRAFTING_EQUIPMENT = register("crafting_equipment");
    public static final RecipeBookCategory CRAFTING_MISC = register("crafting_misc");
    public static final RecipeBookCategory FURNACE_FOOD = register("furnace_food");
    public static final RecipeBookCategory FURNACE_BLOCKS = register("furnace_blocks");
    public static final RecipeBookCategory FURNACE_MISC = register("furnace_misc");
    public static final RecipeBookCategory BLAST_FURNACE_BLOCKS = register("blast_furnace_blocks");
    public static final RecipeBookCategory BLAST_FURNACE_MISC = register("blast_furnace_misc");
    public static final RecipeBookCategory SMOKER_FOOD = register("smoker_food");
    public static final RecipeBookCategory STONECUTTER = register("stonecutter");
    public static final RecipeBookCategory SMITHING = register("smithing");
    public static final RecipeBookCategory CAMPFIRE = register("campfire");

    public RecipeBookCategories() {}

    private static RecipeBookCategory register(String s) {
        return (RecipeBookCategory) IRegistry.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY, s, new RecipeBookCategory());
    }

    public static RecipeBookCategory bootstrap(IRegistry<RecipeBookCategory> iregistry) {
        return RecipeBookCategories.CAMPFIRE;
    }
}
