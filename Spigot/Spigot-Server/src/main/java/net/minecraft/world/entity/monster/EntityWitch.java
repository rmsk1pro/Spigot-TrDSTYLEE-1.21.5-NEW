package net.minecraft.world.entity.monster;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.Particles;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalArrowAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTargetWitch;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestHealableRaider;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.entity.projectile.ThrownSplashPotion;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3D;

public class EntityWitch extends EntityRaider implements IRangedEntity {

    private static final MinecraftKey SPEED_MODIFIER_DRINKING_ID = MinecraftKey.withDefaultNamespace("drinking");
    private static final AttributeModifier SPEED_MODIFIER_DRINKING = new AttributeModifier(EntityWitch.SPEED_MODIFIER_DRINKING_ID, -0.25D, AttributeModifier.Operation.ADD_VALUE);
    private static final DataWatcherObject<Boolean> DATA_USING_ITEM = DataWatcher.<Boolean>defineId(EntityWitch.class, DataWatcherRegistry.BOOLEAN);
    private int usingTime;
    private PathfinderGoalNearestHealableRaider<EntityRaider> healRaidersGoal;
    private PathfinderGoalNearestAttackableTargetWitch<EntityHuman> attackPlayersGoal;

    public EntityWitch(EntityTypes<? extends EntityWitch> entitytypes, World world) {
        super(entitytypes, world);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.healRaidersGoal = new PathfinderGoalNearestHealableRaider<EntityRaider>(this, EntityRaider.class, true, (entityliving, worldserver) -> {
            return this.hasActiveRaid() && entityliving.getType() != EntityTypes.WITCH;
        });
        this.attackPlayersGoal = new PathfinderGoalNearestAttackableTargetWitch<EntityHuman>(this, EntityHuman.class, 10, true, false, (PathfinderTargetCondition.a) null);
        this.goalSelector.addGoal(1, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(2, new PathfinderGoalArrowAttack(this, 1.0D, 60, 10.0F));
        this.goalSelector.addGoal(2, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.goalSelector.addGoal(3, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(3, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, new PathfinderGoalHurtByTarget(this, new Class[]{EntityRaider.class}));
        this.targetSelector.addGoal(2, this.healRaidersGoal);
        this.targetSelector.addGoal(3, this.attackPlayersGoal);
    }

    @Override
    protected void defineSynchedData(DataWatcher.a datawatcher_a) {
        super.defineSynchedData(datawatcher_a);
        datawatcher_a.define(EntityWitch.DATA_USING_ITEM, false);
    }

    @Override
    protected SoundEffect getAmbientSound() {
        return SoundEffects.WITCH_AMBIENT;
    }

    @Override
    protected SoundEffect getHurtSound(DamageSource damagesource) {
        return SoundEffects.WITCH_HURT;
    }

    @Override
    protected SoundEffect getDeathSound() {
        return SoundEffects.WITCH_DEATH;
    }

    public void setUsingItem(boolean flag) {
        this.getEntityData().set(EntityWitch.DATA_USING_ITEM, flag);
    }

    public boolean isDrinkingPotion() {
        return (Boolean) this.getEntityData().get(EntityWitch.DATA_USING_ITEM);
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 26.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide && this.isAlive()) {
            this.healRaidersGoal.decrementCooldown();
            if (this.healRaidersGoal.getCooldown() <= 0) {
                this.attackPlayersGoal.setCanAttack(true);
            } else {
                this.attackPlayersGoal.setCanAttack(false);
            }

            if (this.isDrinkingPotion()) {
                if (this.usingTime-- <= 0) {
                    this.setUsingItem(false);
                    ItemStack itemstack = this.getMainHandItem();

                    this.setItemSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
                    PotionContents potioncontents = (PotionContents) itemstack.get(DataComponents.POTION_CONTENTS);

                    if (itemstack.is(Items.POTION) && potioncontents != null) {
                        potioncontents.forEachEffect((effect) -> this.addEffect(effect, org.bukkit.event.entity.EntityPotionEffectEvent.Cause.ATTACK), (Float) itemstack.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F)); // CraftBukkit
                    }

                    this.gameEvent(GameEvent.DRINK);
                    this.getAttribute(GenericAttributes.MOVEMENT_SPEED).removeModifier(EntityWitch.SPEED_MODIFIER_DRINKING.id());
                }
            } else {
                Holder<PotionRegistry> holder = null;

                if (this.random.nextFloat() < 0.15F && this.isEyeInFluid(TagsFluid.WATER) && !this.hasEffect(MobEffects.WATER_BREATHING)) {
                    holder = Potions.WATER_BREATHING;
                } else if (this.random.nextFloat() < 0.15F && (this.isOnFire() || this.getLastDamageSource() != null && this.getLastDamageSource().is(DamageTypeTags.IS_FIRE)) && !this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                    holder = Potions.FIRE_RESISTANCE;
                } else if (this.random.nextFloat() < 0.05F && this.getHealth() < this.getMaxHealth()) {
                    holder = Potions.HEALING;
                } else if (this.random.nextFloat() < 0.5F && this.getTarget() != null && !this.hasEffect(MobEffects.SPEED) && this.getTarget().distanceToSqr((Entity) this) > 121.0D) {
                    holder = Potions.SWIFTNESS;
                }

                if (holder != null) {
                    this.setItemSlot(EnumItemSlot.MAINHAND, PotionContents.createItemStack(Items.POTION, holder));
                    this.usingTime = this.getMainHandItem().getUseDuration(this);
                    this.setUsingItem(true);
                    if (!this.isSilent()) {
                        this.level().playSound((Entity) null, this.getX(), this.getY(), this.getZ(), SoundEffects.WITCH_DRINK, this.getSoundSource(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
                    }

                    AttributeModifiable attributemodifiable = this.getAttribute(GenericAttributes.MOVEMENT_SPEED);

                    attributemodifiable.removeModifier(EntityWitch.SPEED_MODIFIER_DRINKING_ID);
                    attributemodifiable.addTransientModifier(EntityWitch.SPEED_MODIFIER_DRINKING);
                }
            }

            if (this.random.nextFloat() < 7.5E-4F) {
                this.level().broadcastEntityEvent(this, (byte) 15);
            }
        }

        super.aiStep();
    }

    @Override
    public SoundEffect getCelebrateSound() {
        return SoundEffects.WITCH_CELEBRATE;
    }

    @Override
    public void handleEntityEvent(byte b0) {
        if (b0 == 15) {
            for (int i = 0; i < this.random.nextInt(35) + 10; ++i) {
                this.level().addParticle(Particles.WITCH, this.getX() + this.random.nextGaussian() * (double) 0.13F, this.getBoundingBox().maxY + 0.5D + this.random.nextGaussian() * (double) 0.13F, this.getZ() + this.random.nextGaussian() * (double) 0.13F, 0.0D, 0.0D, 0.0D);
            }
        } else {
            super.handleEntityEvent(b0);
        }

    }

    @Override
    protected float getDamageAfterMagicAbsorb(DamageSource damagesource, float f) {
        f = super.getDamageAfterMagicAbsorb(damagesource, f);
        if (damagesource.getEntity() == this) {
            f = 0.0F;
        }

        if (damagesource.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            f *= 0.15F;
        }

        return f;
    }

    @Override
    public void performRangedAttack(EntityLiving entityliving, float f) {
        if (!this.isDrinkingPotion()) {
            Vec3D vec3d = entityliving.getDeltaMovement();
            double d0 = entityliving.getX() + vec3d.x - this.getX();
            double d1 = entityliving.getEyeY() - (double) 1.1F - this.getY();
            double d2 = entityliving.getZ() + vec3d.z - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            Holder<PotionRegistry> holder = Potions.HARMING;

            if (entityliving instanceof EntityRaider) {
                if (entityliving.getHealth() <= 4.0F) {
                    holder = Potions.HEALING;
                } else {
                    holder = Potions.REGENERATION;
                }

                this.setTarget((EntityLiving) null);
            } else if (d3 >= 8.0D && !entityliving.hasEffect(MobEffects.SLOWNESS)) {
                holder = Potions.SLOWNESS;
            } else if (entityliving.getHealth() >= 8.0F && !entityliving.hasEffect(MobEffects.POISON)) {
                holder = Potions.POISON;
            } else if (d3 <= 3.0D && !entityliving.hasEffect(MobEffects.WEAKNESS) && this.random.nextFloat() < 0.25F) {
                holder = Potions.WEAKNESS;
            }

            World world = this.level();

            if (world instanceof WorldServer) {
                WorldServer worldserver = (WorldServer) world;
                ItemStack itemstack = PotionContents.createItemStack(Items.SPLASH_POTION, holder);

                IProjectile.spawnProjectileUsingShoot(ThrownSplashPotion::new, worldserver, itemstack, this, d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
            }

            if (!this.isSilent()) {
                this.level().playSound((Entity) null, this.getX(), this.getY(), this.getZ(), SoundEffects.WITCH_THROW, this.getSoundSource(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
            }

        }
    }

    @Override
    public void applyRaidBuffs(WorldServer worldserver, int i, boolean flag) {}

    @Override
    public boolean canBeLeader() {
        return false;
    }
}
