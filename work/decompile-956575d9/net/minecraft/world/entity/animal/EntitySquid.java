package net.minecraft.world.entity.animal;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3D;

public class EntitySquid extends AgeableWaterCreature {

    public float xBodyRot;
    public float xBodyRotO;
    public float zBodyRot;
    public float zBodyRotO;
    public float tentacleMovement;
    public float oldTentacleMovement;
    public float tentacleAngle;
    public float oldTentacleAngle;
    private float speed;
    private float tentacleSpeed;
    private float rotateSpeed;
    Vec3D movementVector;

    public EntitySquid(EntityTypes<? extends EntitySquid> entitytypes, World world) {
        super(entitytypes, world);
        this.movementVector = Vec3D.ZERO;
        this.random.setSeed((long) this.getId());
        this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new EntitySquid.PathfinderGoalSquid(this));
        this.goalSelector.addGoal(1, new EntitySquid.a());
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 10.0D);
    }

    @Override
    protected SoundEffect getAmbientSound() {
        return SoundEffects.SQUID_AMBIENT;
    }

    @Override
    protected SoundEffect getHurtSound(DamageSource damagesource) {
        return SoundEffects.SQUID_HURT;
    }

    @Override
    protected SoundEffect getDeathSound() {
        return SoundEffects.SQUID_DEATH;
    }

    protected SoundEffect getSquirtSound() {
        return SoundEffects.SQUID_SQUIRT;
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Nullable
    @Override
    public EntityAgeable getBreedOffspring(WorldServer worldserver, EntityAgeable entityageable) {
        return EntityTypes.SQUID.create(worldserver, EntitySpawnReason.BREEDING);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.08D;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.xBodyRotO = this.xBodyRot;
        this.zBodyRotO = this.zBodyRot;
        this.oldTentacleMovement = this.tentacleMovement;
        this.oldTentacleAngle = this.tentacleAngle;
        this.tentacleMovement += this.tentacleSpeed;
        if ((double) this.tentacleMovement > (Math.PI * 2D)) {
            if (this.level().isClientSide) {
                this.tentacleMovement = ((float) Math.PI * 2F);
            } else {
                this.tentacleMovement -= ((float) Math.PI * 2F);
                if (this.random.nextInt(10) == 0) {
                    this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
                }

                this.level().broadcastEntityEvent(this, (byte) 19);
            }
        }

        if (this.isInWater()) {
            if (this.tentacleMovement < (float) Math.PI) {
                float f = this.tentacleMovement / (float) Math.PI;

                this.tentacleAngle = MathHelper.sin(f * f * (float) Math.PI) * (float) Math.PI * 0.25F;
                if ((double) f > 0.75D) {
                    if (this.isLocalInstanceAuthoritative()) {
                        this.setDeltaMovement(this.movementVector);
                    }

                    this.rotateSpeed = 1.0F;
                } else {
                    this.rotateSpeed *= 0.8F;
                }
            } else {
                this.tentacleAngle = 0.0F;
                if (this.isLocalInstanceAuthoritative()) {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
                }

                this.rotateSpeed *= 0.99F;
            }

            Vec3D vec3d = this.getDeltaMovement();
            double d0 = vec3d.horizontalDistance();

            this.yBodyRot += (-((float) MathHelper.atan2(vec3d.x, vec3d.z)) * (180F / (float) Math.PI) - this.yBodyRot) * 0.1F;
            this.setYRot(this.yBodyRot);
            this.zBodyRot += (float) Math.PI * this.rotateSpeed * 1.5F;
            this.xBodyRot += (-((float) MathHelper.atan2(d0, vec3d.y)) * (180F / (float) Math.PI) - this.xBodyRot) * 0.1F;
        } else {
            this.tentacleAngle = MathHelper.abs(MathHelper.sin(this.tentacleMovement)) * (float) Math.PI * 0.25F;
            if (!this.level().isClientSide) {
                double d1 = this.getDeltaMovement().y;

                if (this.hasEffect(MobEffects.LEVITATION)) {
                    d1 = 0.05D * (double) (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1);
                } else {
                    d1 -= this.getGravity();
                }

                this.setDeltaMovement(0.0D, d1 * (double) 0.98F, 0.0D);
            }

            this.xBodyRot += (-90.0F - this.xBodyRot) * 0.02F;
        }

    }

    @Override
    public boolean hurtServer(WorldServer worldserver, DamageSource damagesource, float f) {
        if (super.hurtServer(worldserver, damagesource, f) && this.getLastHurtByMob() != null) {
            this.spawnInk();
            return true;
        } else {
            return false;
        }
    }

    private Vec3D rotateVector(Vec3D vec3d) {
        Vec3D vec3d1 = vec3d.xRot(this.xBodyRotO * ((float) Math.PI / 180F));

        vec3d1 = vec3d1.yRot(-this.yBodyRotO * ((float) Math.PI / 180F));
        return vec3d1;
    }

    private void spawnInk() {
        this.makeSound(this.getSquirtSound());
        Vec3D vec3d = this.rotateVector(new Vec3D(0.0D, -1.0D, 0.0D)).add(this.getX(), this.getY(), this.getZ());

        for (int i = 0; i < 30; ++i) {
            Vec3D vec3d1 = this.rotateVector(new Vec3D((double) this.random.nextFloat() * 0.6D - 0.3D, -1.0D, (double) this.random.nextFloat() * 0.6D - 0.3D));
            float f = this.isBaby() ? 0.1F : 0.3F;
            Vec3D vec3d2 = vec3d1.scale((double) (f + this.random.nextFloat() * 2.0F));

            ((WorldServer) this.level()).sendParticles(this.getInkParticle(), vec3d.x, vec3d.y + 0.5D, vec3d.z, 0, vec3d2.x, vec3d2.y, vec3d2.z, (double) 0.1F);
        }

    }

    protected ParticleParam getInkParticle() {
        return Particles.SQUID_INK;
    }

    @Override
    public void travel(Vec3D vec3d) {
        this.move(EnumMoveType.SELF, this.getDeltaMovement());
    }

    @Override
    public void handleEntityEvent(byte b0) {
        if (b0 == 19) {
            this.tentacleMovement = 0.0F;
        } else {
            super.handleEntityEvent(b0);
        }

    }

    public boolean hasMovementVector() {
        return this.movementVector.lengthSqr() > (double) 1.0E-5F;
    }

    @Nullable
    @Override
    public GroupDataEntity finalizeSpawn(WorldAccess worldaccess, DifficultyDamageScaler difficultydamagescaler, EntitySpawnReason entityspawnreason, @Nullable GroupDataEntity groupdataentity) {
        GroupDataEntity groupdataentity1 = (GroupDataEntity) Objects.requireNonNullElseGet(groupdataentity, () -> {
            return new EntityAgeable.a(0.05F);
        });

        return super.finalizeSpawn(worldaccess, difficultydamagescaler, entityspawnreason, groupdataentity1);
    }

    private static class PathfinderGoalSquid extends PathfinderGoal {

        private final EntitySquid squid;

        public PathfinderGoalSquid(EntitySquid entitysquid) {
            this.squid = entitysquid;
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            int i = this.squid.getNoActionTime();

            if (i > 100) {
                this.squid.movementVector = Vec3D.ZERO;
            } else if (this.squid.getRandom().nextInt(reducedTickDelay(50)) == 0 || !this.squid.wasTouchingWater || !this.squid.hasMovementVector()) {
                float f = this.squid.getRandom().nextFloat() * ((float) Math.PI * 2F);

                this.squid.movementVector = new Vec3D((double) (MathHelper.cos(f) * 0.2F), (double) (-0.1F + this.squid.getRandom().nextFloat() * 0.2F), (double) (MathHelper.sin(f) * 0.2F));
            }

        }
    }

    private class a extends PathfinderGoal {

        private static final float SQUID_FLEE_SPEED = 3.0F;
        private static final float SQUID_FLEE_MIN_DISTANCE = 5.0F;
        private static final float SQUID_FLEE_MAX_DISTANCE = 10.0F;
        private int fleeTicks;

        a() {}

        @Override
        public boolean canUse() {
            EntityLiving entityliving = EntitySquid.this.getLastHurtByMob();

            return EntitySquid.this.isInWater() && entityliving != null ? EntitySquid.this.distanceToSqr((Entity) entityliving) < 100.0D : false;
        }

        @Override
        public void start() {
            this.fleeTicks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ++this.fleeTicks;
            EntityLiving entityliving = EntitySquid.this.getLastHurtByMob();

            if (entityliving != null) {
                Vec3D vec3d = new Vec3D(EntitySquid.this.getX() - entityliving.getX(), EntitySquid.this.getY() - entityliving.getY(), EntitySquid.this.getZ() - entityliving.getZ());
                IBlockData iblockdata = EntitySquid.this.level().getBlockState(BlockPosition.containing(EntitySquid.this.getX() + vec3d.x, EntitySquid.this.getY() + vec3d.y, EntitySquid.this.getZ() + vec3d.z));
                Fluid fluid = EntitySquid.this.level().getFluidState(BlockPosition.containing(EntitySquid.this.getX() + vec3d.x, EntitySquid.this.getY() + vec3d.y, EntitySquid.this.getZ() + vec3d.z));

                if (fluid.is(TagsFluid.WATER) || iblockdata.isAir()) {
                    double d0 = vec3d.length();

                    if (d0 > 0.0D) {
                        vec3d.normalize();
                        double d1 = 3.0D;

                        if (d0 > 5.0D) {
                            d1 -= (d0 - 5.0D) / 5.0D;
                        }

                        if (d1 > 0.0D) {
                            vec3d = vec3d.scale(d1);
                        }
                    }

                    if (iblockdata.isAir()) {
                        vec3d = vec3d.subtract(0.0D, vec3d.y, 0.0D);
                    }

                    EntitySquid.this.movementVector = new Vec3D(vec3d.x / 20.0D, vec3d.y / 20.0D, vec3d.z / 20.0D);
                }

                if (this.fleeTicks % 10 == 5) {
                    EntitySquid.this.level().addParticle(Particles.BUBBLE, EntitySquid.this.getX(), EntitySquid.this.getY(), EntitySquid.this.getZ(), 0.0D, 0.0D, 0.0D);
                }

            }
        }
    }
}
