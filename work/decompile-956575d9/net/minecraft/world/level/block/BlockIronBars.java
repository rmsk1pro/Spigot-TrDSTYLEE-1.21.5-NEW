package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockIronBars extends BlockTall {

    public static final MapCodec<BlockIronBars> CODEC = simpleCodec(BlockIronBars::new);

    @Override
    public MapCodec<? extends BlockIronBars> codec() {
        return BlockIronBars.CODEC;
    }

    protected BlockIronBars(BlockBase.Info blockbase_info) {
        super(2.0F, 16.0F, 2.0F, 16.0F, 16.0F, blockbase_info);
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(BlockIronBars.NORTH, false)).setValue(BlockIronBars.EAST, false)).setValue(BlockIronBars.SOUTH, false)).setValue(BlockIronBars.WEST, false)).setValue(BlockIronBars.WATERLOGGED, false));
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        IBlockAccess iblockaccess = blockactioncontext.getLevel();
        BlockPosition blockposition = blockactioncontext.getClickedPos();
        Fluid fluid = blockactioncontext.getLevel().getFluidState(blockactioncontext.getClickedPos());
        BlockPosition blockposition1 = blockposition.north();
        BlockPosition blockposition2 = blockposition.south();
        BlockPosition blockposition3 = blockposition.west();
        BlockPosition blockposition4 = blockposition.east();
        IBlockData iblockdata = iblockaccess.getBlockState(blockposition1);
        IBlockData iblockdata1 = iblockaccess.getBlockState(blockposition2);
        IBlockData iblockdata2 = iblockaccess.getBlockState(blockposition3);
        IBlockData iblockdata3 = iblockaccess.getBlockState(blockposition4);

        return (IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) this.defaultBlockState().setValue(BlockIronBars.NORTH, this.attachsTo(iblockdata, iblockdata.isFaceSturdy(iblockaccess, blockposition1, EnumDirection.SOUTH)))).setValue(BlockIronBars.SOUTH, this.attachsTo(iblockdata1, iblockdata1.isFaceSturdy(iblockaccess, blockposition2, EnumDirection.NORTH)))).setValue(BlockIronBars.WEST, this.attachsTo(iblockdata2, iblockdata2.isFaceSturdy(iblockaccess, blockposition3, EnumDirection.EAST)))).setValue(BlockIronBars.EAST, this.attachsTo(iblockdata3, iblockdata3.isFaceSturdy(iblockaccess, blockposition4, EnumDirection.WEST)))).setValue(BlockIronBars.WATERLOGGED, fluid.getType() == FluidTypes.WATER);
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        if ((Boolean) iblockdata.getValue(BlockIronBars.WATERLOGGED)) {
            scheduledtickaccess.scheduleTick(blockposition, (FluidType) FluidTypes.WATER, FluidTypes.WATER.getTickDelay(iworldreader));
        }

        return enumdirection.getAxis().isHorizontal() ? (IBlockData) iblockdata.setValue((IBlockState) BlockIronBars.PROPERTY_BY_DIRECTION.get(enumdirection), this.attachsTo(iblockdata1, iblockdata1.isFaceSturdy(iworldreader, blockposition1, enumdirection.getOpposite()))) : super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
    }

    @Override
    protected VoxelShape getVisualShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return VoxelShapes.empty();
    }

    @Override
    protected boolean skipRendering(IBlockData iblockdata, IBlockData iblockdata1, EnumDirection enumdirection) {
        if (iblockdata1.is(this)) {
            if (!enumdirection.getAxis().isHorizontal()) {
                return true;
            }

            if ((Boolean) iblockdata.getValue((IBlockState) BlockIronBars.PROPERTY_BY_DIRECTION.get(enumdirection)) && (Boolean) iblockdata1.getValue((IBlockState) BlockIronBars.PROPERTY_BY_DIRECTION.get(enumdirection.getOpposite()))) {
                return true;
            }
        }

        return super.skipRendering(iblockdata, iblockdata1, enumdirection);
    }

    public final boolean attachsTo(IBlockData iblockdata, boolean flag) {
        return !isExceptionForConnection(iblockdata) && flag || iblockdata.getBlock() instanceof BlockIronBars || iblockdata.is(TagsBlock.WALLS);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockIronBars.NORTH, BlockIronBars.EAST, BlockIronBars.WEST, BlockIronBars.SOUTH, BlockIronBars.WATERLOGGED);
    }
}
