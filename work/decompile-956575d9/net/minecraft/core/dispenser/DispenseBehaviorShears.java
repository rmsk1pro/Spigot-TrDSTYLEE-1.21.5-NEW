package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Holder;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.IShearable;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BlockBeehive;
import net.minecraft.world.level.block.BlockDispenser;
import net.minecraft.world.level.block.entity.TileEntityBeehive;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;

public class DispenseBehaviorShears extends DispenseBehaviorMaybe {

    public DispenseBehaviorShears() {}

    @Override
    protected ItemStack execute(SourceBlock sourceblock, ItemStack itemstack) {
        WorldServer worldserver = sourceblock.level();

        if (!worldserver.isClientSide()) {
            BlockPosition blockposition = sourceblock.pos().relative((EnumDirection) sourceblock.state().getValue(BlockDispenser.FACING));

            this.setSuccess(tryShearBeehive(worldserver, blockposition) || tryShearLivingEntity(worldserver, blockposition, itemstack));
            if (this.isSuccess()) {
                itemstack.hurtAndBreak(1, worldserver, (EntityPlayer) null, (item) -> {
                });
            }
        }

        return itemstack;
    }

    private static boolean tryShearBeehive(WorldServer worldserver, BlockPosition blockposition) {
        IBlockData iblockdata = worldserver.getBlockState(blockposition);

        if (iblockdata.is(TagsBlock.BEEHIVES, (blockbase_blockdata) -> {
            return blockbase_blockdata.hasProperty(BlockBeehive.HONEY_LEVEL) && blockbase_blockdata.getBlock() instanceof BlockBeehive;
        })) {
            int i = (Integer) iblockdata.getValue(BlockBeehive.HONEY_LEVEL);

            if (i >= 5) {
                worldserver.playSound((Entity) null, blockposition, SoundEffects.BEEHIVE_SHEAR, SoundCategory.BLOCKS, 1.0F, 1.0F);
                BlockBeehive.dropHoneycomb(worldserver, blockposition);
                ((BlockBeehive) iblockdata.getBlock()).releaseBeesAndResetHoneyLevel(worldserver, iblockdata, blockposition, (EntityHuman) null, TileEntityBeehive.ReleaseStatus.BEE_RELEASED);
                worldserver.gameEvent((Entity) null, (Holder) GameEvent.SHEAR, blockposition);
                return true;
            }
        }

        return false;
    }

    private static boolean tryShearLivingEntity(WorldServer worldserver, BlockPosition blockposition, ItemStack itemstack) {
        for (EntityLiving entityliving : worldserver.getEntitiesOfClass(EntityLiving.class, new AxisAlignedBB(blockposition), IEntitySelector.NO_SPECTATORS)) {
            if (entityliving instanceof IShearable ishearable) {
                if (ishearable.readyForShearing()) {
                    ishearable.shear(worldserver, SoundCategory.BLOCKS, itemstack);
                    worldserver.gameEvent((Entity) null, (Holder) GameEvent.SHEAR, blockposition);
                    return true;
                }
            }
        }

        return false;
    }
}
