package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;

// CraftBukkit start
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
// CraftBukkit end

public class LightningRodBlock extends RodBlock implements IBlockWaterlogged {

    public static final MapCodec<LightningRodBlock> CODEC = simpleCodec(LightningRodBlock::new);
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    private static final int ACTIVATION_TICKS = 8;
    public static final int RANGE = 128;
    private static final int SPARK_CYCLE = 200;

    @Override
    public MapCodec<LightningRodBlock> codec() {
        return LightningRodBlock.CODEC;
    }

    public LightningRodBlock(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(LightningRodBlock.FACING, EnumDirection.UP)).setValue(LightningRodBlock.WATERLOGGED, false)).setValue(LightningRodBlock.POWERED, false));
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        Fluid fluid = blockactioncontext.getLevel().getFluidState(blockactioncontext.getClickedPos());
        boolean flag = fluid.getType() == FluidTypes.WATER;

        return (IBlockData) ((IBlockData) this.defaultBlockState().setValue(LightningRodBlock.FACING, blockactioncontext.getClickedFace())).setValue(LightningRodBlock.WATERLOGGED, flag);
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        if ((Boolean) iblockdata.getValue(LightningRodBlock.WATERLOGGED)) {
            scheduledtickaccess.scheduleTick(blockposition, (FluidType) FluidTypes.WATER, FluidTypes.WATER.getTickDelay(iworldreader));
        }

        return super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
    }

    @Override
    protected Fluid getFluidState(IBlockData iblockdata) {
        return (Boolean) iblockdata.getValue(LightningRodBlock.WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(iblockdata);
    }

    @Override
    protected int getSignal(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        return (Boolean) iblockdata.getValue(LightningRodBlock.POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        return (Boolean) iblockdata.getValue(LightningRodBlock.POWERED) && iblockdata.getValue(LightningRodBlock.FACING) == enumdirection ? 15 : 0;
    }

    public void onLightningStrike(IBlockData iblockdata, World world, BlockPosition blockposition) {
        // CraftBukkit start
        boolean powered = iblockdata.getValue(LightningRodBlock.POWERED);
        int old = (powered) ? 15 : 0;
        int current = (!powered) ? 15 : 0;

        BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(CraftBlock.at(world, blockposition), old, current);
        world.getCraftServer().getPluginManager().callEvent(eventRedstone);

        if (eventRedstone.getNewCurrent() <= 0) {
            return;
        }
        // CraftBukkit end
        world.setBlock(blockposition, (IBlockData) iblockdata.setValue(LightningRodBlock.POWERED, true), 3);
        this.updateNeighbours(iblockdata, world, blockposition);
        world.scheduleTick(blockposition, (Block) this, 8);
        world.levelEvent(3002, blockposition, ((EnumDirection) iblockdata.getValue(LightningRodBlock.FACING)).getAxis().ordinal());
    }

    private void updateNeighbours(IBlockData iblockdata, World world, BlockPosition blockposition) {
        EnumDirection enumdirection = ((EnumDirection) iblockdata.getValue(LightningRodBlock.FACING)).getOpposite();

        world.updateNeighborsAt(blockposition.relative(enumdirection), this, ExperimentalRedstoneUtils.initialOrientation(world, enumdirection, (EnumDirection) null));
    }

    @Override
    protected void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        worldserver.setBlock(blockposition, (IBlockData) iblockdata.setValue(LightningRodBlock.POWERED, false), 3);
        this.updateNeighbours(iblockdata, worldserver, blockposition);
    }

    @Override
    public void animateTick(IBlockData iblockdata, World world, BlockPosition blockposition, RandomSource randomsource) {
        if (world.isThundering() && (long) world.random.nextInt(200) <= world.getGameTime() % 200L && blockposition.getY() == world.getHeight(HeightMap.Type.WORLD_SURFACE, blockposition.getX(), blockposition.getZ()) - 1) {
            ParticleUtils.spawnParticlesAlongAxis(((EnumDirection) iblockdata.getValue(LightningRodBlock.FACING)).getAxis(), world, blockposition, 0.125D, Particles.ELECTRIC_SPARK, UniformInt.of(1, 2));
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, boolean flag) {
        if ((Boolean) iblockdata.getValue(LightningRodBlock.POWERED)) {
            this.updateNeighbours(iblockdata, worldserver, blockposition);
        }

    }

    @Override
    protected void onPlace(IBlockData iblockdata, World world, BlockPosition blockposition, IBlockData iblockdata1, boolean flag) {
        if (!iblockdata.is(iblockdata1.getBlock())) {
            if ((Boolean) iblockdata.getValue(LightningRodBlock.POWERED) && !world.getBlockTicks().hasScheduledTick(blockposition, this)) {
                world.setBlock(blockposition, (IBlockData) iblockdata.setValue(LightningRodBlock.POWERED, false), 18);
            }

        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(LightningRodBlock.FACING, LightningRodBlock.POWERED, LightningRodBlock.WATERLOGGED);
    }

    @Override
    protected boolean isSignalSource(IBlockData iblockdata) {
        return true;
    }
}
