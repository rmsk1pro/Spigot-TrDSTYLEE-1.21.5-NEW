package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyWood;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockFenceGate extends BlockFacingHorizontal {

    public static final MapCodec<BlockFenceGate> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(BlockPropertyWood.CODEC.fieldOf("wood_type").forGetter((blockfencegate) -> {
            return blockfencegate.type;
        }), propertiesCodec()).apply(instance, BlockFenceGate::new);
    });
    public static final BlockStateBoolean OPEN = BlockProperties.OPEN;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    public static final BlockStateBoolean IN_WALL = BlockProperties.IN_WALL;
    private static final Map<EnumDirection.EnumAxis, VoxelShape> SHAPES = VoxelShapes.rotateHorizontalAxis(Block.cube(16.0D, 16.0D, 4.0D));
    private static final Map<EnumDirection.EnumAxis, VoxelShape> SHAPES_WALL = Maps.newEnumMap(SystemUtils.mapValues(BlockFenceGate.SHAPES, (voxelshape) -> {
        return VoxelShapes.join(voxelshape, Block.column(16.0D, 13.0D, 16.0D), OperatorBoolean.ONLY_FIRST);
    }));
    private static final Map<EnumDirection.EnumAxis, VoxelShape> SHAPE_COLLISION = VoxelShapes.rotateHorizontalAxis(Block.column(16.0D, 4.0D, 0.0D, 24.0D));
    private static final Map<EnumDirection.EnumAxis, VoxelShape> SHAPE_SUPPORT = VoxelShapes.rotateHorizontalAxis(Block.column(16.0D, 4.0D, 5.0D, 24.0D));
    private static final Map<EnumDirection.EnumAxis, VoxelShape> SHAPE_OCCLUSION = VoxelShapes.rotateHorizontalAxis(VoxelShapes.or(Block.box(0.0D, 5.0D, 7.0D, 2.0D, 16.0D, 9.0D), Block.box(14.0D, 5.0D, 7.0D, 16.0D, 16.0D, 9.0D)));
    private static final Map<EnumDirection.EnumAxis, VoxelShape> SHAPE_OCCLUSION_WALL = Maps.newEnumMap(SystemUtils.mapValues(BlockFenceGate.SHAPE_OCCLUSION, (voxelshape) -> {
        return voxelshape.move(0.0D, -0.1875D, 0.0D).optimize();
    }));
    private final BlockPropertyWood type;

    @Override
    public MapCodec<BlockFenceGate> codec() {
        return BlockFenceGate.CODEC;
    }

    public BlockFenceGate(BlockPropertyWood blockpropertywood, BlockBase.Info blockbase_info) {
        super(blockbase_info.sound(blockpropertywood.soundType()));
        this.type = blockpropertywood;
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(BlockFenceGate.OPEN, false)).setValue(BlockFenceGate.POWERED, false)).setValue(BlockFenceGate.IN_WALL, false));
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        EnumDirection.EnumAxis enumdirection_enumaxis = ((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getAxis();

        return (VoxelShape) ((Boolean) iblockdata.getValue(BlockFenceGate.IN_WALL) ? BlockFenceGate.SHAPES_WALL : BlockFenceGate.SHAPES).get(enumdirection_enumaxis);
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        EnumDirection.EnumAxis enumdirection_enumaxis = enumdirection.getAxis();

        if (((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getClockWise().getAxis() != enumdirection_enumaxis) {
            return super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
        } else {
            boolean flag = this.isWall(iblockdata1) || this.isWall(iworldreader.getBlockState(blockposition.relative(enumdirection.getOpposite())));

            return (IBlockData) iblockdata.setValue(BlockFenceGate.IN_WALL, flag);
        }
    }

    @Override
    protected VoxelShape getBlockSupportShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        EnumDirection.EnumAxis enumdirection_enumaxis = ((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getAxis();

        return (Boolean) iblockdata.getValue(BlockFenceGate.OPEN) ? VoxelShapes.empty() : (VoxelShape) BlockFenceGate.SHAPE_SUPPORT.get(enumdirection_enumaxis);
    }

    @Override
    protected VoxelShape getCollisionShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        EnumDirection.EnumAxis enumdirection_enumaxis = ((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getAxis();

        return (Boolean) iblockdata.getValue(BlockFenceGate.OPEN) ? VoxelShapes.empty() : (VoxelShape) BlockFenceGate.SHAPE_COLLISION.get(enumdirection_enumaxis);
    }

    @Override
    protected VoxelShape getOcclusionShape(IBlockData iblockdata) {
        EnumDirection.EnumAxis enumdirection_enumaxis = ((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getAxis();

        return (VoxelShape) ((Boolean) iblockdata.getValue(BlockFenceGate.IN_WALL) ? BlockFenceGate.SHAPE_OCCLUSION_WALL : BlockFenceGate.SHAPE_OCCLUSION).get(enumdirection_enumaxis);
    }

    @Override
    protected boolean isPathfindable(IBlockData iblockdata, PathMode pathmode) {
        switch (pathmode) {
            case LAND:
                return (Boolean) iblockdata.getValue(BlockFenceGate.OPEN);
            case WATER:
                return false;
            case AIR:
                return (Boolean) iblockdata.getValue(BlockFenceGate.OPEN);
            default:
                return false;
        }
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        World world = blockactioncontext.getLevel();
        BlockPosition blockposition = blockactioncontext.getClickedPos();
        boolean flag = world.hasNeighborSignal(blockposition);
        EnumDirection enumdirection = blockactioncontext.getHorizontalDirection();
        EnumDirection.EnumAxis enumdirection_enumaxis = enumdirection.getAxis();
        boolean flag1 = enumdirection_enumaxis == EnumDirection.EnumAxis.Z && (this.isWall(world.getBlockState(blockposition.west())) || this.isWall(world.getBlockState(blockposition.east()))) || enumdirection_enumaxis == EnumDirection.EnumAxis.X && (this.isWall(world.getBlockState(blockposition.north())) || this.isWall(world.getBlockState(blockposition.south())));

        return (IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) this.defaultBlockState().setValue(BlockFenceGate.FACING, enumdirection)).setValue(BlockFenceGate.OPEN, flag)).setValue(BlockFenceGate.POWERED, flag)).setValue(BlockFenceGate.IN_WALL, flag1);
    }

    private boolean isWall(IBlockData iblockdata) {
        return iblockdata.is(TagsBlock.WALLS);
    }

    @Override
    protected EnumInteractionResult useWithoutItem(IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, MovingObjectPositionBlock movingobjectpositionblock) {
        if ((Boolean) iblockdata.getValue(BlockFenceGate.OPEN)) {
            iblockdata = (IBlockData) iblockdata.setValue(BlockFenceGate.OPEN, false);
            world.setBlock(blockposition, iblockdata, 10);
        } else {
            EnumDirection enumdirection = entityhuman.getDirection();

            if (iblockdata.getValue(BlockFenceGate.FACING) == enumdirection.getOpposite()) {
                iblockdata = (IBlockData) iblockdata.setValue(BlockFenceGate.FACING, enumdirection);
            }

            iblockdata = (IBlockData) iblockdata.setValue(BlockFenceGate.OPEN, true);
            world.setBlock(blockposition, iblockdata, 10);
        }

        boolean flag = (Boolean) iblockdata.getValue(BlockFenceGate.OPEN);

        world.playSound(entityhuman, blockposition, flag ? this.type.fenceGateOpen() : this.type.fenceGateClose(), SoundCategory.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.1F + 0.9F);
        world.gameEvent(entityhuman, (Holder) (flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE), blockposition);
        return EnumInteractionResult.SUCCESS;
    }

    @Override
    protected void onExplosionHit(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Explosion explosion, BiConsumer<ItemStack, BlockPosition> biconsumer) {
        if (explosion.canTriggerBlocks() && !(Boolean) iblockdata.getValue(BlockFenceGate.POWERED)) {
            boolean flag = (Boolean) iblockdata.getValue(BlockFenceGate.OPEN);

            worldserver.setBlockAndUpdate(blockposition, (IBlockData) iblockdata.setValue(BlockFenceGate.OPEN, !flag));
            worldserver.playSound((Entity) null, blockposition, flag ? this.type.fenceGateClose() : this.type.fenceGateOpen(), SoundCategory.BLOCKS, 1.0F, worldserver.getRandom().nextFloat() * 0.1F + 0.9F);
            worldserver.gameEvent(flag ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, blockposition, GameEvent.a.of(iblockdata));
        }

        super.onExplosionHit(iblockdata, worldserver, blockposition, explosion, biconsumer);
    }

    @Override
    protected void neighborChanged(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, @Nullable Orientation orientation, boolean flag) {
        if (!world.isClientSide) {
            boolean flag1 = world.hasNeighborSignal(blockposition);

            if ((Boolean) iblockdata.getValue(BlockFenceGate.POWERED) != flag1) {
                world.setBlock(blockposition, (IBlockData) ((IBlockData) iblockdata.setValue(BlockFenceGate.POWERED, flag1)).setValue(BlockFenceGate.OPEN, flag1), 2);
                if ((Boolean) iblockdata.getValue(BlockFenceGate.OPEN) != flag1) {
                    world.playSound((Entity) null, blockposition, flag1 ? this.type.fenceGateOpen() : this.type.fenceGateClose(), SoundCategory.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.1F + 0.9F);
                    world.gameEvent((Entity) null, (Holder) (flag1 ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE), blockposition);
                }
            }

        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockFenceGate.FACING, BlockFenceGate.OPEN, BlockFenceGate.POWERED, BlockFenceGate.IN_WALL);
    }

    public static boolean connectsToDirection(IBlockData iblockdata, EnumDirection enumdirection) {
        return ((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getAxis() == enumdirection.getClockWise().getAxis();
    }
}
