package net.minecraft.world.inventory;

import net.minecraft.core.IRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

public class Containers<T extends Container> implements FeatureElement {

    public static final Containers<ContainerChest> GENERIC_9x1 = register("generic_9x1", ContainerChest::oneRow);
    public static final Containers<ContainerChest> GENERIC_9x2 = register("generic_9x2", ContainerChest::twoRows);
    public static final Containers<ContainerChest> GENERIC_9x3 = register("generic_9x3", ContainerChest::threeRows);
    public static final Containers<ContainerChest> GENERIC_9x4 = register("generic_9x4", ContainerChest::fourRows);
    public static final Containers<ContainerChest> GENERIC_9x5 = register("generic_9x5", ContainerChest::fiveRows);
    public static final Containers<ContainerChest> GENERIC_9x6 = register("generic_9x6", ContainerChest::sixRows);
    public static final Containers<ContainerDispenser> GENERIC_3x3 = register("generic_3x3", ContainerDispenser::new);
    public static final Containers<CrafterMenu> CRAFTER_3x3 = register("crafter_3x3", CrafterMenu::new);
    public static final Containers<ContainerAnvil> ANVIL = register("anvil", ContainerAnvil::new);
    public static final Containers<ContainerBeacon> BEACON = register("beacon", ContainerBeacon::new);
    public static final Containers<ContainerBlastFurnace> BLAST_FURNACE = register("blast_furnace", ContainerBlastFurnace::new);
    public static final Containers<ContainerBrewingStand> BREWING_STAND = register("brewing_stand", ContainerBrewingStand::new);
    public static final Containers<ContainerWorkbench> CRAFTING = register("crafting", ContainerWorkbench::new);
    public static final Containers<ContainerEnchantTable> ENCHANTMENT = register("enchantment", ContainerEnchantTable::new);
    public static final Containers<ContainerFurnaceFurnace> FURNACE = register("furnace", ContainerFurnaceFurnace::new);
    public static final Containers<ContainerGrindstone> GRINDSTONE = register("grindstone", ContainerGrindstone::new);
    public static final Containers<ContainerHopper> HOPPER = register("hopper", ContainerHopper::new);
    public static final Containers<ContainerLectern> LECTERN = register("lectern", (i, playerinventory) -> {
        return new ContainerLectern(i);
    });
    public static final Containers<ContainerLoom> LOOM = register("loom", ContainerLoom::new);
    public static final Containers<ContainerMerchant> MERCHANT = register("merchant", ContainerMerchant::new);
    public static final Containers<ContainerShulkerBox> SHULKER_BOX = register("shulker_box", ContainerShulkerBox::new);
    public static final Containers<ContainerSmithing> SMITHING = register("smithing", ContainerSmithing::new);
    public static final Containers<ContainerSmoker> SMOKER = register("smoker", ContainerSmoker::new);
    public static final Containers<ContainerCartography> CARTOGRAPHY_TABLE = register("cartography_table", ContainerCartography::new);
    public static final Containers<ContainerStonecutter> STONECUTTER = register("stonecutter", ContainerStonecutter::new);
    private final FeatureFlagSet requiredFeatures;
    private final Containers.Supplier<T> constructor;

    private static <T extends Container> Containers<T> register(String s, Containers.Supplier<T> containers_supplier) {
        return (Containers) IRegistry.register(BuiltInRegistries.MENU, s, new Containers(containers_supplier, FeatureFlags.VANILLA_SET));
    }

    private static <T extends Container> Containers<T> register(String s, Containers.Supplier<T> containers_supplier, FeatureFlag... afeatureflag) {
        return (Containers) IRegistry.register(BuiltInRegistries.MENU, s, new Containers(containers_supplier, FeatureFlags.REGISTRY.subset(afeatureflag)));
    }

    private Containers(Containers.Supplier<T> containers_supplier, FeatureFlagSet featureflagset) {
        this.constructor = containers_supplier;
        this.requiredFeatures = featureflagset;
    }

    public T create(int i, PlayerInventory playerinventory) {
        return this.constructor.create(i, playerinventory);
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.requiredFeatures;
    }

    private interface Supplier<T extends Container> {

        T create(int i, PlayerInventory playerinventory);
    }
}
