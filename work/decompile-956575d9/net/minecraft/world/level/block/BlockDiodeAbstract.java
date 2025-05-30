package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.ticks.TickListPriority;

public abstract class BlockDiodeAbstract extends BlockFacingHorizontal {

    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    private static final VoxelShape SHAPE = Block.column(16.0D, 0.0D, 2.0D);

    protected BlockDiodeAbstract(BlockBase.Info blockbase_info) {
        super(blockbase_info);
    }

    @Override
    protected abstract MapCodec<? extends BlockDiodeAbstract> codec();

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockDiodeAbstract.SHAPE;
    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        BlockPosition blockposition1 = blockposition.below();

        return this.canSurviveOn(iworldreader, blockposition1, iworldreader.getBlockState(blockposition1));
    }

    protected boolean canSurviveOn(IWorldReader iworldreader, BlockPosition blockposition, IBlockData iblockdata) {
        return iblockdata.isFaceSturdy(iworldreader, blockposition, EnumDirection.UP, EnumBlockSupport.RIGID);
    }

    @Override
    protected void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        if (!this.isLocked(worldserver, blockposition, iblockdata)) {
            boolean flag = (Boolean) iblockdata.getValue(BlockDiodeAbstract.POWERED);
            boolean flag1 = this.shouldTurnOn(worldserver, blockposition, iblockdata);

            if (flag && !flag1) {
                worldserver.setBlock(blockposition, (IBlockData) iblockdata.setValue(BlockDiodeAbstract.POWERED, false), 2);
            } else if (!flag) {
                worldserver.setBlock(blockposition, (IBlockData) iblockdata.setValue(BlockDiodeAbstract.POWERED, true), 2);
                if (!flag1) {
                    worldserver.scheduleTick(blockposition, (Block) this, this.getDelay(iblockdata), TickListPriority.VERY_HIGH);
                }
            }

        }
    }

    @Override
    protected int getDirectSignal(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        return iblockdata.getSignal(iblockaccess, blockposition, enumdirection);
    }

    @Override
    protected int getSignal(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        return !(Boolean) iblockdata.getValue(BlockDiodeAbstract.POWERED) ? 0 : (iblockdata.getValue(BlockDiodeAbstract.FACING) == enumdirection ? this.getOutputSignal(iblockaccess, blockposition, iblockdata) : 0);
    }

    @Override
    protected void neighborChanged(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, @Nullable Orientation orientation, boolean flag) {
        if (iblockdata.canSurvive(world, blockposition)) {
            this.checkTickOnNeighbor(world, blockposition, iblockdata);
        } else {
            TileEntity tileentity = iblockdata.hasBlockEntity() ? world.getBlockEntity(blockposition) : null;

            dropResources(iblockdata, world, blockposition, tileentity);
            world.removeBlock(blockposition, false);

            for (EnumDirection enumdirection : EnumDirection.values()) {
                world.updateNeighborsAt(blockposition.relative(enumdirection), this);
            }

        }
    }

    protected void checkTickOnNeighbor(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if (!this.isLocked(world, blockposition, iblockdata)) {
            boolean flag = (Boolean) iblockdata.getValue(BlockDiodeAbstract.POWERED);
            boolean flag1 = this.shouldTurnOn(world, blockposition, iblockdata);

            if (flag != flag1 && !world.getBlockTicks().willTickThisTick(blockposition, this)) {
                TickListPriority ticklistpriority = TickListPriority.HIGH;

                if (this.shouldPrioritize(world, blockposition, iblockdata)) {
                    ticklistpriority = TickListPriority.EXTREMELY_HIGH;
                } else if (flag) {
                    ticklistpriority = TickListPriority.VERY_HIGH;
                }

                world.scheduleTick(blockposition, (Block) this, this.getDelay(iblockdata), ticklistpriority);
            }

        }
    }

    public boolean isLocked(IWorldReader iworldreader, BlockPosition blockposition, IBlockData iblockdata) {
        return false;
    }

    protected boolean shouldTurnOn(World world, BlockPosition blockposition, IBlockData iblockdata) {
        return this.getInputSignal(world, blockposition, iblockdata) > 0;
    }

    protected int getInputSignal(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection enumdirection = (EnumDirection) iblockdata.getValue(BlockDiodeAbstract.FACING);
        BlockPosition blockposition1 = blockposition.relative(enumdirection);
        int i = world.getSignal(blockposition1, enumdirection);

        if (i >= 15) {
            return i;
        } else {
            IBlockData iblockdata1 = world.getBlockState(blockposition1);

            return Math.max(i, iblockdata1.is(Blocks.REDSTONE_WIRE) ? (Integer) iblockdata1.getValue(BlockRedstoneWire.POWER) : 0);
        }
    }

    protected int getAlternateSignal(SignalGetter signalgetter, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection enumdirection = (EnumDirection) iblockdata.getValue(BlockDiodeAbstract.FACING);
        EnumDirection enumdirection1 = enumdirection.getClockWise();
        EnumDirection enumdirection2 = enumdirection.getCounterClockWise();
        boolean flag = this.sideInputDiodesOnly();

        return Math.max(signalgetter.getControlInputSignal(blockposition.relative(enumdirection1), enumdirection1, flag), signalgetter.getControlInputSignal(blockposition.relative(enumdirection2), enumdirection2, flag));
    }

    @Override
    protected boolean isSignalSource(IBlockData iblockdata) {
        return true;
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        return (IBlockData) this.defaultBlockState().setValue(BlockDiodeAbstract.FACING, blockactioncontext.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(World world, BlockPosition blockposition, IBlockData iblockdata, EntityLiving entityliving, ItemStack itemstack) {
        if (this.shouldTurnOn(world, blockposition, iblockdata)) {
            world.scheduleTick(blockposition, (Block) this, 1);
        }

    }

    @Override
    protected void onPlace(IBlockData iblockdata, World world, BlockPosition blockposition, IBlockData iblockdata1, boolean flag) {
        this.updateNeighborsInFront(world, blockposition, iblockdata);
    }

    @Override
    protected void affectNeighborsAfterRemoval(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, boolean flag) {
        if (!flag) {
            this.updateNeighborsInFront(worldserver, blockposition, iblockdata);
        }

    }

    protected void updateNeighborsInFront(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection enumdirection = (EnumDirection) iblockdata.getValue(BlockDiodeAbstract.FACING);
        BlockPosition blockposition1 = blockposition.relative(enumdirection.getOpposite());
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(world, enumdirection.getOpposite(), EnumDirection.UP);

        world.neighborChanged(blockposition1, this, orientation);
        world.updateNeighborsAtExceptFromFacing(blockposition1, this, enumdirection, orientation);
    }

    protected boolean sideInputDiodesOnly() {
        return false;
    }

    protected int getOutputSignal(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata) {
        return 15;
    }

    public static boolean isDiode(IBlockData iblockdata) {
        return iblockdata.getBlock() instanceof BlockDiodeAbstract;
    }

    public boolean shouldPrioritize(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection enumdirection = ((EnumDirection) iblockdata.getValue(BlockDiodeAbstract.FACING)).getOpposite();
        IBlockData iblockdata1 = iblockaccess.getBlockState(blockposition.relative(enumdirection));

        return isDiode(iblockdata1) && iblockdata1.getValue(BlockDiodeAbstract.FACING) != enumdirection;
    }

    protected abstract int getDelay(IBlockData iblockdata);
}
