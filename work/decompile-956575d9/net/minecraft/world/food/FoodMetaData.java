package net.minecraft.world.food;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.level.GameRules;

public class FoodMetaData {

    private static final int DEFAULT_TICK_TIMER = 0;
    private static final float DEFAULT_EXHAUSTION_LEVEL = 0.0F;
    public int foodLevel = 20;
    public float saturationLevel = 5.0F;
    public float exhaustionLevel;
    private int tickTimer;

    public FoodMetaData() {}

    private void add(int i, float f) {
        this.foodLevel = MathHelper.clamp(i + this.foodLevel, 0, 20);
        this.saturationLevel = MathHelper.clamp(f + this.saturationLevel, 0.0F, (float) this.foodLevel);
    }

    public void eat(int i, float f) {
        this.add(i, FoodConstants.saturationByModifier(i, f));
    }

    public void eat(FoodInfo foodinfo) {
        this.add(foodinfo.nutrition(), foodinfo.saturation());
    }

    public void tick(EntityPlayer entityplayer) {
        WorldServer worldserver = entityplayer.serverLevel();
        EnumDifficulty enumdifficulty = worldserver.getDifficulty();

        if (this.exhaustionLevel > 4.0F) {
            this.exhaustionLevel -= 4.0F;
            if (this.saturationLevel > 0.0F) {
                this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
            } else if (enumdifficulty != EnumDifficulty.PEACEFUL) {
                this.foodLevel = Math.max(this.foodLevel - 1, 0);
            }
        }

        boolean flag = worldserver.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);

        if (flag && this.saturationLevel > 0.0F && entityplayer.isHurt() && this.foodLevel >= 20) {
            ++this.tickTimer;
            if (this.tickTimer >= 10) {
                float f = Math.min(this.saturationLevel, 6.0F);

                entityplayer.heal(f / 6.0F);
                this.addExhaustion(f);
                this.tickTimer = 0;
            }
        } else if (flag && this.foodLevel >= 18 && entityplayer.isHurt()) {
            ++this.tickTimer;
            if (this.tickTimer >= 80) {
                entityplayer.heal(1.0F);
                this.addExhaustion(6.0F);
                this.tickTimer = 0;
            }
        } else if (this.foodLevel <= 0) {
            ++this.tickTimer;
            if (this.tickTimer >= 80) {
                if (entityplayer.getHealth() > 10.0F || enumdifficulty == EnumDifficulty.HARD || entityplayer.getHealth() > 1.0F && enumdifficulty == EnumDifficulty.NORMAL) {
                    entityplayer.hurtServer(worldserver, entityplayer.damageSources().starve(), 1.0F);
                }

                this.tickTimer = 0;
            }
        } else {
            this.tickTimer = 0;
        }

    }

    public void readAdditionalSaveData(NBTTagCompound nbttagcompound) {
        this.foodLevel = nbttagcompound.getIntOr("foodLevel", 20);
        this.tickTimer = nbttagcompound.getIntOr("foodTickTimer", 0);
        this.saturationLevel = nbttagcompound.getFloatOr("foodSaturationLevel", 5.0F);
        this.exhaustionLevel = nbttagcompound.getFloatOr("foodExhaustionLevel", 0.0F);
    }

    public void addAdditionalSaveData(NBTTagCompound nbttagcompound) {
        nbttagcompound.putInt("foodLevel", this.foodLevel);
        nbttagcompound.putInt("foodTickTimer", this.tickTimer);
        nbttagcompound.putFloat("foodSaturationLevel", this.saturationLevel);
        nbttagcompound.putFloat("foodExhaustionLevel", this.exhaustionLevel);
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public boolean needsFood() {
        return this.foodLevel < 20;
    }

    public void addExhaustion(float f) {
        this.exhaustionLevel = Math.min(this.exhaustionLevel + f, 40.0F);
    }

    public float getSaturationLevel() {
        return this.saturationLevel;
    }

    public void setFoodLevel(int i) {
        this.foodLevel = i;
    }

    public void setSaturation(float f) {
        this.saturationLevel = f;
    }
}
