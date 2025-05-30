package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class FrogspawnBlock extends Block {

    public static final MapCodec<FrogspawnBlock> CODEC = simpleCodec(FrogspawnBlock::new);
    private static final int MIN_TADPOLES_SPAWN = 2;
    private static final int MAX_TADPOLES_SPAWN = 5;
    private static final int DEFAULT_MIN_HATCH_TICK_DELAY = 3600;
    private static final int DEFAULT_MAX_HATCH_TICK_DELAY = 12000;
    private static final VoxelShape SHAPE = Block.column(16.0D, 0.0D, 1.5D);
    private static int minHatchTickDelay = 3600;
    private static int maxHatchTickDelay = 12000;

    @Override
    public MapCodec<FrogspawnBlock> codec() {
        return FrogspawnBlock.CODEC;
    }

    public FrogspawnBlock(BlockBase.Info blockbase_info) {
        super(blockbase_info);
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return FrogspawnBlock.SHAPE;
    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        return mayPlaceOn(iworldreader, blockposition.below());
    }

    @Override
    protected void onPlace(IBlockData iblockdata, World world, BlockPosition blockposition, IBlockData iblockdata1, boolean flag) {
        world.scheduleTick(blockposition, (Block) this, getFrogspawnHatchDelay(world.getRandom()));
    }

    private static int getFrogspawnHatchDelay(RandomSource randomsource) {
        return randomsource.nextInt(FrogspawnBlock.minHatchTickDelay, FrogspawnBlock.maxHatchTickDelay);
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        return !this.canSurvive(iblockdata, iworldreader, blockposition) ? Blocks.AIR.defaultBlockState() : super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
    }

    @Override
    protected void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        if (!this.canSurvive(iblockdata, worldserver, blockposition)) {
            this.destroyBlock(worldserver, blockposition);
        } else {
            this.hatchFrogspawn(worldserver, blockposition, randomsource);
        }
    }

    @Override
    protected void entityInside(IBlockData iblockdata, World world, BlockPosition blockposition, Entity entity, InsideBlockEffectApplier insideblockeffectapplier) {
        if (entity.getType().equals(EntityTypes.FALLING_BLOCK)) {
            this.destroyBlock(world, blockposition);
        }

    }

    private static boolean mayPlaceOn(IBlockAccess iblockaccess, BlockPosition blockposition) {
        Fluid fluid = iblockaccess.getFluidState(blockposition);
        Fluid fluid1 = iblockaccess.getFluidState(blockposition.above());

        return fluid.getType() == FluidTypes.WATER && fluid1.getType() == FluidTypes.EMPTY;
    }

    private void hatchFrogspawn(WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        this.destroyBlock(worldserver, blockposition);
        worldserver.playSound((Entity) null, blockposition, SoundEffects.FROGSPAWN_HATCH, SoundCategory.BLOCKS, 1.0F, 1.0F);
        this.spawnTadpoles(worldserver, blockposition, randomsource);
    }

    private void destroyBlock(World world, BlockPosition blockposition) {
        world.destroyBlock(blockposition, false);
    }

    private void spawnTadpoles(WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        int i = randomsource.nextInt(2, 6);

        for (int j = 1; j <= i; ++j) {
            Tadpole tadpole = EntityTypes.TADPOLE.create(worldserver, EntitySpawnReason.BREEDING);

            if (tadpole != null) {
                double d0 = (double) blockposition.getX() + this.getRandomTadpolePositionOffset(randomsource);
                double d1 = (double) blockposition.getZ() + this.getRandomTadpolePositionOffset(randomsource);
                int k = randomsource.nextInt(1, 361);

                tadpole.snapTo(d0, (double) blockposition.getY() - 0.5D, d1, (float) k, 0.0F);
                tadpole.setPersistenceRequired();
                worldserver.addFreshEntity(tadpole);
            }
        }

    }

    private double getRandomTadpolePositionOffset(RandomSource randomsource) {
        double d0 = (double) 0.2F;

        return MathHelper.clamp(randomsource.nextDouble(), (double) 0.2F, 0.7999999970197678D);
    }

    @VisibleForTesting
    public static void setHatchDelay(int i, int j) {
        FrogspawnBlock.minHatchTickDelay = i;
        FrogspawnBlock.maxHatchTickDelay = j;
    }

    @VisibleForTesting
    public static void setDefaultHatchDelay() {
        FrogspawnBlock.minHatchTickDelay = 3600;
        FrogspawnBlock.maxHatchTickDelay = 12000;
    }
}
