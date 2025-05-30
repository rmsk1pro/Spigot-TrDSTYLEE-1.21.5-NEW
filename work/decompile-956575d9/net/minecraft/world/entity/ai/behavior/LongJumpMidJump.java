package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class LongJumpMidJump extends Behavior<EntityInsentient> {

    public static final int TIME_OUT_DURATION = 100;
    private final UniformInt timeBetweenLongJumps;
    private final SoundEffect landingSound;

    public LongJumpMidJump(UniformInt uniformint, SoundEffect soundeffect) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_PRESENT), 100);
        this.timeBetweenLongJumps = uniformint;
        this.landingSound = soundeffect;
    }

    protected boolean canStillUse(WorldServer worldserver, EntityInsentient entityinsentient, long i) {
        return !entityinsentient.onGround();
    }

    protected void start(WorldServer worldserver, EntityInsentient entityinsentient, long i) {
        entityinsentient.setDiscardFriction(true);
        entityinsentient.setPose(EntityPose.LONG_JUMPING);
    }

    protected void stop(WorldServer worldserver, EntityInsentient entityinsentient, long i) {
        if (entityinsentient.onGround()) {
            entityinsentient.setDeltaMovement(entityinsentient.getDeltaMovement().multiply((double) 0.1F, 1.0D, (double) 0.1F));
            worldserver.playSound((Entity) null, (Entity) entityinsentient, this.landingSound, SoundCategory.NEUTRAL, 2.0F, 1.0F);
        }

        entityinsentient.setDiscardFriction(false);
        entityinsentient.setPose(EntityPose.STANDING);
        entityinsentient.getBrain().eraseMemory(MemoryModuleType.LONG_JUMP_MID_JUMP);
        entityinsentient.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(worldserver.random));
    }
}
