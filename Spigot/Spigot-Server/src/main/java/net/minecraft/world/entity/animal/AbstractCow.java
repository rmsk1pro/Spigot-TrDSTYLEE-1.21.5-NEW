package net.minecraft.world.entity.animal;

import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemLiquidUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;

// CraftBukkit start
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerBucketFillEvent;
// CraftBukkit end

public abstract class AbstractCow extends EntityAnimal {

    private static final EntitySize BABY_DIMENSIONS = EntityTypes.COW.getDimensions().scale(0.5F).withEyeHeight(0.665F);

    public AbstractCow(EntityTypes<? extends AbstractCow> entitytypes, World world) {
        super(entitytypes, world);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new PathfinderGoalPanic(this, 2.0D));
        this.goalSelector.addGoal(2, new PathfinderGoalBreed(this, 1.0D));
        this.goalSelector.addGoal(3, new PathfinderGoalTempt(this, 1.25D, (itemstack) -> {
            return itemstack.is(TagsItem.COW_FOOD);
        }, false));
        this.goalSelector.addGoal(4, new PathfinderGoalFollowParent(this, 1.25D));
        this.goalSelector.addGoal(5, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.goalSelector.addGoal(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.addGoal(7, new PathfinderGoalRandomLookaround(this));
    }

    @Override
    public boolean isFood(ItemStack itemstack) {
        return itemstack.is(TagsItem.COW_FOOD);
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityAnimal.createAnimalAttributes().add(GenericAttributes.MAX_HEALTH, 10.0D).add(GenericAttributes.MOVEMENT_SPEED, (double) 0.2F);
    }

    @Override
    protected SoundEffect getAmbientSound() {
        return SoundEffects.COW_AMBIENT;
    }

    @Override
    protected SoundEffect getHurtSound(DamageSource damagesource) {
        return SoundEffects.COW_HURT;
    }

    @Override
    protected SoundEffect getDeathSound() {
        return SoundEffects.COW_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition blockposition, IBlockData iblockdata) {
        this.playSound(SoundEffects.COW_STEP, 0.15F, 1.0F);
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.getItemInHand(enumhand);

        if (itemstack.is(Items.BUCKET) && !this.isBaby()) {
            // CraftBukkit start - Got milk?
            PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent(entityhuman.level(), entityhuman, this.blockPosition(), this.blockPosition(), null, itemstack, Items.MILK_BUCKET, enumhand);

            if (event.isCancelled()) {
                return EnumInteractionResult.PASS;
            }
            // CraftBukkit end

            entityhuman.playSound(SoundEffects.COW_MILK, 1.0F, 1.0F);
            ItemStack itemstack1 = ItemLiquidUtil.createFilledResult(itemstack, entityhuman, CraftItemStack.asNMSCopy(event.getItemStack())); // CraftBukkit

            entityhuman.setItemInHand(enumhand, itemstack1);
            return EnumInteractionResult.SUCCESS;
        } else {
            return super.mobInteract(entityhuman, enumhand);
        }
    }

    @Override
    public EntitySize getDefaultDimensions(EntityPose entitypose) {
        return this.isBaby() ? AbstractCow.BABY_DIMENSIONS : super.getDefaultDimensions(entitypose);
    }
}
