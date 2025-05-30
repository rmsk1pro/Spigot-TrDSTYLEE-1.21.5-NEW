package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public abstract class BlockCoralBase extends Block implements IBlockWaterlogged {

    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(12.0D, 0.0D, 4.0D);

    protected BlockCoralBase(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) (this.stateDefinition.any()).setValue(BlockCoralBase.WATERLOGGED, true));
    }

    @Override
    protected abstract MapCodec<? extends BlockCoralBase> codec();

    protected void tryScheduleDieTick(IBlockData iblockdata, IBlockAccess iblockaccess, ScheduledTickAccess scheduledtickaccess, RandomSource randomsource, BlockPosition blockposition) {
        if (!scanForWater(iblockdata, iblockaccess, blockposition)) {
            scheduledtickaccess.scheduleTick(blockposition, (Block) this, 60 + randomsource.nextInt(40));
        }

    }

    protected static boolean scanForWater(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        if ((Boolean) iblockdata.getValue(BlockCoralBase.WATERLOGGED)) {
            return true;
        } else {
            for (EnumDirection enumdirection : EnumDirection.values()) {
                if (iblockaccess.getFluidState(blockposition.relative(enumdirection)).is(TagsFluid.WATER)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Nullable
    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        Fluid fluid = blockactioncontext.getLevel().getFluidState(blockactioncontext.getClickedPos());

        return (IBlockData) this.defaultBlockState().setValue(BlockCoralBase.WATERLOGGED, fluid.is(TagsFluid.WATER) && fluid.getAmount() == 8);
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockCoralBase.SHAPE;
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        if ((Boolean) iblockdata.getValue(BlockCoralBase.WATERLOGGED)) {
            scheduledtickaccess.scheduleTick(blockposition, (FluidType) FluidTypes.WATER, FluidTypes.WATER.getTickDelay(iworldreader));
        }

        return enumdirection == EnumDirection.DOWN && !this.canSurvive(iblockdata, iworldreader, blockposition) ? Blocks.AIR.defaultBlockState() : super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        BlockPosition blockposition1 = blockposition.below();

        return iworldreader.getBlockState(blockposition1).isFaceSturdy(iworldreader, blockposition1, EnumDirection.UP);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockCoralBase.WATERLOGGED);
    }

    @Override
    protected Fluid getFluidState(IBlockData iblockdata) {
        return (Boolean) iblockdata.getValue(BlockCoralBase.WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(iblockdata);
    }
}
