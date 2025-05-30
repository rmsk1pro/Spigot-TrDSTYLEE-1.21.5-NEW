package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorSwim;
import net.minecraft.world.entity.ai.behavior.LongJumpUtil;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.Vec3D;

public class LongJump extends Behavior<Breeze> {

    private static final int REQUIRED_AIR_BLOCKS_ABOVE = 4;
    private static final int JUMP_COOLDOWN_TICKS = 10;
    private static final int JUMP_COOLDOWN_WHEN_HURT_TICKS = 2;
    private static final int INHALING_DURATION_TICKS = Math.round(10.0F);
    private static final float DEFAULT_FOLLOW_RANGE = 24.0F;
    private static final float DEFAULT_MAX_JUMP_VELOCITY = 1.4F;
    private static final float MAX_JUMP_VELOCITY_MULTIPLIER = 0.058333334F;
    private static final ObjectArrayList<Integer> ALLOWED_ANGLES = new ObjectArrayList(Lists.newArrayList(new Integer[]{40, 55, 60, 75, 80}));

    @VisibleForTesting
    public LongJump() {
        super(Map.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.BREEZE_JUMP_COOLDOWN, MemoryStatus.VALUE_ABSENT, MemoryModuleType.BREEZE_JUMP_INHALING, MemoryStatus.REGISTERED, MemoryModuleType.BREEZE_JUMP_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.BREEZE_SHOOT, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.REGISTERED), 200);
    }

    public static boolean canRun(WorldServer worldserver, Breeze breeze) {
        if (!breeze.onGround() && !breeze.isInWater()) {
            return false;
        } else if (BehaviorSwim.shouldSwim(breeze)) {
            return false;
        } else if (breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_TARGET, MemoryStatus.VALUE_PRESENT)) {
            return true;
        } else {
            EntityLiving entityliving = (EntityLiving) breeze.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse((Object) null);

            if (entityliving == null) {
                return false;
            } else if (outOfAggroRange(breeze, entityliving)) {
                breeze.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                return false;
            } else if (tooCloseForJump(breeze, entityliving)) {
                return false;
            } else if (!canJumpFromCurrentPosition(worldserver, breeze)) {
                return false;
            } else {
                BlockPosition blockposition = snapToSurface(breeze, BreezeUtil.randomPointBehindTarget(entityliving, breeze.getRandom()));

                if (blockposition == null) {
                    return false;
                } else {
                    IBlockData iblockdata = worldserver.getBlockState(blockposition.below());

                    if (breeze.getType().isBlockDangerous(iblockdata)) {
                        return false;
                    } else if (!BreezeUtil.hasLineOfSight(breeze, blockposition.getCenter()) && !BreezeUtil.hasLineOfSight(breeze, blockposition.above(4).getCenter())) {
                        return false;
                    } else {
                        breeze.getBrain().setMemory(MemoryModuleType.BREEZE_JUMP_TARGET, blockposition);
                        return true;
                    }
                }
            }
        }
    }

    protected boolean checkExtraStartConditions(WorldServer worldserver, Breeze breeze) {
        return canRun(worldserver, breeze);
    }

    protected boolean canStillUse(WorldServer worldserver, Breeze breeze, long i) {
        return breeze.getPose() != EntityPose.STANDING && !breeze.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_JUMP_COOLDOWN);
    }

    protected void start(WorldServer worldserver, Breeze breeze, long i) {
        if (breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_INHALING, MemoryStatus.VALUE_ABSENT)) {
            breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_INHALING, Unit.INSTANCE, (long) LongJump.INHALING_DURATION_TICKS);
        }

        breeze.setPose(EntityPose.INHALING);
        worldserver.playSound((Entity) null, (Entity) breeze, SoundEffects.BREEZE_CHARGE, SoundCategory.HOSTILE, 1.0F, 1.0F);
        breeze.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_TARGET).ifPresent((blockposition) -> {
            breeze.lookAt(ArgumentAnchor.Anchor.EYES, blockposition.getCenter());
        });
    }

    protected void tick(WorldServer worldserver, Breeze breeze, long i) {
        boolean flag = breeze.isInWater();

        if (!flag && breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.VALUE_PRESENT)) {
            breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_LEAVING_WATER);
        }

        if (isFinishedInhaling(breeze)) {
            Vec3D vec3d = (Vec3D) breeze.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_TARGET).flatMap((blockposition) -> {
                return calculateOptimalJumpVector(breeze, breeze.getRandom(), Vec3D.atBottomCenterOf(blockposition));
            }).orElse((Object) null);

            if (vec3d == null) {
                breeze.setPose(EntityPose.STANDING);
                return;
            }

            if (flag) {
                breeze.getBrain().setMemory(MemoryModuleType.BREEZE_LEAVING_WATER, Unit.INSTANCE);
            }

            breeze.playSound(SoundEffects.BREEZE_JUMP, 1.0F, 1.0F);
            breeze.setPose(EntityPose.LONG_JUMPING);
            breeze.setYRot(breeze.yBodyRot);
            breeze.setDiscardFriction(true);
            breeze.setDeltaMovement(vec3d);
        } else if (isFinishedJumping(breeze)) {
            breeze.playSound(SoundEffects.BREEZE_LAND, 1.0F, 1.0F);
            breeze.setPose(EntityPose.STANDING);
            breeze.setDiscardFriction(false);
            boolean flag1 = breeze.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);

            breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_COOLDOWN, Unit.INSTANCE, flag1 ? 2L : 10L);
            breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, 100L);
        }

    }

    protected void stop(WorldServer worldserver, Breeze breeze, long i) {
        if (breeze.getPose() == EntityPose.LONG_JUMPING || breeze.getPose() == EntityPose.INHALING) {
            breeze.setPose(EntityPose.STANDING);
        }

        breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_TARGET);
        breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_INHALING);
        breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_LEAVING_WATER);
    }

    private static boolean isFinishedInhaling(Breeze breeze) {
        return breeze.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_INHALING).isEmpty() && breeze.getPose() == EntityPose.INHALING;
    }

    private static boolean isFinishedJumping(Breeze breeze) {
        boolean flag = breeze.getPose() == EntityPose.LONG_JUMPING;
        boolean flag1 = breeze.onGround();
        boolean flag2 = breeze.isInWater() && breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.VALUE_ABSENT);

        return flag && (flag1 || flag2);
    }

    @Nullable
    private static BlockPosition snapToSurface(EntityLiving entityliving, Vec3D vec3d) {
        RayTrace raytrace = new RayTrace(vec3d, vec3d.relative(EnumDirection.DOWN, 10.0D), RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, entityliving);
        MovingObjectPosition movingobjectposition = entityliving.level().clip(raytrace);

        if (movingobjectposition.getType() == MovingObjectPosition.EnumMovingObjectType.BLOCK) {
            return BlockPosition.containing(movingobjectposition.getLocation()).above();
        } else {
            RayTrace raytrace1 = new RayTrace(vec3d, vec3d.relative(EnumDirection.UP, 10.0D), RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, entityliving);
            MovingObjectPosition movingobjectposition1 = entityliving.level().clip(raytrace1);

            return movingobjectposition1.getType() == MovingObjectPosition.EnumMovingObjectType.BLOCK ? BlockPosition.containing(movingobjectposition1.getLocation()).above() : null;
        }
    }

    private static boolean outOfAggroRange(Breeze breeze, EntityLiving entityliving) {
        return !entityliving.closerThan(breeze, breeze.getAttributeValue(GenericAttributes.FOLLOW_RANGE));
    }

    private static boolean tooCloseForJump(Breeze breeze, EntityLiving entityliving) {
        return entityliving.distanceTo(breeze) - 4.0F <= 0.0F;
    }

    private static boolean canJumpFromCurrentPosition(WorldServer worldserver, Breeze breeze) {
        BlockPosition blockposition = breeze.blockPosition();

        if (worldserver.getBlockState(blockposition).is(Blocks.HONEY_BLOCK)) {
            return false;
        } else {
            for (int i = 1; i <= 4; ++i) {
                BlockPosition blockposition1 = blockposition.relative(EnumDirection.UP, i);

                if (!worldserver.getBlockState(blockposition1).isAir() && !worldserver.getFluidState(blockposition1).is(TagsFluid.WATER)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static Optional<Vec3D> calculateOptimalJumpVector(Breeze breeze, RandomSource randomsource, Vec3D vec3d) {
        for (int i : SystemUtils.shuffledCopy(LongJump.ALLOWED_ANGLES, randomsource)) {
            float f = 0.058333334F * (float) breeze.getAttributeValue(GenericAttributes.FOLLOW_RANGE);
            Optional<Vec3D> optional = LongJumpUtil.calculateJumpVectorForAngle(breeze, vec3d, f, i, false);

            if (optional.isPresent()) {
                if (breeze.hasEffect(MobEffects.JUMP_BOOST)) {
                    double d0 = ((Vec3D) optional.get()).normalize().y * (double) breeze.getJumpBoostPower();

                    return optional.map((vec3d1) -> {
                        return vec3d1.add(0.0D, d0, 0.0D);
                    });
                }

                return optional;
            }
        }

        return Optional.empty();
    }
}
