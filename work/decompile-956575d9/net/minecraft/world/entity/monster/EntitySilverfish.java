package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockMonsterEggs;
import net.minecraft.world.level.block.state.IBlockData;

public class EntitySilverfish extends EntityMonster {

    @Nullable
    private EntitySilverfish.PathfinderGoalSilverfishWakeOthers friendsGoal;

    public EntitySilverfish(EntityTypes<? extends EntitySilverfish> entitytypes, World world) {
        super(entitytypes, world);
    }

    @Override
    protected void registerGoals() {
        this.friendsGoal = new EntitySilverfish.PathfinderGoalSilverfishWakeOthers(this);
        this.goalSelector.addGoal(1, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
        this.goalSelector.addGoal(3, this.friendsGoal);
        this.goalSelector.addGoal(4, new PathfinderGoalMeleeAttack(this, 1.0D, false));
        this.goalSelector.addGoal(5, new EntitySilverfish.PathfinderGoalSilverfishHideInBlock(this));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this, new Class[0])).setAlertOthers());
        this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget(this, EntityHuman.class, true));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 8.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.25D).add(GenericAttributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected SoundEffect getAmbientSound() {
        return SoundEffects.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEffect getHurtSound(DamageSource damagesource) {
        return SoundEffects.SILVERFISH_HURT;
    }

    @Override
    protected SoundEffect getDeathSound() {
        return SoundEffects.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition blockposition, IBlockData iblockdata) {
        this.playSound(SoundEffects.SILVERFISH_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean hurtServer(WorldServer worldserver, DamageSource damagesource, float f) {
        if (this.isInvulnerableTo(worldserver, damagesource)) {
            return false;
        } else {
            if ((damagesource.getEntity() != null || damagesource.is(DamageTypeTags.ALWAYS_TRIGGERS_SILVERFISH)) && this.friendsGoal != null) {
                this.friendsGoal.notifyHurt();
            }

            return super.hurtServer(worldserver, damagesource, f);
        }
    }

    @Override
    public void tick() {
        this.yBodyRot = this.getYRot();
        super.tick();
    }

    @Override
    public void setYBodyRot(float f) {
        this.setYRot(f);
        super.setYBodyRot(f);
    }

    @Override
    public float getWalkTargetValue(BlockPosition blockposition, IWorldReader iworldreader) {
        return BlockMonsterEggs.isCompatibleHostBlock(iworldreader.getBlockState(blockposition.below())) ? 10.0F : super.getWalkTargetValue(blockposition, iworldreader);
    }

    public static boolean checkSilverfishSpawnRules(EntityTypes<EntitySilverfish> entitytypes, GeneratorAccess generatoraccess, EntitySpawnReason entityspawnreason, BlockPosition blockposition, RandomSource randomsource) {
        if (!checkAnyLightMonsterSpawnRules(entitytypes, generatoraccess, entityspawnreason, blockposition, randomsource)) {
            return false;
        } else if (EntitySpawnReason.isSpawner(entityspawnreason)) {
            return true;
        } else {
            EntityHuman entityhuman = generatoraccess.getNearestPlayer((double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 0.5D, (double) blockposition.getZ() + 0.5D, 5.0D, true);

            return entityhuman == null;
        }
    }

    private static class PathfinderGoalSilverfishWakeOthers extends PathfinderGoal {

        private final EntitySilverfish silverfish;
        private int lookForFriends;

        public PathfinderGoalSilverfishWakeOthers(EntitySilverfish entitysilverfish) {
            this.silverfish = entitysilverfish;
        }

        public void notifyHurt() {
            if (this.lookForFriends == 0) {
                this.lookForFriends = this.adjustedTickDelay(20);
            }

        }

        @Override
        public boolean canUse() {
            return this.lookForFriends > 0;
        }

        @Override
        public void tick() {
            --this.lookForFriends;
            if (this.lookForFriends <= 0) {
                World world = this.silverfish.level();
                RandomSource randomsource = this.silverfish.getRandom();
                BlockPosition blockposition = this.silverfish.blockPosition();

                for (int i = 0; i <= 5 && i >= -5; i = (i <= 0 ? 1 : 0) - i) {
                    for (int j = 0; j <= 10 && j >= -10; j = (j <= 0 ? 1 : 0) - j) {
                        for (int k = 0; k <= 10 && k >= -10; k = (k <= 0 ? 1 : 0) - k) {
                            BlockPosition blockposition1 = blockposition.offset(j, i, k);
                            IBlockData iblockdata = world.getBlockState(blockposition1);
                            Block block = iblockdata.getBlock();

                            if (block instanceof BlockMonsterEggs) {
                                if (getServerLevel(world).getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                                    world.destroyBlock(blockposition1, true, this.silverfish);
                                } else {
                                    world.setBlock(blockposition1, ((BlockMonsterEggs) block).hostStateByInfested(world.getBlockState(blockposition1)), 3);
                                }

                                if (randomsource.nextBoolean()) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private static class PathfinderGoalSilverfishHideInBlock extends PathfinderGoalRandomStroll {

        @Nullable
        private EnumDirection selectedDirection;
        private boolean doMerge;

        public PathfinderGoalSilverfishHideInBlock(EntitySilverfish entitysilverfish) {
            super(entitysilverfish, 1.0D, 10);
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.mob.getTarget() != null) {
                return false;
            } else if (!this.mob.getNavigation().isDone()) {
                return false;
            } else {
                RandomSource randomsource = this.mob.getRandom();

                if (getServerLevel((Entity) this.mob).getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && randomsource.nextInt(reducedTickDelay(10)) == 0) {
                    this.selectedDirection = EnumDirection.getRandom(randomsource);
                    BlockPosition blockposition = BlockPosition.containing(this.mob.getX(), this.mob.getY() + 0.5D, this.mob.getZ()).relative(this.selectedDirection);
                    IBlockData iblockdata = this.mob.level().getBlockState(blockposition);

                    if (BlockMonsterEggs.isCompatibleHostBlock(iblockdata)) {
                        this.doMerge = true;
                        return true;
                    }
                }

                this.doMerge = false;
                return super.canUse();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.doMerge ? false : super.canContinueToUse();
        }

        @Override
        public void start() {
            if (!this.doMerge) {
                super.start();
            } else {
                GeneratorAccess generatoraccess = this.mob.level();
                BlockPosition blockposition = BlockPosition.containing(this.mob.getX(), this.mob.getY() + 0.5D, this.mob.getZ()).relative(this.selectedDirection);
                IBlockData iblockdata = generatoraccess.getBlockState(blockposition);

                if (BlockMonsterEggs.isCompatibleHostBlock(iblockdata)) {
                    generatoraccess.setBlock(blockposition, BlockMonsterEggs.infestedStateByHost(iblockdata), 3);
                    this.mob.spawnAnim();
                    this.mob.discard();
                }

            }
        }
    }
}
