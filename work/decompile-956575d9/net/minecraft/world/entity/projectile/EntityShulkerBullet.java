package net.minecraft.world.entity.projectile;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

public class EntityShulkerBullet extends IProjectile {

    private static final double SPEED = 0.15D;
    @Nullable
    private Entity finalTarget;
    @Nullable
    private EnumDirection currentMoveDirection;
    private int flightSteps;
    private double targetDeltaX;
    private double targetDeltaY;
    private double targetDeltaZ;
    @Nullable
    private UUID targetId;

    public EntityShulkerBullet(EntityTypes<? extends EntityShulkerBullet> entitytypes, World world) {
        super(entitytypes, world);
        this.noPhysics = true;
    }

    public EntityShulkerBullet(World world, EntityLiving entityliving, Entity entity, EnumDirection.EnumAxis enumdirection_enumaxis) {
        this(EntityTypes.SHULKER_BULLET, world);
        this.setOwner(entityliving);
        Vec3D vec3d = entityliving.getBoundingBox().getCenter();

        this.snapTo(vec3d.x, vec3d.y, vec3d.z, this.getYRot(), this.getXRot());
        this.finalTarget = entity;
        this.currentMoveDirection = EnumDirection.UP;
        this.selectNextMoveDirection(enumdirection_enumaxis);
    }

    @Override
    public SoundCategory getSoundSource() {
        return SoundCategory.HOSTILE;
    }

    @Override
    protected void addAdditionalSaveData(NBTTagCompound nbttagcompound) {
        super.addAdditionalSaveData(nbttagcompound);
        if (this.finalTarget != null) {
            nbttagcompound.store("Target", UUIDUtil.CODEC, this.finalTarget.getUUID());
        }

        nbttagcompound.storeNullable("Dir", EnumDirection.LEGACY_ID_CODEC, this.currentMoveDirection);
        nbttagcompound.putInt("Steps", this.flightSteps);
        nbttagcompound.putDouble("TXD", this.targetDeltaX);
        nbttagcompound.putDouble("TYD", this.targetDeltaY);
        nbttagcompound.putDouble("TZD", this.targetDeltaZ);
    }

    @Override
    protected void readAdditionalSaveData(NBTTagCompound nbttagcompound) {
        super.readAdditionalSaveData(nbttagcompound);
        this.flightSteps = nbttagcompound.getIntOr("Steps", 0);
        this.targetDeltaX = nbttagcompound.getDoubleOr("TXD", 0.0D);
        this.targetDeltaY = nbttagcompound.getDoubleOr("TYD", 0.0D);
        this.targetDeltaZ = nbttagcompound.getDoubleOr("TZD", 0.0D);
        this.currentMoveDirection = (EnumDirection) nbttagcompound.read("Dir", EnumDirection.LEGACY_ID_CODEC).orElse((Object) null);
        this.targetId = (UUID) nbttagcompound.read("Target", UUIDUtil.CODEC).orElse((Object) null);
    }

    @Override
    protected void defineSynchedData(DataWatcher.a datawatcher_a) {}

    @Nullable
    private EnumDirection getMoveDirection() {
        return this.currentMoveDirection;
    }

    private void setMoveDirection(@Nullable EnumDirection enumdirection) {
        this.currentMoveDirection = enumdirection;
    }

    private void selectNextMoveDirection(@Nullable EnumDirection.EnumAxis enumdirection_enumaxis) {
        double d0 = 0.5D;
        BlockPosition blockposition;

        if (this.finalTarget == null) {
            blockposition = this.blockPosition().below();
        } else {
            d0 = (double) this.finalTarget.getBbHeight() * 0.5D;
            blockposition = BlockPosition.containing(this.finalTarget.getX(), this.finalTarget.getY() + d0, this.finalTarget.getZ());
        }

        double d1 = (double) blockposition.getX() + 0.5D;
        double d2 = (double) blockposition.getY() + d0;
        double d3 = (double) blockposition.getZ() + 0.5D;
        EnumDirection enumdirection = null;

        if (!blockposition.closerToCenterThan(this.position(), 2.0D)) {
            BlockPosition blockposition1 = this.blockPosition();
            List<EnumDirection> list = Lists.newArrayList();

            if (enumdirection_enumaxis != EnumDirection.EnumAxis.X) {
                if (blockposition1.getX() < blockposition.getX() && this.level().isEmptyBlock(blockposition1.east())) {
                    list.add(EnumDirection.EAST);
                } else if (blockposition1.getX() > blockposition.getX() && this.level().isEmptyBlock(blockposition1.west())) {
                    list.add(EnumDirection.WEST);
                }
            }

            if (enumdirection_enumaxis != EnumDirection.EnumAxis.Y) {
                if (blockposition1.getY() < blockposition.getY() && this.level().isEmptyBlock(blockposition1.above())) {
                    list.add(EnumDirection.UP);
                } else if (blockposition1.getY() > blockposition.getY() && this.level().isEmptyBlock(blockposition1.below())) {
                    list.add(EnumDirection.DOWN);
                }
            }

            if (enumdirection_enumaxis != EnumDirection.EnumAxis.Z) {
                if (blockposition1.getZ() < blockposition.getZ() && this.level().isEmptyBlock(blockposition1.south())) {
                    list.add(EnumDirection.SOUTH);
                } else if (blockposition1.getZ() > blockposition.getZ() && this.level().isEmptyBlock(blockposition1.north())) {
                    list.add(EnumDirection.NORTH);
                }
            }

            enumdirection = EnumDirection.getRandom(this.random);
            if (list.isEmpty()) {
                for (int i = 5; !this.level().isEmptyBlock(blockposition1.relative(enumdirection)) && i > 0; --i) {
                    enumdirection = EnumDirection.getRandom(this.random);
                }
            } else {
                enumdirection = (EnumDirection) list.get(this.random.nextInt(list.size()));
            }

            d1 = this.getX() + (double) enumdirection.getStepX();
            d2 = this.getY() + (double) enumdirection.getStepY();
            d3 = this.getZ() + (double) enumdirection.getStepZ();
        }

        this.setMoveDirection(enumdirection);
        double d4 = d1 - this.getX();
        double d5 = d2 - this.getY();
        double d6 = d3 - this.getZ();
        double d7 = Math.sqrt(d4 * d4 + d5 * d5 + d6 * d6);

        if (d7 == 0.0D) {
            this.targetDeltaX = 0.0D;
            this.targetDeltaY = 0.0D;
            this.targetDeltaZ = 0.0D;
        } else {
            this.targetDeltaX = d4 / d7 * 0.15D;
            this.targetDeltaY = d5 / d7 * 0.15D;
            this.targetDeltaZ = d6 / d7 * 0.15D;
        }

        this.hasImpulse = true;
        this.flightSteps = 10 + this.random.nextInt(5) * 10;
    }

    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == EnumDifficulty.PEACEFUL) {
            this.discard();
        }

    }

    @Override
    protected double getDefaultGravity() {
        return 0.04D;
    }

    @Override
    public void tick() {
        super.tick();
        MovingObjectPosition movingobjectposition = null;

        if (!this.level().isClientSide) {
            if (this.finalTarget == null && this.targetId != null) {
                this.finalTarget = ((WorldServer) this.level()).getEntity(this.targetId);
                if (this.finalTarget == null) {
                    this.targetId = null;
                }
            }

            if (this.finalTarget == null || !this.finalTarget.isAlive() || this.finalTarget instanceof EntityHuman && this.finalTarget.isSpectator()) {
                this.applyGravity();
            } else {
                this.targetDeltaX = MathHelper.clamp(this.targetDeltaX * 1.025D, -1.0D, 1.0D);
                this.targetDeltaY = MathHelper.clamp(this.targetDeltaY * 1.025D, -1.0D, 1.0D);
                this.targetDeltaZ = MathHelper.clamp(this.targetDeltaZ * 1.025D, -1.0D, 1.0D);
                Vec3D vec3d = this.getDeltaMovement();

                this.setDeltaMovement(vec3d.add((this.targetDeltaX - vec3d.x) * 0.2D, (this.targetDeltaY - vec3d.y) * 0.2D, (this.targetDeltaZ - vec3d.z) * 0.2D));
            }

            movingobjectposition = ProjectileHelper.getHitResultOnMoveVector(this, this::canHitEntity);
        }

        Vec3D vec3d1 = this.getDeltaMovement();

        this.setPos(this.position().add(vec3d1));
        this.applyEffectsFromBlocks();
        if (this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
            this.handlePortal();
        }

        if (movingobjectposition != null && this.isAlive() && movingobjectposition.getType() != MovingObjectPosition.EnumMovingObjectType.MISS) {
            this.hitTargetOrDeflectSelf(movingobjectposition);
        }

        ProjectileHelper.rotateTowardsMovement(this, 0.5F);
        if (this.level().isClientSide) {
            this.level().addParticle(Particles.END_ROD, this.getX() - vec3d1.x, this.getY() - vec3d1.y + 0.15D, this.getZ() - vec3d1.z, 0.0D, 0.0D, 0.0D);
        } else if (this.finalTarget != null && !this.finalTarget.isRemoved()) {
            if (this.flightSteps > 0) {
                --this.flightSteps;
                if (this.flightSteps == 0) {
                    this.selectNextMoveDirection(this.currentMoveDirection == null ? null : this.currentMoveDirection.getAxis());
                }
            }

            if (this.currentMoveDirection != null) {
                BlockPosition blockposition = this.blockPosition();
                EnumDirection.EnumAxis enumdirection_enumaxis = this.currentMoveDirection.getAxis();

                if (this.level().loadedAndEntityCanStandOn(blockposition.relative(this.currentMoveDirection), this)) {
                    this.selectNextMoveDirection(enumdirection_enumaxis);
                } else {
                    BlockPosition blockposition1 = this.finalTarget.blockPosition();

                    if (enumdirection_enumaxis == EnumDirection.EnumAxis.X && blockposition.getX() == blockposition1.getX() || enumdirection_enumaxis == EnumDirection.EnumAxis.Z && blockposition.getZ() == blockposition1.getZ() || enumdirection_enumaxis == EnumDirection.EnumAxis.Y && blockposition.getY() == blockposition1.getY()) {
                        this.selectNextMoveDirection(enumdirection_enumaxis);
                    }
                }
            }
        }

    }

    @Override
    protected boolean isAffectedByBlocks() {
        return !this.isRemoved();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !entity.noPhysics;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d0) {
        return d0 < 16384.0D;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity movingobjectpositionentity) {
        super.onHitEntity(movingobjectpositionentity);
        Entity entity = movingobjectpositionentity.getEntity();
        Entity entity1 = this.getOwner();
        EntityLiving entityliving = entity1 instanceof EntityLiving ? (EntityLiving) entity1 : null;
        DamageSource damagesource = this.damageSources().mobProjectile(this, entityliving);
        boolean flag = entity.hurtOrSimulate(damagesource, 4.0F);

        if (flag) {
            World world = this.level();

            if (world instanceof WorldServer) {
                WorldServer worldserver = (WorldServer) world;

                EnchantmentManager.doPostAttackEffects(worldserver, entity, damagesource);
            }

            if (entity instanceof EntityLiving) {
                EntityLiving entityliving1 = (EntityLiving) entity;

                entityliving1.addEffect(new MobEffect(MobEffects.LEVITATION, 200), (Entity) MoreObjects.firstNonNull(entity1, this));
            }
        }

    }

    @Override
    protected void onHitBlock(MovingObjectPositionBlock movingobjectpositionblock) {
        super.onHitBlock(movingobjectpositionblock);
        ((WorldServer) this.level()).sendParticles(Particles.EXPLOSION, this.getX(), this.getY(), this.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
        this.playSound(SoundEffects.SHULKER_BULLET_HIT, 1.0F, 1.0F);
    }

    private void destroy() {
        this.discard();
        this.level().gameEvent(GameEvent.ENTITY_DAMAGE, this.position(), GameEvent.a.of((Entity) this));
    }

    @Override
    protected void onHit(MovingObjectPosition movingobjectposition) {
        super.onHit(movingobjectposition);
        this.destroy();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurtClient(DamageSource damagesource) {
        return true;
    }

    @Override
    public boolean hurtServer(WorldServer worldserver, DamageSource damagesource, float f) {
        this.playSound(SoundEffects.SHULKER_BULLET_HURT, 1.0F, 1.0F);
        worldserver.sendParticles(Particles.CRIT, this.getX(), this.getY(), this.getZ(), 15, 0.2D, 0.2D, 0.2D, 0.0D);
        this.destroy();
        return true;
    }

    @Override
    public void recreateFromPacket(PacketPlayOutSpawnEntity packetplayoutspawnentity) {
        super.recreateFromPacket(packetplayoutspawnentity);
        double d0 = packetplayoutspawnentity.getXa();
        double d1 = packetplayoutspawnentity.getYa();
        double d2 = packetplayoutspawnentity.getZa();

        this.setDeltaMovement(d0, d1, d2);
    }
}
