package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.item.Items;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

public class CraftAbstractArrow extends CraftProjectile implements AbstractArrow {

    public CraftAbstractArrow(CraftServer server, EntityArrow entity) {
        super(server, entity);
    }

    @Override
    public void setKnockbackStrength(int knockbackStrength) {
    }

    @Override
    public int getKnockbackStrength() {
        return 0;
    }

    @Override
    public double getDamage() {
        return getHandle().baseDamage;
    }

    @Override
    public void setDamage(double damage) {
        Preconditions.checkArgument(damage >= 0, "Damage value (%s) must be positive", damage);
        getHandle().setBaseDamage(damage);
    }

    @Override
    public int getPierceLevel() {
        return getHandle().getPierceLevel();
    }

    @Override
    public void setPierceLevel(int pierceLevel) {
        Preconditions.checkArgument(0 <= pierceLevel && pierceLevel <= Byte.MAX_VALUE, "Pierce level (%s) out of range, expected 0 < level < 127", pierceLevel);

        getHandle().setPierceLevel((byte) pierceLevel);
    }

    @Override
    public boolean isCritical() {
        return getHandle().isCritArrow();
    }

    @Override
    public void setCritical(boolean critical) {
        getHandle().setCritArrow(critical);
    }

    @Override
    public ProjectileSource getShooter() {
        return getHandle().projectileSource;
    }

    @Override
    public void setShooter(ProjectileSource shooter) {
        if (shooter instanceof Entity) {
            getHandle().setOwner(((CraftEntity) shooter).getHandle());
        } else {
            getHandle().setOwner(null);
        }
        getHandle().projectileSource = shooter;
    }

    @Override
    public boolean isInBlock() {
        return getHandle().isInGround();
    }

    @Override
    public Block getAttachedBlock() {
        if (!isInBlock()) {
            return null;
        }

        BlockPosition pos = getHandle().blockPosition();
        return getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public PickupStatus getPickupStatus() {
        return PickupStatus.values()[getHandle().pickup.ordinal()];
    }

    @Override
    public void setPickupStatus(PickupStatus status) {
        Preconditions.checkArgument(status != null, "PickupStatus cannot be null");
        getHandle().pickup = EntityArrow.PickupStatus.byOrdinal(status.ordinal());
    }

    @Override
    public void setTicksLived(int value) {
        super.setTicksLived(value);

        // Second field for EntityArrow
        getHandle().life = value;
    }

    @Override
    public boolean isShotFromCrossbow() {
        net.minecraft.world.item.ItemStack firedFromWeapon = getHandle().getWeaponItem();

        return firedFromWeapon != null && firedFromWeapon.is(Items.CROSSBOW);
    }

    @Override
    public void setShotFromCrossbow(boolean shotFromCrossbow) {
    }

    @Override
    public ItemStack getItem() {
        return CraftItemStack.asBukkitCopy(getHandle().pickupItemStack);
    }

    @Override
    public void setItem(ItemStack item) {
        Preconditions.checkArgument(item != null, "ItemStack cannot be null");

        getHandle().pickupItemStack = CraftItemStack.asNMSCopy(item);
    }

    @Override
    public ItemStack getWeapon() {
        return CraftItemStack.asBukkitCopy(getHandle().getWeaponItem());
    }

    @Override
    public void setWeapon(ItemStack item) {
        Preconditions.checkArgument(item != null, "ItemStack cannot be null");

        getHandle().firedFromWeapon = CraftItemStack.asNMSCopy(item);
    }

    @Override
    public EntityArrow getHandle() {
        return (EntityArrow) entity;
    }

    @Override
    public String toString() {
        return "CraftArrow";
    }
}
