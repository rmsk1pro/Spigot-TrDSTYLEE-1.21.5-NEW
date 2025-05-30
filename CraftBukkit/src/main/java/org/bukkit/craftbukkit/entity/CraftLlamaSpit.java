package org.bukkit.craftbukkit.entity;

import net.minecraft.world.entity.projectile.EntityLlamaSpit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.projectiles.ProjectileSource;

public class CraftLlamaSpit extends CraftProjectile implements LlamaSpit {

    public CraftLlamaSpit(CraftServer server, EntityLlamaSpit entity) {
        super(server, entity);
    }

    @Override
    public EntityLlamaSpit getHandle() {
        return (EntityLlamaSpit) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftLlamaSpit";
    }

    @Override
    public ProjectileSource getShooter() {
        return (getHandle().getOwner() != null) ? (ProjectileSource) getHandle().getOwner().getBukkitEntity() : null;
    }

    @Override
    public void setShooter(ProjectileSource source) {
        getHandle().setOwner((source != null) ? ((CraftLivingEntity) source).getHandle() : null);
    }
}
