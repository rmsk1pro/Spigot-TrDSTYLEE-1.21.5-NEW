package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BigDripleafBlock extends BlockFacingHorizontal implements IBlockFragilePlantElement, IBlockWaterlogged {

    public static final MapCodec<BigDripleafBlock> CODEC = simpleCodec(BigDripleafBlock::new);
    private static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    private static final BlockStateEnum<Tilt> TILT = BlockProperties.TILT;
    private static final int NO_TICK = -1;
    private static final Object2IntMap<Tilt> DELAY_UNTIL_NEXT_TILT_STATE = (Object2IntMap) SystemUtils.make(new Object2IntArrayMap(), (object2intarraymap) -> {
        object2intarraymap.defaultReturnValue(-1);
        object2intarraymap.put(Tilt.UNSTABLE, 10);
        object2intarraymap.put(Tilt.PARTIAL, 10);
        object2intarraymap.put(Tilt.FULL, 100);
    });
    private static final int MAX_GEN_HEIGHT = 5;
    private static final int ENTITY_DETECTION_MIN_Y = 11;
    private static final int LOWEST_LEAF_TOP = 13;
    private static final Map<Tilt, VoxelShape> SHAPE_LEAF = Maps.newEnumMap(Map.of(Tilt.NONE, Block.column(16.0D, 11.0D, 15.0D), Tilt.UNSTABLE, Block.column(16.0D, 11.0D, 15.0D), Tilt.PARTIAL, Block.column(16.0D, 11.0D, 13.0D), Tilt.FULL, VoxelShapes.empty()));
    private final Function<IBlockData, VoxelShape> shapes;

    @Override
    public MapCodec<BigDripleafBlock> codec() {
        return BigDripleafBlock.CODEC;
    }

    protected BigDripleafBlock(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(BigDripleafBlock.WATERLOGGED, false)).setValue(BigDripleafBlock.FACING, EnumDirection.NORTH)).setValue(BigDripleafBlock.TILT, Tilt.NONE));
        this.shapes = this.makeShapes();
    }

    private Function<IBlockData, VoxelShape> makeShapes() {
        Map<EnumDirection, VoxelShape> map = VoxelShapes.rotateHorizontal(Block.column(6.0D, 0.0D, 13.0D).move(0.0D, 0.0D, 0.25D).optimize());

        return this.getShapeForEachState((iblockdata) -> {
            return VoxelShapes.or((VoxelShape) BigDripleafBlock.SHAPE_LEAF.get(iblockdata.getValue(BigDripleafBlock.TILT)), (VoxelShape) map.get(iblockdata.getValue(BigDripleafBlock.FACING)));
        }, new IBlockState[]{BigDripleafBlock.WATERLOGGED});
    }

    public static void placeWithRandomHeight(GeneratorAccess generatoraccess, RandomSource randomsource, BlockPosition blockposition, EnumDirection enumdirection) {
        int i = MathHelper.nextInt(randomsource, 2, 5);
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = blockposition.mutable();
        int j = 0;

        while (j < i && canPlaceAt(generatoraccess, blockposition_mutableblockposition, generatoraccess.getBlockState(blockposition_mutableblockposition))) {
            ++j;
            blockposition_mutableblockposition.move(EnumDirection.UP);
        }

        int k = blockposition.getY() + j - 1;

        blockposition_mutableblockposition.setY(blockposition.getY());

        while (blockposition_mutableblockposition.getY() < k) {
            BigDripleafStemBlock.place(generatoraccess, blockposition_mutableblockposition, generatoraccess.getFluidState(blockposition_mutableblockposition), enumdirection);
            blockposition_mutableblockposition.move(EnumDirection.UP);
        }

        place(generatoraccess, blockposition_mutableblockposition, generatoraccess.getFluidState(blockposition_mutableblockposition), enumdirection);
    }

    private static boolean canReplace(IBlockData iblockdata) {
        return iblockdata.isAir() || iblockdata.is(Blocks.WATER) || iblockdata.is(Blocks.SMALL_DRIPLEAF);
    }

    protected static boolean canPlaceAt(LevelHeightAccessor levelheightaccessor, BlockPosition blockposition, IBlockData iblockdata) {
        return !levelheightaccessor.isOutsideBuildHeight(blockposition) && canReplace(iblockdata);
    }

    protected static boolean place(GeneratorAccess generatoraccess, BlockPosition blockposition, Fluid fluid, EnumDirection enumdirection) {
        IBlockData iblockdata = (IBlockData) ((IBlockData) Blocks.BIG_DRIPLEAF.defaultBlockState().setValue(BigDripleafBlock.WATERLOGGED, fluid.isSourceOfType(FluidTypes.WATER))).setValue(BigDripleafBlock.FACING, enumdirection);

        return generatoraccess.setBlock(blockposition, iblockdata, 3);
    }

    @Override
    protected void onProjectileHit(World world, IBlockData iblockdata, MovingObjectPositionBlock movingobjectpositionblock, IProjectile iprojectile) {
        this.setTiltAndScheduleTick(iblockdata, world, movingobjectpositionblock.getBlockPos(), Tilt.FULL, SoundEffects.BIG_DRIPLEAF_TILT_DOWN);
    }

    @Override
    protected Fluid getFluidState(IBlockData iblockdata) {
        return (Boolean) iblockdata.getValue(BigDripleafBlock.WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(iblockdata);
    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        BlockPosition blockposition1 = blockposition.below();
        IBlockData iblockdata1 = iworldreader.getBlockState(blockposition1);

        return iblockdata1.is(this) || iblockdata1.is(Blocks.BIG_DRIPLEAF_STEM) || iblockdata1.is(TagsBlock.BIG_DRIPLEAF_PLACEABLE);
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        if (enumdirection == EnumDirection.DOWN && !iblockdata.canSurvive(iworldreader, blockposition)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            if ((Boolean) iblockdata.getValue(BigDripleafBlock.WATERLOGGED)) {
                scheduledtickaccess.scheduleTick(blockposition, (FluidType) FluidTypes.WATER, FluidTypes.WATER.getTickDelay(iworldreader));
            }

            return enumdirection == EnumDirection.UP && iblockdata1.is(this) ? Blocks.BIG_DRIPLEAF_STEM.withPropertiesOf(iblockdata) : super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
        }
    }

    @Override
    public boolean isValidBonemealTarget(IWorldReader iworldreader, BlockPosition blockposition, IBlockData iblockdata) {
        IBlockData iblockdata1 = iworldreader.getBlockState(blockposition.above());

        return canReplace(iblockdata1);
    }

    @Override
    public boolean isBonemealSuccess(World world, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer worldserver, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        BlockPosition blockposition1 = blockposition.above();
        IBlockData iblockdata1 = worldserver.getBlockState(blockposition1);

        if (canPlaceAt(worldserver, blockposition1, iblockdata1)) {
            EnumDirection enumdirection = (EnumDirection) iblockdata.getValue(BigDripleafBlock.FACING);

            BigDripleafStemBlock.place(worldserver, blockposition, iblockdata.getFluidState(), enumdirection);
            place(worldserver, blockposition1, iblockdata1.getFluidState(), enumdirection);
        }

    }

    @Override
    protected void entityInside(IBlockData iblockdata, World world, BlockPosition blockposition, Entity entity, InsideBlockEffectApplier insideblockeffectapplier) {
        if (!world.isClientSide) {
            if (iblockdata.getValue(BigDripleafBlock.TILT) == Tilt.NONE && canEntityTilt(blockposition, entity) && !world.hasNeighborSignal(blockposition)) {
                this.setTiltAndScheduleTick(iblockdata, world, blockposition, Tilt.UNSTABLE, (SoundEffect) null);
            }

        }
    }

    @Override
    protected void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        if (worldserver.hasNeighborSignal(blockposition)) {
            resetTilt(iblockdata, worldserver, blockposition);
        } else {
            Tilt tilt = (Tilt) iblockdata.getValue(BigDripleafBlock.TILT);

            if (tilt == Tilt.UNSTABLE) {
                this.setTiltAndScheduleTick(iblockdata, worldserver, blockposition, Tilt.PARTIAL, SoundEffects.BIG_DRIPLEAF_TILT_DOWN);
            } else if (tilt == Tilt.PARTIAL) {
                this.setTiltAndScheduleTick(iblockdata, worldserver, blockposition, Tilt.FULL, SoundEffects.BIG_DRIPLEAF_TILT_DOWN);
            } else if (tilt == Tilt.FULL) {
                resetTilt(iblockdata, worldserver, blockposition);
            }

        }
    }

    @Override
    protected void neighborChanged(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, @Nullable Orientation orientation, boolean flag) {
        if (world.hasNeighborSignal(blockposition)) {
            resetTilt(iblockdata, world, blockposition);
        }

    }

    private static void playTiltSound(World world, BlockPosition blockposition, SoundEffect soundeffect) {
        float f = MathHelper.randomBetween(world.random, 0.8F, 1.2F);

        world.playSound((Entity) null, blockposition, soundeffect, SoundCategory.BLOCKS, 1.0F, f);
    }

    private static boolean canEntityTilt(BlockPosition blockposition, Entity entity) {
        return entity.onGround() && entity.position().y > (double) ((float) blockposition.getY() + 0.6875F);
    }

    private void setTiltAndScheduleTick(IBlockData iblockdata, World world, BlockPosition blockposition, Tilt tilt, @Nullable SoundEffect soundeffect) {
        setTilt(iblockdata, world, blockposition, tilt);
        if (soundeffect != null) {
            playTiltSound(world, blockposition, soundeffect);
        }

        int i = BigDripleafBlock.DELAY_UNTIL_NEXT_TILT_STATE.getInt(tilt);

        if (i != -1) {
            world.scheduleTick(blockposition, (Block) this, i);
        }

    }

    private static void resetTilt(IBlockData iblockdata, World world, BlockPosition blockposition) {
        setTilt(iblockdata, world, blockposition, Tilt.NONE);
        if (iblockdata.getValue(BigDripleafBlock.TILT) != Tilt.NONE) {
            playTiltSound(world, blockposition, SoundEffects.BIG_DRIPLEAF_TILT_UP);
        }

    }

    private static void setTilt(IBlockData iblockdata, World world, BlockPosition blockposition, Tilt tilt) {
        Tilt tilt1 = (Tilt) iblockdata.getValue(BigDripleafBlock.TILT);

        world.setBlock(blockposition, (IBlockData) iblockdata.setValue(BigDripleafBlock.TILT, tilt), 2);
        if (tilt.causesVibration() && tilt != tilt1) {
            world.gameEvent((Entity) null, (Holder) GameEvent.BLOCK_CHANGE, blockposition);
        }

    }

    @Override
    protected VoxelShape getCollisionShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return (VoxelShape) BigDripleafBlock.SHAPE_LEAF.get(iblockdata.getValue(BigDripleafBlock.TILT));
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return (VoxelShape) this.shapes.apply(iblockdata);
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        IBlockData iblockdata = blockactioncontext.getLevel().getBlockState(blockactioncontext.getClickedPos().below());
        Fluid fluid = blockactioncontext.getLevel().getFluidState(blockactioncontext.getClickedPos());
        boolean flag = iblockdata.is(Blocks.BIG_DRIPLEAF) || iblockdata.is(Blocks.BIG_DRIPLEAF_STEM);

        return (IBlockData) ((IBlockData) this.defaultBlockState().setValue(BigDripleafBlock.WATERLOGGED, fluid.isSourceOfType(FluidTypes.WATER))).setValue(BigDripleafBlock.FACING, flag ? (EnumDirection) iblockdata.getValue(BigDripleafBlock.FACING) : blockactioncontext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BigDripleafBlock.WATERLOGGED, BigDripleafBlock.FACING, BigDripleafBlock.TILT);
    }
}
