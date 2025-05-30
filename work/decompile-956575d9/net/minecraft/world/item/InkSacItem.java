package net.minecraft.world.item;

import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntitySign;

public class InkSacItem extends Item implements SignApplicator {

    public InkSacItem(Item.Info item_info) {
        super(item_info);
    }

    @Override
    public boolean tryApplyToSign(World world, TileEntitySign tileentitysign, boolean flag, EntityHuman entityhuman) {
        if (tileentitysign.updateText((signtext) -> {
            return signtext.setHasGlowingText(false);
        }, flag)) {
            world.playSound((Entity) null, tileentitysign.getBlockPos(), SoundEffects.INK_SAC_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return true;
        } else {
            return false;
        }
    }
}
