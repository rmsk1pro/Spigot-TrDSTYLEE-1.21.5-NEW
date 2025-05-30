package net.minecraft.world.level.block.entity;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import java.util.Collections;
import java.util.SequencedSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.block.Blocks;

public class FuelValues {

    private final Object2IntSortedMap<Item> values;

    FuelValues(Object2IntSortedMap<Item> object2intsortedmap) {
        this.values = object2intsortedmap;
    }

    public boolean isFuel(ItemStack itemstack) {
        return this.values.containsKey(itemstack.getItem());
    }

    public SequencedSet<Item> fuelItems() {
        return Collections.unmodifiableSequencedSet(this.values.keySet());
    }

    public int burnDuration(ItemStack itemstack) {
        return itemstack.isEmpty() ? 0 : this.values.getInt(itemstack.getItem());
    }

    public static FuelValues vanillaBurnTimes(HolderLookup.a holderlookup_a, FeatureFlagSet featureflagset) {
        return vanillaBurnTimes(holderlookup_a, featureflagset, 200);
    }

    public static FuelValues vanillaBurnTimes(HolderLookup.a holderlookup_a, FeatureFlagSet featureflagset, int i) {
        return (new FuelValues.a(holderlookup_a, featureflagset)).add(Items.LAVA_BUCKET, i * 100).add(Blocks.COAL_BLOCK, i * 8 * 10).add(Items.BLAZE_ROD, i * 12).add(Items.COAL, i * 8).add(Items.CHARCOAL, i * 8).add(TagsItem.LOGS, i * 3 / 2).add(TagsItem.BAMBOO_BLOCKS, i * 3 / 2).add(TagsItem.PLANKS, i * 3 / 2).add(Blocks.BAMBOO_MOSAIC, i * 3 / 2).add(TagsItem.WOODEN_STAIRS, i * 3 / 2).add(Blocks.BAMBOO_MOSAIC_STAIRS, i * 3 / 2).add(TagsItem.WOODEN_SLABS, i * 3 / 4).add(Blocks.BAMBOO_MOSAIC_SLAB, i * 3 / 4).add(TagsItem.WOODEN_TRAPDOORS, i * 3 / 2).add(TagsItem.WOODEN_PRESSURE_PLATES, i * 3 / 2).add(TagsItem.WOODEN_FENCES, i * 3 / 2).add(TagsItem.FENCE_GATES, i * 3 / 2).add(Blocks.NOTE_BLOCK, i * 3 / 2).add(Blocks.BOOKSHELF, i * 3 / 2).add(Blocks.CHISELED_BOOKSHELF, i * 3 / 2).add(Blocks.LECTERN, i * 3 / 2).add(Blocks.JUKEBOX, i * 3 / 2).add(Blocks.CHEST, i * 3 / 2).add(Blocks.TRAPPED_CHEST, i * 3 / 2).add(Blocks.CRAFTING_TABLE, i * 3 / 2).add(Blocks.DAYLIGHT_DETECTOR, i * 3 / 2).add(TagsItem.BANNERS, i * 3 / 2).add(Items.BOW, i * 3 / 2).add(Items.FISHING_ROD, i * 3 / 2).add(Blocks.LADDER, i * 3 / 2).add(TagsItem.SIGNS, i).add(TagsItem.HANGING_SIGNS, i * 4).add(Items.WOODEN_SHOVEL, i).add(Items.WOODEN_SWORD, i).add(Items.WOODEN_HOE, i).add(Items.WOODEN_AXE, i).add(Items.WOODEN_PICKAXE, i).add(TagsItem.WOODEN_DOORS, i).add(TagsItem.BOATS, i * 6).add(TagsItem.WOOL, i / 2).add(TagsItem.WOODEN_BUTTONS, i / 2).add(Items.STICK, i / 2).add(TagsItem.SAPLINGS, i / 2).add(Items.BOWL, i / 2).add(TagsItem.WOOL_CARPETS, 1 + i / 3).add(Blocks.DRIED_KELP_BLOCK, 1 + i * 20).add(Items.CROSSBOW, i * 3 / 2).add(Blocks.BAMBOO, i / 4).add(Blocks.DEAD_BUSH, i / 2).add(Blocks.SHORT_DRY_GRASS, i / 2).add(Blocks.TALL_DRY_GRASS, i / 2).add(Blocks.SCAFFOLDING, i / 4).add(Blocks.LOOM, i * 3 / 2).add(Blocks.BARREL, i * 3 / 2).add(Blocks.CARTOGRAPHY_TABLE, i * 3 / 2).add(Blocks.FLETCHING_TABLE, i * 3 / 2).add(Blocks.SMITHING_TABLE, i * 3 / 2).add(Blocks.COMPOSTER, i * 3 / 2).add(Blocks.AZALEA, i / 2).add(Blocks.FLOWERING_AZALEA, i / 2).add(Blocks.MANGROVE_ROOTS, i * 3 / 2).add(Blocks.LEAF_LITTER, i / 2).remove(TagsItem.NON_FLAMMABLE_WOOD).build();
    }

    public static class a {

        private final HolderLookup<Item> items;
        private final FeatureFlagSet enabledFeatures;
        private final Object2IntSortedMap<Item> values = new Object2IntLinkedOpenHashMap();

        public a(HolderLookup.a holderlookup_a, FeatureFlagSet featureflagset) {
            this.items = holderlookup_a.lookupOrThrow(Registries.ITEM);
            this.enabledFeatures = featureflagset;
        }

        public FuelValues build() {
            return new FuelValues(this.values);
        }

        public FuelValues.a remove(TagKey<Item> tagkey) {
            this.values.keySet().removeIf((item) -> {
                return item.builtInRegistryHolder().is(tagkey);
            });
            return this;
        }

        public FuelValues.a add(TagKey<Item> tagkey, int i) {
            this.items.get(tagkey).ifPresent((holderset_named) -> {
                for (Holder<Item> holder : holderset_named) {
                    this.putInternal(i, holder.value());
                }

            });
            return this;
        }

        public FuelValues.a add(IMaterial imaterial, int i) {
            Item item = imaterial.asItem();

            this.putInternal(i, item);
            return this;
        }

        private void putInternal(int i, Item item) {
            if (item.isEnabled(this.enabledFeatures)) {
                this.values.put(item, i);
            }

        }
    }
}
