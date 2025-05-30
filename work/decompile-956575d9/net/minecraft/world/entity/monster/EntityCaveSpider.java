package net.minecraft.world.entity.monster;

import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.phys.Vec3D;

public class EntityCaveSpider extends EntitySpider {

    public EntityCaveSpider(EntityTypes<? extends EntityCaveSpider> entitytypes, World world) {
        super(entitytypes, world);
    }

    public static AttributeProvider.Builder createCaveSpider() {
        return EntitySpider.createAttributes().add(GenericAttributes.MAX_HEALTH, 12.0D);
    }

    @Override
    public boolean doHurtTarget(WorldServer worldserver, Entity entity) {
        if (super.doHurtTarget(worldserver, entity)) {
            if (entity instanceof EntityLiving) {
                int i = 0;

                if (this.level().getDifficulty() == EnumDifficulty.NORMAL) {
                    i = 7;
                } else if (this.level().getDifficulty() == EnumDifficulty.HARD) {
                    i = 15;
                }

                if (i > 0) {
                    ((EntityLiving) entity).addEffect(new MobEffect(MobEffects.POISON, i * 20, 0), this);
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Nullable
    @Override
    public GroupDataEntity finalizeSpawn(WorldAccess worldaccess, DifficultyDamageScaler difficultydamagescaler, EntitySpawnReason entityspawnreason, @Nullable GroupDataEntity groupdataentity) {
        return groupdataentity;
    }

    @Override
    public Vec3D getVehicleAttachmentPoint(Entity entity) {
        return entity.getBbWidth() <= this.getBbWidth() ? new Vec3D(0.0D, 0.21875D * (double) this.getScale(), 0.0D) : super.getVehicleAttachmentPoint(entity);
    }
}
