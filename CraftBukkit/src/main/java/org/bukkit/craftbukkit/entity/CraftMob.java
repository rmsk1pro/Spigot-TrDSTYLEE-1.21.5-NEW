package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import java.util.Optional;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.entity.EntityInsentient;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.CraftLootTable;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.loot.LootTable;

public abstract class CraftMob extends CraftLivingEntity implements Mob {
    public CraftMob(CraftServer server, EntityInsentient entity) {
        super(server, entity);
    }

    @Override
    public void setTarget(LivingEntity target) {
        Preconditions.checkState(!getHandle().generation, "Cannot set target during world generation");

        EntityInsentient entity = getHandle();
        if (target == null) {
            entity.setTarget(null, null, false);
        } else if (target instanceof CraftLivingEntity) {
            entity.setTarget(((CraftLivingEntity) target).getHandle(), null, false);
        }
    }

    @Override
    public CraftLivingEntity getTarget() {
        if (getHandle().getTarget() == null) return null;

        return (CraftLivingEntity) getHandle().getTarget().getBukkitEntity();
    }

    @Override
    public void setAware(boolean aware) {
        getHandle().aware = aware;
    }

    @Override
    public boolean isAware() {
        return getHandle().aware;
    }

    @Override
    public Sound getAmbientSound() {
        SoundEffect sound = getHandle().getAmbientSound0();
        return (sound != null) ? CraftSound.minecraftToBukkit(sound) : null;
    }

    @Override
    public EntityInsentient getHandle() {
        return (EntityInsentient) entity;
    }

    @Override
    public String toString() {
        return "CraftMob";
    }

    @Override
    public void setLootTable(LootTable table) {
        getHandle().lootTable = Optional.ofNullable(CraftLootTable.bukkitToMinecraft(table));
    }

    @Override
    public LootTable getLootTable() {
        return CraftLootTable.minecraftToBukkit(getHandle().getLootTable().orElse(null));
    }

    @Override
    public void setSeed(long seed) {
        getHandle().lootTableSeed = seed;
    }

    @Override
    public long getSeed() {
        return getHandle().lootTableSeed;
    }
}
