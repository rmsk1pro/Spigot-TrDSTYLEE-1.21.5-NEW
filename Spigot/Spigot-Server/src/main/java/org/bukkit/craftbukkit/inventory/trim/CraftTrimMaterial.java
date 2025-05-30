package org.bukkit.craftbukkit.inventory.trim;

import com.google.common.base.Preconditions;
import net.minecraft.core.Holder;
import net.minecraft.core.IRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.registry.CraftRegistryItem;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.jetbrains.annotations.NotNull;

public class CraftTrimMaterial extends CraftRegistryItem<net.minecraft.world.item.equipment.trim.TrimMaterial> implements TrimMaterial {

    public static TrimMaterial minecraftToBukkit(net.minecraft.world.item.equipment.trim.TrimMaterial minecraft) {
        return CraftRegistry.minecraftToBukkit(minecraft, Registries.TRIM_MATERIAL, Registry.TRIM_MATERIAL);
    }

    public static TrimMaterial minecraftHolderToBukkit(Holder<net.minecraft.world.item.equipment.trim.TrimMaterial> minecraft) {
        return minecraftToBukkit(minecraft.value());
    }

    public static net.minecraft.world.item.equipment.trim.TrimMaterial bukkitToMinecraft(TrimMaterial bukkit) {
        return CraftRegistry.bukkitToMinecraft(bukkit);
    }

    public static Holder<net.minecraft.world.item.equipment.trim.TrimMaterial> bukkitToMinecraftHolder(TrimMaterial bukkit) {
        Preconditions.checkArgument(bukkit != null);

        IRegistry<net.minecraft.world.item.equipment.trim.TrimMaterial> registry = CraftRegistry.getMinecraftRegistry(Registries.TRIM_MATERIAL);

        if (registry.wrapAsHolder(bukkitToMinecraft(bukkit)) instanceof Holder.c<net.minecraft.world.item.equipment.trim.TrimMaterial> holder) {
            return holder;
        }

        throw new IllegalArgumentException("No Reference holder found for " + bukkit
                + ", this can happen if a plugin creates its own trim material without properly registering it.");
    }

    public CraftTrimMaterial(NamespacedKey key, Holder<net.minecraft.world.item.equipment.trim.TrimMaterial> handle) {
        super(key, handle);
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return getKeyOrThrow();
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return ((TranslatableContents) getHandle().description().getContents()).getKey();
    }
}
