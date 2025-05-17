package net.minecraft.world.entity.projectile;

import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityFireballFireball extends EntityFireball implements ItemSupplier {

    private static final float MIN_CAMERA_DISTANCE_SQUARED = 12.25F;
    private static final DataWatcherObject<ItemStack> DATA_ITEM_STACK = DataWatcher.<ItemStack>defineId(EntityFireballFireball.class, DataWatcherRegistry.ITEM_STACK);

    public EntityFireballFireball(EntityTypes<? extends EntityFireballFireball> entitytypes, World world) {
        super(entitytypes, world);
    }

    public EntityFireballFireball(EntityTypes<? extends EntityFireballFireball> entitytypes, double d0, double d1, double d2, Vec3D vec3d, World world) {
        super(entitytypes, d0, d1, d2, vec3d, world);
    }

    public EntityFireballFireball(EntityTypes<? extends EntityFireballFireball> entitytypes, EntityLiving entityliving, Vec3D vec3d, World world) {
        super(entitytypes, entityliving, vec3d, world);
    }

    public void setItem(ItemStack itemstack) {
        if (itemstack.isEmpty()) {
            this.getEntityData().set(EntityFireballFireball.DATA_ITEM_STACK, this.getDefaultItem());
        } else {
            this.getEntityData().set(EntityFireballFireball.DATA_ITEM_STACK, itemstack.copyWithCount(1));
        }

    }

    @Override
    protected void playEntityOnFireExtinguishedSound() {}

    @Override
    public ItemStack getItem() {
        return (ItemStack) this.getEntityData().get(EntityFireballFireball.DATA_ITEM_STACK);
    }

    @Override
    protected void defineSynchedData(DataWatcher.a datawatcher_a) {
        datawatcher_a.define(EntityFireballFireball.DATA_ITEM_STACK, this.getDefaultItem());
    }

    @Override
    public void addAdditionalSaveData(NBTTagCompound nbttagcompound) {
        super.addAdditionalSaveData(nbttagcompound);
        RegistryOps<NBTBase> registryops = this.registryAccess().<NBTBase>createSerializationContext(DynamicOpsNBT.INSTANCE);

        nbttagcompound.store("Item", ItemStack.CODEC, registryops, this.getItem());
    }

    @Override
    public void readAdditionalSaveData(NBTTagCompound nbttagcompound) {
        super.readAdditionalSaveData(nbttagcompound);
        RegistryOps<NBTBase> registryops = this.registryAccess().<NBTBase>createSerializationContext(DynamicOpsNBT.INSTANCE);

        this.setItem((ItemStack) nbttagcompound.read("Item", ItemStack.CODEC, registryops).orElse(this.getDefaultItem()));
    }

    private ItemStack getDefaultItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }

    @Override
    public SlotAccess getSlot(int i) {
        return i == 0 ? SlotAccess.of(this::getItem, this::setItem) : super.getSlot(i);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d0) {
        return this.tickCount < 2 && d0 < 12.25D ? false : super.shouldRenderAtSqrDistance(d0);
    }
}
