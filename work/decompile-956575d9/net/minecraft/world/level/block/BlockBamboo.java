package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyBambooSize;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockBamboo extends Block implements IBlockFragilePlantElement {

    public static final MapCodec<BlockBamboo> CODEC = simpleCodec(BlockBamboo::new);
    private static final VoxelShape SHAPE_SMALL = Block.column(6.0D, 0.0D, 16.0D);
    private static final VoxelShape SHAPE_LARGE = Block.column(10.0D, 0.0D, 16.0D);
    private static final VoxelShape SHAPE_COLLISION = Block.column(3.0D, 0.0D, 16.0D);
    public static final BlockStateInteger AGE = BlockProperties.AGE_1;
    public static final BlockStateEnum<BlockPropertyBambooSize> LEAVES = BlockProperties.BAMBOO_LEAVES;
    public static final BlockStateInteger STAGE = BlockProperties.STAGE;
    public static final int MAX_HEIGHT = 16;
    public static final int STAGE_GROWING = 0;
    public static final int STAGE_DONE_GROWING = 1;
    public static final int AGE_THIN_BAMBOO = 0;
    public static final int AGE_THICK_BAMBOO = 1;

    @Override
    public MapCodec<BlockBamboo> codec() {
        return BlockBamboo.CODEC;
    }

    public BlockBamboo(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(BlockBamboo.AGE, 0)).setValue(BlockBamboo.LEAVES, BlockPropertyBambooSize.NONE)).setValue(BlockBamboo.STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockBamboo.AGE, BlockBamboo.LEAVES, BlockBamboo.STAGE);
    }

    @Override
    protected boolean propagatesSkylightDown(IBlockData iblockdata) {
        return true;
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        VoxelShape voxelshape = iblockdata.getValue(BlockBamboo.LEAVES) == BlockPropertyBambooSize.LARGE ? BlockBamboo.SHAPE_LARGE : BlockBamboo.SHAPE_SMALL;

        return voxelshape.move(iblockdata.getOffset(blockposition));
    }

    @Override
    protected boolean isPathfindable(IBlockData iblockdata, PathMode pathmode) {
        return false;
    }

    @Override
    protected VoxelShape getCollisionShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockBamboo.SHAPE_COLLISION.move(iblockdata.getOffset(blockposition));
    }

    @Override
    protected boolean isCollisionShapeFullBlock(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return false;
    }

    @Nullable
    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        Fluid fluid = blockactioncontext.getLevel().getFluidState(blockactioncontext.getClickedPos());

        if (!fluid.isEmpty()) {
            return null;
        } else {
            IBlockData iblockdata = blockactioncontext.getLevel().getBlockState(blockactioncontext.getClickedPos().below());

            if (iblockdata.is(TagsBlock.BAMBOO_PLANTABLE_ON)) {
                if (iblockdata.is(Blocks.BAMBOO_SAPLING)) {
                    return (IBlockData) this.defaultBlockState().setValue(BlockBamboo.AGE, 0);
                } else if (iblockdata.is(Blocks.BAMBOO)) {
                    int i = (Integer) iblockdata.getValue(BlockBamboo.AGE) > 0 ? 1 : 0;

                    return (IBlockData) this.defaultBlockState().setValue(BlockBamboo.AGE, i);
                } else {
                    IBlockData iblockdata1 = blockactioncontext.getLevel().getBlockState(blockactioncontext.getClickedPos().above());

                    return iblockdata1.is(Blocks.BAMBOO) ? (IBlockData) this.defaultBlockState().setValue(BlockBamboo.AGE, (Integer) iblockdata1.getValue(BlockBamboo.AGE)) : Blocks.BAMBOO_SAPLING.defaultBlockState();
                }
            } else {
                return null;
            }
        }
    }

    @Override
    protected void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        if (!iblockdata.canSurvive(worldserver, blockposition)) {
            worldserver.destroyBlock(blockposition, true);
        }

    }

    @Override
    protected boolean isRandomlyTicking(IBlockData iblockdata) {
        return (Integer) iblockdata.getValue(BlockBamboo.STAGE) == 0;
    }

    @Override
    protected void randomTick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        if ((Integer) iblockdata.getValue(BlockBamboo.STAGE) == 0) {
            if (randomsource.nextInt(3) == 0 && worldserver.isEmptyBlock(blockposition.above()) && worldserver.getRawBrightness(blockposition.above(), 0) >= 9) {
                int i = this.getHeightBelowUpToMax(worldserver, blockposition) + 1;

                if (i < 16) {
                    this.growBamboo(iblockdata, worldserver, blockposition, randomsource, i);
                }
            }

        }
    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        return iworldreader.getBlockState(blockposition.below()).is(TagsBlock.BAMBOO_PLANTABLE_ON);
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        if (!iblockdata.canSurvive(iworldreader, blockposition)) {
            scheduledtickaccess.scheduleTick(blockposition, (Block) this, 1);
        }

        return enumdirection == EnumDirection.UP && iblockdata1.is(Blocks.BAMBOO) && (Integer) iblockdata1.getValue(BlockBamboo.AGE) > (Integer) iblockdata.getValue(BlockBamboo.AGE) ? (IBlockData) iblockdata.cycle(BlockBamboo.AGE) : super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
    }

    @Override
    public boolean isValidBonemealTarget(IWorldReader iworldreader, BlockPosition blockposition, IBlockData iblockdata) {
        int i = this.getHeightAboveUpToMax(iworldreader, blockposition);
        int j = this.getHeightBelowUpToMax(iworldreader, blockposition);

        return i + j + 1 < 16 && (Integer) iworldreader.getBlockState(blockposition.above(i)).getValue(BlockBamboo.STAGE) != 1;
    }

    @Override
    public boolean isBonemealSuccess(World world, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer worldserver, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        int i = this.getHeightAboveUpToMax(worldserver, blockposition);
        int j = this.getHeightBelowUpToMax(worldserver, blockposition);
        int k = i + j + 1;
        int l = 1 + randomsource.nextInt(2);

        for (int i1 = 0; i1 < l; ++i1) {
            BlockPosition blockposition1 = blockposition.above(i);
            IBlockData iblockdata1 = worldserver.getBlockState(blockposition1);

            if (k >= 16 || (Integer) iblockdata1.getValue(BlockBamboo.STAGE) == 1 || !worldserver.isEmptyBlock(blockposition1.above())) {
                return;
            }

            this.growBamboo(iblockdata1, worldserver, blockposition1, randomsource, k);
            ++i;
            ++k;
        }

    }

    protected void growBamboo(IBlockData iblockdata, World world, BlockPosition blockposition, RandomSource randomsource, int i) {
        IBlockData iblockdata1 = world.getBlockState(blockposition.below());
        BlockPosition blockposition1 = blockposition.below(2);
        IBlockData iblockdata2 = world.getBlockState(blockposition1);
        BlockPropertyBambooSize blockpropertybamboosize = BlockPropertyBambooSize.NONE;

        if (i >= 1) {
            if (iblockdata1.is(Blocks.BAMBOO) && iblockdata1.getValue(BlockBamboo.LEAVES) != BlockPropertyBambooSize.NONE) {
                if (iblockdata1.is(Blocks.BAMBOO) && iblockdata1.getValue(BlockBamboo.LEAVES) != BlockPropertyBambooSize.NONE) {
                    blockpropertybamboosize = BlockPropertyBambooSize.LARGE;
                    if (iblockdata2.is(Blocks.BAMBOO)) {
                        world.setBlock(blockposition.below(), (IBlockData) iblockdata1.setValue(BlockBamboo.LEAVES, BlockPropertyBambooSize.SMALL), 3);
                        world.setBlock(blockposition1, (IBlockData) iblockdata2.setValue(BlockBamboo.LEAVES, BlockPropertyBambooSize.NONE), 3);
                    }
                }
            } else {
                blockpropertybamboosize = BlockPropertyBambooSize.SMALL;
            }
        }

        int j = (Integer) iblockdata.getValue(BlockBamboo.AGE) != 1 && !iblockdata2.is(Blocks.BAMBOO) ? 0 : 1;
        int k = (i < 11 || randomsource.nextFloat() >= 0.25F) && i != 15 ? 0 : 1;

        world.setBlock(blockposition.above(), (IBlockData) ((IBlockData) ((IBlockData) this.defaultBlockState().setValue(BlockBamboo.AGE, j)).setValue(BlockBamboo.LEAVES, blockpropertybamboosize)).setValue(BlockBamboo.STAGE, k), 3);
    }

    protected int getHeightAboveUpToMax(IBlockAccess iblockaccess, BlockPosition blockposition) {
        int i;

        for (i = 0; i < 16 && iblockaccess.getBlockState(blockposition.above(i + 1)).is(Blocks.BAMBOO); ++i) {
            ;
        }

        return i;
    }

    protected int getHeightBelowUpToMax(IBlockAccess iblockaccess, BlockPosition blockposition) {
        int i;

        for (i = 0; i < 16 && iblockaccess.getBlockState(blockposition.below(i + 1)).is(Blocks.BAMBOO); ++i) {
            ;
        }

        return i;
    }
}
