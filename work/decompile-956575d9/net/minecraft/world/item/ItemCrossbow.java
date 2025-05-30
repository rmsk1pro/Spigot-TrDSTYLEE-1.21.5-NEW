package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.INamable;
import net.minecraft.util.MathHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityFireworks;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ItemCrossbow extends ItemProjectileWeapon {

    private static final float MAX_CHARGE_DURATION = 1.25F;
    public static final int DEFAULT_RANGE = 8;
    private boolean startSoundPlayed = false;
    private boolean midLoadSoundPlayed = false;
    private static final float START_SOUND_PERCENT = 0.2F;
    private static final float MID_SOUND_PERCENT = 0.5F;
    private static final float ARROW_POWER = 3.15F;
    private static final float FIREWORK_POWER = 1.6F;
    public static final float MOB_ARROW_POWER = 1.6F;
    private static final ItemCrossbow.b DEFAULT_SOUNDS = new ItemCrossbow.b(Optional.of(SoundEffects.CROSSBOW_LOADING_START), Optional.of(SoundEffects.CROSSBOW_LOADING_MIDDLE), Optional.of(SoundEffects.CROSSBOW_LOADING_END));

    public ItemCrossbow(Item.Info item_info) {
        super(item_info);
    }

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return ItemCrossbow.ARROW_OR_FIREWORK;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return ItemCrossbow.ARROW_ONLY;
    }

    @Override
    public EnumInteractionResult use(World world, EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.getItemInHand(enumhand);
        ChargedProjectiles chargedprojectiles = (ChargedProjectiles) itemstack.get(DataComponents.CHARGED_PROJECTILES);

        if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
            this.performShooting(world, entityhuman, enumhand, itemstack, getShootingPower(chargedprojectiles), 1.0F, (EntityLiving) null);
            return EnumInteractionResult.CONSUME;
        } else if (!entityhuman.getProjectile(itemstack).isEmpty()) {
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
            entityhuman.startUsingItem(enumhand);
            return EnumInteractionResult.CONSUME;
        } else {
            return EnumInteractionResult.FAIL;
        }
    }

    private static float getShootingPower(ChargedProjectiles chargedprojectiles) {
        return chargedprojectiles.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
    }

    @Override
    public boolean releaseUsing(ItemStack itemstack, World world, EntityLiving entityliving, int i) {
        int j = this.getUseDuration(itemstack, entityliving) - i;

        return getPowerForTime(j, itemstack, entityliving) >= 1.0F && isCharged(itemstack);
    }

    private static boolean tryLoadProjectiles(EntityLiving entityliving, ItemStack itemstack) {
        List<ItemStack> list = draw(itemstack, entityliving.getProjectile(itemstack), entityliving);

        if (!list.isEmpty()) {
            itemstack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(list));
            return true;
        } else {
            return false;
        }
    }

    public static boolean isCharged(ItemStack itemstack) {
        ChargedProjectiles chargedprojectiles = (ChargedProjectiles) itemstack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);

        return !chargedprojectiles.isEmpty();
    }

    @Override
    protected void shootProjectile(EntityLiving entityliving, IProjectile iprojectile, int i, float f, float f1, float f2, @Nullable EntityLiving entityliving1) {
        Vector3f vector3f;

        if (entityliving1 != null) {
            double d0 = entityliving1.getX() - entityliving.getX();
            double d1 = entityliving1.getZ() - entityliving.getZ();
            double d2 = Math.sqrt(d0 * d0 + d1 * d1);
            double d3 = entityliving1.getY(0.3333333333333333D) - iprojectile.getY() + d2 * (double) 0.2F;

            vector3f = getProjectileShotVector(entityliving, new Vec3D(d0, d3, d1), f2);
        } else {
            Vec3D vec3d = entityliving.getUpVector(1.0F);
            Quaternionf quaternionf = (new Quaternionf()).setAngleAxis((double) (f2 * ((float) Math.PI / 180F)), vec3d.x, vec3d.y, vec3d.z);
            Vec3D vec3d1 = entityliving.getViewVector(1.0F);

            vector3f = vec3d1.toVector3f().rotate(quaternionf);
        }

        iprojectile.shoot((double) vector3f.x(), (double) vector3f.y(), (double) vector3f.z(), f, f1);
        float f3 = getShotPitch(entityliving.getRandom(), i);

        entityliving.level().playSound((Entity) null, entityliving.getX(), entityliving.getY(), entityliving.getZ(), SoundEffects.CROSSBOW_SHOOT, entityliving.getSoundSource(), 1.0F, f3);
    }

    private static Vector3f getProjectileShotVector(EntityLiving entityliving, Vec3D vec3d, float f) {
        Vector3f vector3f = vec3d.toVector3f().normalize();
        Vector3f vector3f1 = (new Vector3f(vector3f)).cross(new Vector3f(0.0F, 1.0F, 0.0F));

        if ((double) vector3f1.lengthSquared() <= 1.0E-7D) {
            Vec3D vec3d1 = entityliving.getUpVector(1.0F);

            vector3f1 = (new Vector3f(vector3f)).cross(vec3d1.toVector3f());
        }

        Vector3f vector3f2 = (new Vector3f(vector3f)).rotateAxis(((float) Math.PI / 2F), vector3f1.x, vector3f1.y, vector3f1.z);

        return (new Vector3f(vector3f)).rotateAxis(f * ((float) Math.PI / 180F), vector3f2.x, vector3f2.y, vector3f2.z);
    }

    @Override
    protected IProjectile createProjectile(World world, EntityLiving entityliving, ItemStack itemstack, ItemStack itemstack1, boolean flag) {
        if (itemstack1.is(Items.FIREWORK_ROCKET)) {
            return new EntityFireworks(world, itemstack1, entityliving, entityliving.getX(), entityliving.getEyeY() - (double) 0.15F, entityliving.getZ(), true);
        } else {
            IProjectile iprojectile = super.createProjectile(world, entityliving, itemstack, itemstack1, flag);

            if (iprojectile instanceof EntityArrow) {
                EntityArrow entityarrow = (EntityArrow) iprojectile;

                entityarrow.setSoundEvent(SoundEffects.CROSSBOW_HIT);
            }

            return iprojectile;
        }
    }

    @Override
    protected int getDurabilityUse(ItemStack itemstack) {
        return itemstack.is(Items.FIREWORK_ROCKET) ? 3 : 1;
    }

    public void performShooting(World world, EntityLiving entityliving, EnumHand enumhand, ItemStack itemstack, float f, float f1, @Nullable EntityLiving entityliving1) {
        if (world instanceof WorldServer worldserver) {
            ChargedProjectiles chargedprojectiles = (ChargedProjectiles) itemstack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);

            if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
                this.shoot(worldserver, entityliving, enumhand, itemstack, chargedprojectiles.getItems(), f, f1, entityliving instanceof EntityHuman, entityliving1);
                if (entityliving instanceof EntityPlayer) {
                    EntityPlayer entityplayer = (EntityPlayer) entityliving;

                    CriterionTriggers.SHOT_CROSSBOW.trigger(entityplayer, itemstack);
                    entityplayer.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                }

            }
        }
    }

    private static float getShotPitch(RandomSource randomsource, int i) {
        return i == 0 ? 1.0F : getRandomShotPitch((i & 1) == 1, randomsource);
    }

    private static float getRandomShotPitch(boolean flag, RandomSource randomsource) {
        float f = flag ? 0.63F : 0.43F;

        return 1.0F / (randomsource.nextFloat() * 0.5F + 1.8F) + f;
    }

    @Override
    public void onUseTick(World world, EntityLiving entityliving, ItemStack itemstack, int i) {
        if (!world.isClientSide) {
            ItemCrossbow.b itemcrossbow_b = this.getChargingSounds(itemstack);
            float f = (float) (itemstack.getUseDuration(entityliving) - i) / (float) getChargeDuration(itemstack, entityliving);

            if (f < 0.2F) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
            }

            if (f >= 0.2F && !this.startSoundPlayed) {
                this.startSoundPlayed = true;
                itemcrossbow_b.start().ifPresent((holder) -> {
                    world.playSound((Entity) null, entityliving.getX(), entityliving.getY(), entityliving.getZ(), (SoundEffect) holder.value(), SoundCategory.PLAYERS, 0.5F, 1.0F);
                });
            }

            if (f >= 0.5F && !this.midLoadSoundPlayed) {
                this.midLoadSoundPlayed = true;
                itemcrossbow_b.mid().ifPresent((holder) -> {
                    world.playSound((Entity) null, entityliving.getX(), entityliving.getY(), entityliving.getZ(), (SoundEffect) holder.value(), SoundCategory.PLAYERS, 0.5F, 1.0F);
                });
            }

            if (f >= 1.0F && !isCharged(itemstack) && tryLoadProjectiles(entityliving, itemstack)) {
                itemcrossbow_b.end().ifPresent((holder) -> {
                    world.playSound((Entity) null, entityliving.getX(), entityliving.getY(), entityliving.getZ(), (SoundEffect) holder.value(), entityliving.getSoundSource(), 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
                });
            }
        }

    }

    @Override
    public int getUseDuration(ItemStack itemstack, EntityLiving entityliving) {
        return 72000;
    }

    public static int getChargeDuration(ItemStack itemstack, EntityLiving entityliving) {
        float f = EnchantmentManager.modifyCrossbowChargingTime(itemstack, entityliving, 1.25F);

        return MathHelper.floor(f * 20.0F);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack itemstack) {
        return ItemUseAnimation.CROSSBOW;
    }

    ItemCrossbow.b getChargingSounds(ItemStack itemstack) {
        return (ItemCrossbow.b) EnchantmentManager.pickHighestLevel(itemstack, EnchantmentEffectComponents.CROSSBOW_CHARGING_SOUNDS).orElse(ItemCrossbow.DEFAULT_SOUNDS);
    }

    private static float getPowerForTime(int i, ItemStack itemstack, EntityLiving entityliving) {
        float f = (float) i / (float) getChargeDuration(itemstack, entityliving);

        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public boolean useOnRelease(ItemStack itemstack) {
        return itemstack.is(this);
    }

    @Override
    public int getDefaultProjectileRange() {
        return 8;
    }

    public static record b(Optional<Holder<SoundEffect>> start, Optional<Holder<SoundEffect>> mid, Optional<Holder<SoundEffect>> end) {

        public static final Codec<ItemCrossbow.b> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(SoundEffect.CODEC.optionalFieldOf("start").forGetter(ItemCrossbow.b::start), SoundEffect.CODEC.optionalFieldOf("mid").forGetter(ItemCrossbow.b::mid), SoundEffect.CODEC.optionalFieldOf("end").forGetter(ItemCrossbow.b::end)).apply(instance, ItemCrossbow.b::new);
        });
    }

    public static enum a implements INamable {

        NONE("none"), ARROW("arrow"), ROCKET("rocket");

        public static final Codec<ItemCrossbow.a> CODEC = INamable.<ItemCrossbow.a>fromEnum(ItemCrossbow.a::values);
        private final String name;

        private a(final String s) {
            this.name = s;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
