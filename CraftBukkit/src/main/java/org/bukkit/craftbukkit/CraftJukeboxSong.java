package org.bukkit.craftbukkit;

import com.google.common.base.Preconditions;
import net.minecraft.core.Holder;
import net.minecraft.core.IRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.bukkit.JukeboxSong;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.registry.CraftRegistryItem;
import org.jetbrains.annotations.NotNull;

public class CraftJukeboxSong extends CraftRegistryItem<net.minecraft.world.item.JukeboxSong> implements JukeboxSong {

    public static JukeboxSong minecraftToBukkit(net.minecraft.world.item.JukeboxSong minecraft) {
        return CraftRegistry.minecraftToBukkit(minecraft, Registries.JUKEBOX_SONG, Registry.JUKEBOX_SONG);
    }

    public static JukeboxSong minecraftHolderToBukkit(Holder<net.minecraft.world.item.JukeboxSong> minecraft) {
        return minecraftToBukkit(minecraft.value());
    }

    public static net.minecraft.world.item.JukeboxSong bukkitToMinecraft(JukeboxSong bukkit) {
        return CraftRegistry.bukkitToMinecraft(bukkit);
    }

    public static Holder<net.minecraft.world.item.JukeboxSong> bukkitToMinecraftHolder(JukeboxSong bukkit) {
        Preconditions.checkArgument(bukkit != null);

        IRegistry<net.minecraft.world.item.JukeboxSong> registry = CraftRegistry.getMinecraftRegistry(Registries.JUKEBOX_SONG);

        if (registry.wrapAsHolder(bukkitToMinecraft(bukkit)) instanceof Holder.c<net.minecraft.world.item.JukeboxSong> holder) {
            return holder;
        }

        throw new IllegalArgumentException("No Reference holder found for " + bukkit
                + ", this can happen if a plugin creates its own trim pattern without properly registering it.");
    }

    public CraftJukeboxSong(NamespacedKey key, Holder<net.minecraft.world.item.JukeboxSong> handle) {
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
