package org.bukkit.craftbukkit.inventory.components;

import com.google.common.base.Preconditions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.JukeboxPlayable;
import org.bukkit.JukeboxSong;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.craftbukkit.CraftJukeboxSong;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.inventory.SerializableMeta;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.meta.components.JukeboxPlayableComponent;

@SerializableAs("JukeboxPlayable")
public final class CraftJukeboxComponent implements JukeboxPlayableComponent {

    private JukeboxPlayable handle;

    public CraftJukeboxComponent(JukeboxPlayable jukebox) {
        this.handle = jukebox;
    }

    public CraftJukeboxComponent(CraftJukeboxComponent jukebox) {
        this.handle = jukebox.handle;
    }

    public CraftJukeboxComponent(Map<String, Object> map) {
        String song = SerializableMeta.getObject(String.class, map, "song", false);

        this.handle = new JukeboxPlayable(new EitherHolder<>(ResourceKey.create(Registries.JUKEBOX_SONG, MinecraftKey.parse(song))));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("song", getSongKey().toString());
        return result;
    }

    public JukeboxPlayable getHandle() {
        return handle;
    }

    @Override
    public JukeboxSong getSong() {
        Optional<Holder<net.minecraft.world.item.JukeboxSong>> song = handle.song().unwrap(CraftRegistry.getMinecraftRegistry());
        return (song.isPresent()) ? CraftJukeboxSong.minecraftHolderToBukkit(song.get()) : null;
    }

    @Override
    public NamespacedKey getSongKey() {
        return handle.song().key().map(ResourceKey::location).map(CraftNamespacedKey::fromMinecraft).orElse(null);
    }

    @Override
    public void setSong(JukeboxSong song) {
        Preconditions.checkArgument(song != null, "song cannot be null");

        handle = new JukeboxPlayable(new EitherHolder<>(CraftJukeboxSong.bukkitToMinecraftHolder(song)));
    }

    @Override
    public void setSongKey(NamespacedKey song) {
        Preconditions.checkArgument(song != null, "song cannot be null");

        handle = new JukeboxPlayable(new EitherHolder<>(ResourceKey.create(Registries.JUKEBOX_SONG, CraftNamespacedKey.toMinecraft(song))));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.handle);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CraftJukeboxComponent other = (CraftJukeboxComponent) obj;
        return Objects.equals(this.handle, other.handle);
    }

    @Override
    public String toString() {
        return "CraftJukeboxComponent{" + "handle=" + handle + '}';
    }
}
