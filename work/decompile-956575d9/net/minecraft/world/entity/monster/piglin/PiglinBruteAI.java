package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.behavior.BehaviorAttack;
import net.minecraft.world.entity.ai.behavior.BehaviorAttackTargetForget;
import net.minecraft.world.entity.ai.behavior.BehaviorAttackTargetSet;
import net.minecraft.world.entity.ai.behavior.BehaviorForgetAnger;
import net.minecraft.world.entity.ai.behavior.BehaviorGateSingle;
import net.minecraft.world.entity.ai.behavior.BehaviorInteract;
import net.minecraft.world.entity.ai.behavior.BehaviorInteractDoor;
import net.minecraft.world.entity.ai.behavior.BehaviorLook;
import net.minecraft.world.entity.ai.behavior.BehaviorLookInteract;
import net.minecraft.world.entity.ai.behavior.BehaviorLookTarget;
import net.minecraft.world.entity.ai.behavior.BehaviorNop;
import net.minecraft.world.entity.ai.behavior.BehaviorStrollPlace;
import net.minecraft.world.entity.ai.behavior.BehaviorStrollPosition;
import net.minecraft.world.entity.ai.behavior.BehaviorStrollRandomUnconstrained;
import net.minecraft.world.entity.ai.behavior.BehaviorUtil;
import net.minecraft.world.entity.ai.behavior.BehaviorWalkAwayOutOfRange;
import net.minecraft.world.entity.ai.behavior.BehavorMove;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.schedule.Activity;

public class PiglinBruteAI {

    private static final int ANGER_DURATION = 600;
    private static final int MELEE_ATTACK_COOLDOWN = 20;
    private static final double ACTIVITY_SOUND_LIKELIHOOD_PER_TICK = 0.0125D;
    private static final int MAX_LOOK_DIST = 8;
    private static final int INTERACTION_RANGE = 8;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6F;
    private static final int HOME_CLOSE_ENOUGH_DISTANCE = 2;
    private static final int HOME_TOO_FAR_DISTANCE = 100;
    private static final int HOME_STROLL_AROUND_DISTANCE = 5;

    public PiglinBruteAI() {}

    protected static BehaviorController<?> makeBrain(EntityPiglinBrute entitypiglinbrute, BehaviorController<EntityPiglinBrute> behaviorcontroller) {
        initCoreActivity(entitypiglinbrute, behaviorcontroller);
        initIdleActivity(entitypiglinbrute, behaviorcontroller);
        initFightActivity(entitypiglinbrute, behaviorcontroller);
        behaviorcontroller.setCoreActivities(ImmutableSet.of(Activity.CORE));
        behaviorcontroller.setDefaultActivity(Activity.IDLE);
        behaviorcontroller.useDefaultActivity();
        return behaviorcontroller;
    }

    protected static void initMemories(EntityPiglinBrute entitypiglinbrute) {
        GlobalPos globalpos = GlobalPos.of(entitypiglinbrute.level().dimension(), entitypiglinbrute.blockPosition());

        entitypiglinbrute.getBrain().setMemory(MemoryModuleType.HOME, globalpos);
    }

    private static void initCoreActivity(EntityPiglinBrute entitypiglinbrute, BehaviorController<EntityPiglinBrute> behaviorcontroller) {
        behaviorcontroller.addActivity(Activity.CORE, 0, ImmutableList.of(new BehaviorLook(45, 90), new BehavorMove(), BehaviorInteractDoor.create(), BehaviorForgetAnger.create()));
    }

    private static void initIdleActivity(EntityPiglinBrute entitypiglinbrute, BehaviorController<EntityPiglinBrute> behaviorcontroller) {
        behaviorcontroller.addActivity(Activity.IDLE, 10, ImmutableList.of(BehaviorAttackTargetSet.create(PiglinBruteAI::findNearestValidAttackTarget), createIdleLookBehaviors(), createIdleMovementBehaviors(), BehaviorLookInteract.create(EntityTypes.PLAYER, 4)));
    }

    private static void initFightActivity(EntityPiglinBrute entitypiglinbrute, BehaviorController<EntityPiglinBrute> behaviorcontroller) {
        behaviorcontroller.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 10, ImmutableList.of(BehaviorAttackTargetForget.create((worldserver, entityliving) -> {
            return !isNearestValidAttackTarget(worldserver, entitypiglinbrute, entityliving);
        }), BehaviorWalkAwayOutOfRange.create(1.0F), BehaviorAttack.create(20)), MemoryModuleType.ATTACK_TARGET);
    }

    private static BehaviorGateSingle<EntityPiglinBrute> createIdleLookBehaviors() {
        return new BehaviorGateSingle<EntityPiglinBrute>(ImmutableList.of(Pair.of(BehaviorLookTarget.create(EntityTypes.PLAYER, 8.0F), 1), Pair.of(BehaviorLookTarget.create(EntityTypes.PIGLIN, 8.0F), 1), Pair.of(BehaviorLookTarget.create(EntityTypes.PIGLIN_BRUTE, 8.0F), 1), Pair.of(BehaviorLookTarget.create(8.0F), 1), Pair.of(new BehaviorNop(30, 60), 1)));
    }

    private static BehaviorGateSingle<EntityPiglinBrute> createIdleMovementBehaviors() {
        return new BehaviorGateSingle<EntityPiglinBrute>(ImmutableList.of(Pair.of(BehaviorStrollRandomUnconstrained.stroll(0.6F), 2), Pair.of(BehaviorInteract.of(EntityTypes.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2), Pair.of(BehaviorInteract.of(EntityTypes.PIGLIN_BRUTE, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2), Pair.of(BehaviorStrollPlace.create(MemoryModuleType.HOME, 0.6F, 2, 100), 2), Pair.of(BehaviorStrollPosition.create(MemoryModuleType.HOME, 0.6F, 5), 2), Pair.of(new BehaviorNop(30, 60), 1)));
    }

    protected static void updateActivity(EntityPiglinBrute entitypiglinbrute) {
        BehaviorController<EntityPiglinBrute> behaviorcontroller = entitypiglinbrute.getBrain();
        Activity activity = (Activity) behaviorcontroller.getActiveNonCoreActivity().orElse((Object) null);

        behaviorcontroller.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
        Activity activity1 = (Activity) behaviorcontroller.getActiveNonCoreActivity().orElse((Object) null);

        if (activity != activity1) {
            playActivitySound(entitypiglinbrute);
        }

        entitypiglinbrute.setAggressive(behaviorcontroller.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
    }

    private static boolean isNearestValidAttackTarget(WorldServer worldserver, EntityPiglinAbstract entitypiglinabstract, EntityLiving entityliving) {
        return findNearestValidAttackTarget(worldserver, entitypiglinabstract).filter((entityliving1) -> {
            return entityliving1 == entityliving;
        }).isPresent();
    }

    private static Optional<? extends EntityLiving> findNearestValidAttackTarget(WorldServer worldserver, EntityPiglinAbstract entitypiglinabstract) {
        Optional<EntityLiving> optional = BehaviorUtil.getLivingEntityFromUUIDMemory(entitypiglinabstract, MemoryModuleType.ANGRY_AT);

        if (optional.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(worldserver, entitypiglinabstract, (EntityLiving) optional.get())) {
            return optional;
        } else {
            Optional<? extends EntityLiving> optional1 = entitypiglinabstract.getBrain().<EntityLiving>getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);

            return optional1.isPresent() ? optional1 : entitypiglinabstract.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
        }
    }

    protected static void wasHurtBy(WorldServer worldserver, EntityPiglinBrute entitypiglinbrute, EntityLiving entityliving) {
        if (!(entityliving instanceof EntityPiglinAbstract)) {
            PiglinAI.maybeRetaliate(worldserver, entitypiglinbrute, entityliving);
        }
    }

    protected static void setAngerTarget(EntityPiglinBrute entitypiglinbrute, EntityLiving entityliving) {
        entitypiglinbrute.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        entitypiglinbrute.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, entityliving.getUUID(), 600L);
    }

    protected static void maybePlayActivitySound(EntityPiglinBrute entitypiglinbrute) {
        if ((double) entitypiglinbrute.level().random.nextFloat() < 0.0125D) {
            playActivitySound(entitypiglinbrute);
        }

    }

    private static void playActivitySound(EntityPiglinBrute entitypiglinbrute) {
        entitypiglinbrute.getBrain().getActiveNonCoreActivity().ifPresent((activity) -> {
            if (activity == Activity.FIGHT) {
                entitypiglinbrute.playAngrySound();
            }

        });
    }
}
