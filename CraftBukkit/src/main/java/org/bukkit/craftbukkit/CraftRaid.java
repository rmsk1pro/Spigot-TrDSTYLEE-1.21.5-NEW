package org.bukkit.craftbukkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.level.World;
import org.bukkit.Location;
import org.bukkit.Raid;
import org.bukkit.Raid.RaidStatus;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Raider;

public final class CraftRaid implements Raid {

    private final net.minecraft.world.entity.raid.Raid handle;
    private final World world;

    public CraftRaid(net.minecraft.world.entity.raid.Raid handle, World world) {
        this.handle = handle;
        this.world = world;
    }

    @Override
    public boolean isStarted() {
        return handle.isStarted();
    }

    @Override
    public long getActiveTicks() {
        return handle.ticksActive;
    }

    @Override
    public int getBadOmenLevel() {
        return handle.raidOmenLevel;
    }

    @Override
    public void setBadOmenLevel(int badOmenLevel) {
        int max = handle.getMaxRaidOmenLevel();
        Preconditions.checkArgument(0 <= badOmenLevel && badOmenLevel <= max, "Bad Omen level must be between 0 and %s", max);
        handle.raidOmenLevel = badOmenLevel;
    }

    @Override
    public Location getLocation() {
        BlockPosition pos = handle.getCenter();
        return CraftLocation.toBukkit(pos, world.getWorld());
    }

    @Override
    public RaidStatus getStatus() {
        if (handle.isStopped()) {
            return RaidStatus.STOPPED;
        } else if (handle.isVictory()) {
            return RaidStatus.VICTORY;
        } else if (handle.isLoss()) {
            return RaidStatus.LOSS;
        } else {
            return RaidStatus.ONGOING;
        }
    }

    @Override
    public int getSpawnedGroups() {
        return handle.getGroupsSpawned();
    }

    @Override
    public int getTotalGroups() {
        return handle.numGroups + (handle.raidOmenLevel > 1 ? 1 : 0);
    }

    @Override
    public int getTotalWaves() {
        return handle.numGroups;
    }

    @Override
    public float getTotalHealth() {
        return handle.getHealthOfLivingRaiders();
    }

    @Override
    public Set<UUID> getHeroes() {
        return Collections.unmodifiableSet(handle.heroesOfTheVillage);
    }

    @Override
    public List<Raider> getRaiders() {
        return handle.getRaiders().stream().map(new Function<EntityRaider, Raider>() {
            @Override
            public Raider apply(EntityRaider entityRaider) {
                return (Raider) entityRaider.getBukkitEntity();
            }
        }).collect(ImmutableList.toImmutableList());
    }

    public net.minecraft.world.entity.raid.Raid getHandle() {
        return handle;
    }
}
