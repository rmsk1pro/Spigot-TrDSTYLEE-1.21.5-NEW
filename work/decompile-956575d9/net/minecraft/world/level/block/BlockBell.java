package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBell;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyBellAttach;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockBell extends BlockTileEntity {

    public static final MapCodec<BlockBell> CODEC = simpleCodec(BlockBell::new);
    public static final BlockStateEnum<EnumDirection> FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateEnum<BlockPropertyBellAttach> ATTACHMENT = BlockProperties.BELL_ATTACHMENT;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    private static final VoxelShape BELL_SHAPE = VoxelShapes.or(Block.column(6.0D, 6.0D, 13.0D), Block.column(8.0D, 4.0D, 6.0D));
    private static final VoxelShape SHAPE_CEILING = VoxelShapes.or(BlockBell.BELL_SHAPE, Block.column(2.0D, 13.0D, 16.0D));
    private static final Map<EnumDirection.EnumAxis, VoxelShape> SHAPE_FLOOR = VoxelShapes.rotateHorizontalAxis(Block.cube(16.0D, 16.0D, 8.0D));
    private static final Map<EnumDirection.EnumAxis, VoxelShape> SHAPE_DOUBLE_WALL = VoxelShapes.rotateHorizontalAxis(VoxelShapes.or(BlockBell.BELL_SHAPE, Block.column(2.0D, 16.0D, 13.0D, 15.0D)));
    private static final Map<EnumDirection, VoxelShape> SHAPE_SINGLE_WALL = VoxelShapes.rotateHorizontal(VoxelShapes.or(BlockBell.BELL_SHAPE, Block.boxZ(2.0D, 13.0D, 15.0D, 0.0D, 13.0D)));
    public static final int EVENT_BELL_RING = 1;

    @Override
    public MapCodec<BlockBell> codec() {
        return BlockBell.CODEC;
    }

    public BlockBell(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(BlockBell.FACING, EnumDirection.NORTH)).setValue(BlockBell.ATTACHMENT, BlockPropertyBellAttach.FLOOR)).setValue(BlockBell.POWERED, false));
    }

    @Override
    protected void neighborChanged(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, @Nullable Orientation orientation, boolean flag) {
        boolean flag1 = world.hasNeighborSignal(blockposition);

        if (flag1 != (Boolean) iblockdata.getValue(BlockBell.POWERED)) {
            if (flag1) {
                this.attemptToRing(world, blockposition, (EnumDirection) null);
            }

            world.setBlock(blockposition, (IBlockData) iblockdata.setValue(BlockBell.POWERED, flag1), 3);
        }

    }

    @Override
    protected void onProjectileHit(World world, IBlockData iblockdata, MovingObjectPositionBlock movingobjectpositionblock, IProjectile iprojectile) {
        Entity entity = iprojectile.getOwner();
        EntityHuman entityhuman;

        if (entity instanceof EntityHuman entityhuman1) {
            entityhuman = entityhuman1;
        } else {
            entityhuman = null;
        }

        EntityHuman entityhuman2 = entityhuman;

        this.onHit(world, iblockdata, movingobjectpositionblock, entityhuman2, true);
    }

    @Override
    protected EnumInteractionResult useWithoutItem(IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, MovingObjectPositionBlock movingobjectpositionblock) {
        return (EnumInteractionResult) (this.onHit(world, iblockdata, movingobjectpositionblock, entityhuman, true) ? EnumInteractionResult.SUCCESS : EnumInteractionResult.PASS);
    }

    public boolean onHit(World world, IBlockData iblockdata, MovingObjectPositionBlock movingobjectpositionblock, @Nullable EntityHuman entityhuman, boolean flag) {
        EnumDirection enumdirection = movingobjectpositionblock.getDirection();
        BlockPosition blockposition = movingobjectpositionblock.getBlockPos();
        boolean flag1 = !flag || this.isProperHit(iblockdata, enumdirection, movingobjectpositionblock.getLocation().y - (double) blockposition.getY());

        if (flag1) {
            boolean flag2 = this.attemptToRing(entityhuman, world, blockposition, enumdirection);

            if (flag2 && entityhuman != null) {
                entityhuman.awardStat(StatisticList.BELL_RING);
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean isProperHit(IBlockData iblockdata, EnumDirection enumdirection, double d0) {
        if (enumdirection.getAxis() != EnumDirection.EnumAxis.Y && d0 <= (double) 0.8124F) {
            EnumDirection enumdirection1 = (EnumDirection) iblockdata.getValue(BlockBell.FACING);
            BlockPropertyBellAttach blockpropertybellattach = (BlockPropertyBellAttach) iblockdata.getValue(BlockBell.ATTACHMENT);

            switch (blockpropertybellattach) {
                case FLOOR:
                    return enumdirection1.getAxis() == enumdirection.getAxis();
                case SINGLE_WALL:
                case DOUBLE_WALL:
                    return enumdirection1.getAxis() != enumdirection.getAxis();
                case CEILING:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    public boolean attemptToRing(World world, BlockPosition blockposition, @Nullable EnumDirection enumdirection) {
        return this.attemptToRing((Entity) null, world, blockposition, enumdirection);
    }

    public boolean attemptToRing(@Nullable Entity entity, World world, BlockPosition blockposition, @Nullable EnumDirection enumdirection) {
        TileEntity tileentity = world.getBlockEntity(blockposition);

        if (!world.isClientSide && tileentity instanceof TileEntityBell) {
            if (enumdirection == null) {
                enumdirection = (EnumDirection) world.getBlockState(blockposition).getValue(BlockBell.FACING);
            }

            ((TileEntityBell) tileentity).onHit(enumdirection);
            world.playSound((Entity) null, blockposition, SoundEffects.BELL_BLOCK, SoundCategory.BLOCKS, 2.0F, 1.0F);
            world.gameEvent(entity, (Holder) GameEvent.BLOCK_CHANGE, blockposition);
            return true;
        } else {
            return false;
        }
    }

    private VoxelShape getVoxelShape(IBlockData iblockdata) {
        EnumDirection enumdirection = (EnumDirection) iblockdata.getValue(BlockBell.FACING);
        VoxelShape voxelshape;

        switch ((BlockPropertyBellAttach) iblockdata.getValue(BlockBell.ATTACHMENT)) {
            case FLOOR:
                voxelshape = (VoxelShape) BlockBell.SHAPE_FLOOR.get(enumdirection.getAxis());
                break;
            case SINGLE_WALL:
                voxelshape = (VoxelShape) BlockBell.SHAPE_SINGLE_WALL.get(enumdirection);
                break;
            case DOUBLE_WALL:
                voxelshape = (VoxelShape) BlockBell.SHAPE_DOUBLE_WALL.get(enumdirection.getAxis());
                break;
            case CEILING:
                voxelshape = BlockBell.SHAPE_CEILING;
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }

        return voxelshape;
    }

    @Override
    protected VoxelShape getCollisionShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return this.getVoxelShape(iblockdata);
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return this.getVoxelShape(iblockdata);
    }

    @Nullable
    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        EnumDirection enumdirection = blockactioncontext.getClickedFace();
        BlockPosition blockposition = blockactioncontext.getClickedPos();
        World world = blockactioncontext.getLevel();
        EnumDirection.EnumAxis enumdirection_enumaxis = enumdirection.getAxis();

        if (enumdirection_enumaxis == EnumDirection.EnumAxis.Y) {
            IBlockData iblockdata = (IBlockData) ((IBlockData) this.defaultBlockState().setValue(BlockBell.ATTACHMENT, enumdirection == EnumDirection.DOWN ? BlockPropertyBellAttach.CEILING : BlockPropertyBellAttach.FLOOR)).setValue(BlockBell.FACING, blockactioncontext.getHorizontalDirection());

            if (iblockdata.canSurvive(blockactioncontext.getLevel(), blockposition)) {
                return iblockdata;
            }
        } else {
            boolean flag = enumdirection_enumaxis == EnumDirection.EnumAxis.X && world.getBlockState(blockposition.west()).isFaceSturdy(world, blockposition.west(), EnumDirection.EAST) && world.getBlockState(blockposition.east()).isFaceSturdy(world, blockposition.east(), EnumDirection.WEST) || enumdirection_enumaxis == EnumDirection.EnumAxis.Z && world.getBlockState(blockposition.north()).isFaceSturdy(world, blockposition.north(), EnumDirection.SOUTH) && world.getBlockState(blockposition.south()).isFaceSturdy(world, blockposition.south(), EnumDirection.NORTH);
            IBlockData iblockdata1 = (IBlockData) ((IBlockData) this.defaultBlockState().setValue(BlockBell.FACING, enumdirection.getOpposite())).setValue(BlockBell.ATTACHMENT, flag ? BlockPropertyBellAttach.DOUBLE_WALL : BlockPropertyBellAttach.SINGLE_WALL);

            if (iblockdata1.canSurvive(blockactioncontext.getLevel(), blockactioncontext.getClickedPos())) {
                return iblockdata1;
            }

            boolean flag1 = world.getBlockState(blockposition.below()).isFaceSturdy(world, blockposition.below(), EnumDirection.UP);

            iblockdata1 = (IBlockData) iblockdata1.setValue(BlockBell.ATTACHMENT, flag1 ? BlockPropertyBellAttach.FLOOR : BlockPropertyBellAttach.CEILING);
            if (iblockdata1.canSurvive(blockactioncontext.getLevel(), blockactioncontext.getClickedPos())) {
                return iblockdata1;
            }
        }

        return null;
    }

    @Override
    protected void onExplosionHit(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Explosion explosion, BiConsumer<ItemStack, BlockPosition> biconsumer) {
        if (explosion.canTriggerBlocks()) {
            this.attemptToRing(worldserver, blockposition, (EnumDirection) null);
        }

        super.onExplosionHit(iblockdata, worldserver, blockposition, explosion, biconsumer);
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        BlockPropertyBellAttach blockpropertybellattach = (BlockPropertyBellAttach) iblockdata.getValue(BlockBell.ATTACHMENT);
        EnumDirection enumdirection1 = getConnectedDirection(iblockdata).getOpposite();

        if (enumdirection1 == enumdirection && !iblockdata.canSurvive(iworldreader, blockposition) && blockpropertybellattach != BlockPropertyBellAttach.DOUBLE_WALL) {
            return Blocks.AIR.defaultBlockState();
        } else {
            if (enumdirection.getAxis() == ((EnumDirection) iblockdata.getValue(BlockBell.FACING)).getAxis()) {
                if (blockpropertybellattach == BlockPropertyBellAttach.DOUBLE_WALL && !iblockdata1.isFaceSturdy(iworldreader, blockposition1, enumdirection)) {
                    return (IBlockData) ((IBlockData) iblockdata.setValue(BlockBell.ATTACHMENT, BlockPropertyBellAttach.SINGLE_WALL)).setValue(BlockBell.FACING, enumdirection.getOpposite());
                }

                if (blockpropertybellattach == BlockPropertyBellAttach.SINGLE_WALL && enumdirection1.getOpposite() == enumdirection && iblockdata1.isFaceSturdy(iworldreader, blockposition1, (EnumDirection) iblockdata.getValue(BlockBell.FACING))) {
                    return (IBlockData) iblockdata.setValue(BlockBell.ATTACHMENT, BlockPropertyBellAttach.DOUBLE_WALL);
                }
            }

            return super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
        }
    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        EnumDirection enumdirection = getConnectedDirection(iblockdata).getOpposite();

        return enumdirection == EnumDirection.UP ? Block.canSupportCenter(iworldreader, blockposition.above(), EnumDirection.DOWN) : BlockAttachable.canAttach(iworldreader, blockposition, enumdirection);
    }

    private static EnumDirection getConnectedDirection(IBlockData iblockdata) {
        switch ((BlockPropertyBellAttach) iblockdata.getValue(BlockBell.ATTACHMENT)) {
            case FLOOR:
                return EnumDirection.UP;
            case CEILING:
                return EnumDirection.DOWN;
            default:
                return ((EnumDirection) iblockdata.getValue(BlockBell.FACING)).getOpposite();
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockBell.FACING, BlockBell.ATTACHMENT, BlockBell.POWERED);
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(BlockPosition blockposition, IBlockData iblockdata) {
        return new TileEntityBell(blockposition, iblockdata);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData iblockdata, TileEntityTypes<T> tileentitytypes) {
        return createTickerHelper(tileentitytypes, TileEntityTypes.BELL, world.isClientSide ? TileEntityBell::clientTick : TileEntityBell::serverTick);
    }

    @Override
    protected boolean isPathfindable(IBlockData iblockdata, PathMode pathmode) {
        return false;
    }

    @Override
    public IBlockData rotate(IBlockData iblockdata, EnumBlockRotation enumblockrotation) {
        return (IBlockData) iblockdata.setValue(BlockBell.FACING, enumblockrotation.rotate((EnumDirection) iblockdata.getValue(BlockBell.FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData iblockdata, EnumBlockMirror enumblockmirror) {
        return iblockdata.rotate(enumblockmirror.getRotation((EnumDirection) iblockdata.getValue(BlockBell.FACING)));
    }
}
