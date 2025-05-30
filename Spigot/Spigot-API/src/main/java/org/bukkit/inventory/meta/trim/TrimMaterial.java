package org.bukkit.inventory.meta.trim;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Translatable;
import org.bukkit.registry.RegistryAware;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a material that may be used in an {@link ArmorTrim}.
 */
public interface TrimMaterial extends Keyed, Translatable, RegistryAware {

    /**
     * {@link Material#QUARTZ}.
     */
    public static final TrimMaterial QUARTZ = getTrimMaterial("quartz");
    /**
     * {@link Material#IRON_INGOT}.
     */
    public static final TrimMaterial IRON = getTrimMaterial("iron");
    /**
     * {@link Material#NETHERITE_INGOT}.
     */
    public static final TrimMaterial NETHERITE = getTrimMaterial("netherite");
    /**
     * {@link Material#REDSTONE}.
     */
    public static final TrimMaterial REDSTONE = getTrimMaterial("redstone");
    /**
     * {@link Material#COPPER_INGOT}.
     */
    public static final TrimMaterial COPPER = getTrimMaterial("copper");
    /**
     * {@link Material#GOLD_INGOT}.
     */
    public static final TrimMaterial GOLD = getTrimMaterial("gold");
    /**
     * {@link Material#EMERALD}.
     */
    public static final TrimMaterial EMERALD = getTrimMaterial("emerald");
    /**
     * {@link Material#DIAMOND}.
     */
    public static final TrimMaterial DIAMOND = getTrimMaterial("diamond");
    /**
     * {@link Material#LAPIS_LAZULI}.
     */
    public static final TrimMaterial LAPIS = getTrimMaterial("lapis");
    /**
     * {@link Material#AMETHYST_SHARD}.
     */
    public static final TrimMaterial AMETHYST = getTrimMaterial("amethyst");
    /**
     * {@link Material#RESIN_BRICK}.
     */
    public static final TrimMaterial RESIN = getTrimMaterial("resin");

    @NotNull
    private static TrimMaterial getTrimMaterial(@NotNull String key) {
        return Registry.TRIM_MATERIAL.getOrThrow(NamespacedKey.minecraft(key));
    }

    /**
     * {@inheritDoc}
     *
     * @see #getKeyOrThrow()
     * @see #isRegistered()
     * @deprecated A key might not always be present, use {@link #getKeyOrThrow()} instead.
     */
    @NotNull
    @Override
    @Deprecated(since = "1.21.4")
    NamespacedKey getKey();
}
