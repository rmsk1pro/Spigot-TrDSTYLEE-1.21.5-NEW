package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.OptionalInt;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;

public abstract class BlockLeaves extends Block implements IBlockWaterlogged {

    public static final int DECAY_DISTANCE = 7;
    public static final BlockStateInteger DISTANCE = BlockProperties.DISTANCE;
    public static final BlockStateBoolean PERSISTENT = BlockProperties.PERSISTENT;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    protected final float leafParticleChance;
    private static final int TICK_DELAY = 1;

    @Override
    public abstract MapCodec<? extends BlockLeaves> codec();

    public BlockLeaves(float f, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.leafParticleChance = f;
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(BlockLeaves.DISTANCE, 7)).setValue(BlockLeaves.PERSISTENT, false)).setValue(BlockLeaves.WATERLOGGED, false));
    }

    @Override
    protected VoxelShape getBlockSupportShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return VoxelShapes.empty();
    }

    @Override
    protected boolean isRandomlyTicking(IBlockData iblockdata) {
        return (Integer) iblockdata.getValue(BlockLeaves.DISTANCE) == 7 && !(Boolean) iblockdata.getValue(BlockLeaves.PERSISTENT);
    }

    @Override
    protected void randomTick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        if (this.decaying(iblockdata)) {
            dropResources(iblockdata, worldserver, blockposition);
            worldserver.removeBlock(blockposition, false);
        }

    }

    protected boolean decaying(IBlockData iblockdata) {
        return !(Boolean) iblockdata.getValue(BlockLeaves.PERSISTENT) && (Integer) iblockdata.getValue(BlockLeaves.DISTANCE) == 7;
    }

    @Override
    protected void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        worldserver.setBlock(blockposition, updateDistance(iblockdata, worldserver, blockposition), 3);
    }

    @Override
    protected int getLightBlock(IBlockData iblockdata) {
        return 1;
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        if ((Boolean) iblockdata.getValue(BlockLeaves.WATERLOGGED)) {
            scheduledtickaccess.scheduleTick(blockposition, (FluidType) FluidTypes.WATER, FluidTypes.WATER.getTickDelay(iworldreader));
        }

        int i = getDistanceAt(iblockdata1) + 1;

        if (i != 1 || (Integer) iblockdata.getValue(BlockLeaves.DISTANCE) != i) {
            scheduledtickaccess.scheduleTick(blockposition, (Block) this, 1);
        }

        return iblockdata;
    }

    private static IBlockData updateDistance(IBlockData iblockdata, GeneratorAccess generatoraccess, BlockPosition blockposition) {
        int i = 7;
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();

        for (EnumDirection enumdirection : EnumDirection.values()) {
            blockposition_mutableblockposition.setWithOffset(blockposition, enumdirection);
            i = Math.min(i, getDistanceAt(generatoraccess.getBlockState(blockposition_mutableblockposition)) + 1);
            if (i == 1) {
                break;
            }
        }

        return (IBlockData) iblockdata.setValue(BlockLeaves.DISTANCE, i);
    }

    private static int getDistanceAt(IBlockData iblockdata) {
        return getOptionalDistanceAt(iblockdata).orElse(7);
    }

    public static OptionalInt getOptionalDistanceAt(IBlockData iblockdata) {
        return iblockdata.is(TagsBlock.LOGS) ? OptionalInt.of(0) : (iblockdata.hasProperty(BlockLeaves.DISTANCE) ? OptionalInt.of((Integer) iblockdata.getValue(BlockLeaves.DISTANCE)) : OptionalInt.empty());
    }

    @Override
    protected Fluid getFluidState(IBlockData iblockdata) {
        return (Boolean) iblockdata.getValue(BlockLeaves.WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(iblockdata);
    }

    @Override
    public void animateTick(IBlockData iblockdata, World world, BlockPosition blockposition, RandomSource randomsource) {
        super.animateTick(iblockdata, world, blockposition, randomsource);
        BlockPosition blockposition1 = blockposition.below();
        IBlockData iblockdata1 = world.getBlockState(blockposition1);

        makeDrippingWaterParticles(world, blockposition, randomsource, iblockdata1, blockposition1);
        this.makeFallingLeavesParticles(world, blockposition, randomsource, iblockdata1, blockposition1);
    }

    private static void makeDrippingWaterParticles(World world, BlockPosition blockposition, RandomSource randomsource, IBlockData iblockdata, BlockPosition blockposition1) {
        if (world.isRainingAt(blockposition.above())) {
            if (randomsource.nextInt(15) == 1) {
                if (!iblockdata.canOcclude() || !iblockdata.isFaceSturdy(world, blockposition1, EnumDirection.UP)) {
                    ParticleUtils.spawnParticleBelow(world, blockposition, randomsource, Particles.DRIPPING_WATER);
                }
            }
        }
    }

    private void makeFallingLeavesParticles(World world, BlockPosition blockposition, RandomSource randomsource, IBlockData iblockdata, BlockPosition blockposition1) {
        if (randomsource.nextFloat() < this.leafParticleChance) {
            if (!isFaceFull(iblockdata.getCollisionShape(world, blockposition1), EnumDirection.UP)) {
                this.spawnFallingLeavesParticle(world, blockposition, randomsource);
            }
        }
    }

    protected abstract void spawnFallingLeavesParticle(World world, BlockPosition blockposition, RandomSource randomsource);

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockLeaves.DISTANCE, BlockLeaves.PERSISTENT, BlockLeaves.WATERLOGGED);
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        Fluid fluid = blockactioncontext.getLevel().getFluidState(blockactioncontext.getClickedPos());
        IBlockData iblockdata = (IBlockData) ((IBlockData) this.defaultBlockState().setValue(BlockLeaves.PERSISTENT, true)).setValue(BlockLeaves.WATERLOGGED, fluid.getType() == FluidTypes.WATER);

        return updateDistance(iblockdata, blockactioncontext.getLevel(), blockactioncontext.getClickedPos());
    }
}
