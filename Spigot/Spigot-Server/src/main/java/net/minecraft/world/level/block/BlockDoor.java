package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.util.MathHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
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
import net.minecraft.world.level.block.state.properties.BlockPropertyDoorHinge;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

import org.bukkit.event.block.BlockRedstoneEvent; // CraftBukkit

public class BlockDoor extends Block {

    public static final MapCodec<BlockDoor> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(BlockSetType.CODEC.fieldOf("block_set_type").forGetter(BlockDoor::type), propertiesCodec()).apply(instance, BlockDoor::new);
    });
    public static final BlockStateEnum<EnumDirection> FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateEnum<BlockPropertyDoubleBlockHalf> HALF = BlockProperties.DOUBLE_BLOCK_HALF;
    public static final BlockStateEnum<BlockPropertyDoorHinge> HINGE = BlockProperties.DOOR_HINGE;
    public static final BlockStateBoolean OPEN = BlockProperties.OPEN;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    private static final Map<EnumDirection, VoxelShape> SHAPES = VoxelShapes.rotateHorizontal(Block.boxZ(16.0D, 13.0D, 16.0D));
    private final BlockSetType type;

    @Override
    public MapCodec<? extends BlockDoor> codec() {
        return BlockDoor.CODEC;
    }

    protected BlockDoor(BlockSetType blocksettype, BlockBase.Info blockbase_info) {
        super(blockbase_info.sound(blocksettype.soundType()));
        this.type = blocksettype;
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(BlockDoor.FACING, EnumDirection.NORTH)).setValue(BlockDoor.OPEN, false)).setValue(BlockDoor.HINGE, BlockPropertyDoorHinge.LEFT)).setValue(BlockDoor.POWERED, false)).setValue(BlockDoor.HALF, BlockPropertyDoubleBlockHalf.LOWER));
    }

    public BlockSetType type() {
        return this.type;
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        EnumDirection enumdirection = (EnumDirection) iblockdata.getValue(BlockDoor.FACING);
        EnumDirection enumdirection1 = (Boolean) iblockdata.getValue(BlockDoor.OPEN) ? (iblockdata.getValue(BlockDoor.HINGE) == BlockPropertyDoorHinge.RIGHT ? enumdirection.getCounterClockWise() : enumdirection.getClockWise()) : enumdirection;

        return (VoxelShape) BlockDoor.SHAPES.get(enumdirection1);
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        BlockPropertyDoubleBlockHalf blockpropertydoubleblockhalf = (BlockPropertyDoubleBlockHalf) iblockdata.getValue(BlockDoor.HALF);

        return enumdirection.getAxis() == EnumDirection.EnumAxis.Y && blockpropertydoubleblockhalf == BlockPropertyDoubleBlockHalf.LOWER == (enumdirection == EnumDirection.UP) ? (iblockdata1.getBlock() instanceof BlockDoor && iblockdata1.getValue(BlockDoor.HALF) != blockpropertydoubleblockhalf ? (IBlockData) iblockdata1.setValue(BlockDoor.HALF, blockpropertydoubleblockhalf) : Blocks.AIR.defaultBlockState()) : (blockpropertydoubleblockhalf == BlockPropertyDoubleBlockHalf.LOWER && enumdirection == EnumDirection.DOWN && !iblockdata.canSurvive(iworldreader, blockposition) ? Blocks.AIR.defaultBlockState() : super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource));
    }

    @Override
    protected void onExplosionHit(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Explosion explosion, BiConsumer<ItemStack, BlockPosition> biconsumer) {
        if (explosion.canTriggerBlocks() && iblockdata.getValue(BlockDoor.HALF) == BlockPropertyDoubleBlockHalf.LOWER && this.type.canOpenByWindCharge() && !(Boolean) iblockdata.getValue(BlockDoor.POWERED)) {
            this.setOpen((Entity) null, worldserver, iblockdata, blockposition, !this.isOpen(iblockdata));
        }

        super.onExplosionHit(iblockdata, worldserver, blockposition, explosion, biconsumer);
    }

    @Override
    public IBlockData playerWillDestroy(World world, BlockPosition blockposition, IBlockData iblockdata, EntityHuman entityhuman) {
        if (!world.isClientSide && (entityhuman.preventsBlockDrops() || !entityhuman.hasCorrectToolForDrops(iblockdata))) {
            BlockTallPlant.preventDropFromBottomPart(world, blockposition, iblockdata, entityhuman);
        }

        return super.playerWillDestroy(world, blockposition, iblockdata, entityhuman);
    }

    @Override
    protected boolean isPathfindable(IBlockData iblockdata, PathMode pathmode) {
        boolean flag;

        switch (pathmode) {
            case LAND:
            case AIR:
                flag = (Boolean) iblockdata.getValue(BlockDoor.OPEN);
                break;
            case WATER:
                flag = false;
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }

        return flag;
    }

    @Nullable
    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        BlockPosition blockposition = blockactioncontext.getClickedPos();
        World world = blockactioncontext.getLevel();

        if (blockposition.getY() < world.getMaxY() && world.getBlockState(blockposition.above()).canBeReplaced(blockactioncontext)) {
            boolean flag = world.hasNeighborSignal(blockposition) || world.hasNeighborSignal(blockposition.above());

            return (IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) this.defaultBlockState().setValue(BlockDoor.FACING, blockactioncontext.getHorizontalDirection())).setValue(BlockDoor.HINGE, this.getHinge(blockactioncontext))).setValue(BlockDoor.POWERED, flag)).setValue(BlockDoor.OPEN, flag)).setValue(BlockDoor.HALF, BlockPropertyDoubleBlockHalf.LOWER);
        } else {
            return null;
        }
    }

    @Override
    public void setPlacedBy(World world, BlockPosition blockposition, IBlockData iblockdata, EntityLiving entityliving, ItemStack itemstack) {
        world.setBlock(blockposition.above(), (IBlockData) iblockdata.setValue(BlockDoor.HALF, BlockPropertyDoubleBlockHalf.UPPER), 3);
    }

    private BlockPropertyDoorHinge getHinge(BlockActionContext blockactioncontext) {
        IBlockAccess iblockaccess = blockactioncontext.getLevel();
        BlockPosition blockposition = blockactioncontext.getClickedPos();
        EnumDirection enumdirection = blockactioncontext.getHorizontalDirection();
        BlockPosition blockposition1 = blockposition.above();
        EnumDirection enumdirection1 = enumdirection.getCounterClockWise();
        BlockPosition blockposition2 = blockposition.relative(enumdirection1);
        IBlockData iblockdata = iblockaccess.getBlockState(blockposition2);
        BlockPosition blockposition3 = blockposition1.relative(enumdirection1);
        IBlockData iblockdata1 = iblockaccess.getBlockState(blockposition3);
        EnumDirection enumdirection2 = enumdirection.getClockWise();
        BlockPosition blockposition4 = blockposition.relative(enumdirection2);
        IBlockData iblockdata2 = iblockaccess.getBlockState(blockposition4);
        BlockPosition blockposition5 = blockposition1.relative(enumdirection2);
        IBlockData iblockdata3 = iblockaccess.getBlockState(blockposition5);
        int i = (iblockdata.isCollisionShapeFullBlock(iblockaccess, blockposition2) ? -1 : 0) + (iblockdata1.isCollisionShapeFullBlock(iblockaccess, blockposition3) ? -1 : 0) + (iblockdata2.isCollisionShapeFullBlock(iblockaccess, blockposition4) ? 1 : 0) + (iblockdata3.isCollisionShapeFullBlock(iblockaccess, blockposition5) ? 1 : 0);
        boolean flag = iblockdata.getBlock() instanceof BlockDoor && iblockdata.getValue(BlockDoor.HALF) == BlockPropertyDoubleBlockHalf.LOWER;
        boolean flag1 = iblockdata2.getBlock() instanceof BlockDoor && iblockdata2.getValue(BlockDoor.HALF) == BlockPropertyDoubleBlockHalf.LOWER;

        if ((!flag || flag1) && i <= 0) {
            if ((!flag1 || flag) && i >= 0) {
                int j = enumdirection.getStepX();
                int k = enumdirection.getStepZ();
                Vec3D vec3d = blockactioncontext.getClickLocation();
                double d0 = vec3d.x - (double) blockposition.getX();
                double d1 = vec3d.z - (double) blockposition.getZ();

                return (j >= 0 || d1 >= 0.5D) && (j <= 0 || d1 <= 0.5D) && (k >= 0 || d0 <= 0.5D) && (k <= 0 || d0 >= 0.5D) ? BlockPropertyDoorHinge.LEFT : BlockPropertyDoorHinge.RIGHT;
            } else {
                return BlockPropertyDoorHinge.LEFT;
            }
        } else {
            return BlockPropertyDoorHinge.RIGHT;
        }
    }

    @Override
    protected EnumInteractionResult useWithoutItem(IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, MovingObjectPositionBlock movingobjectpositionblock) {
        if (!this.type.canOpenByHand()) {
            return EnumInteractionResult.PASS;
        } else {
            iblockdata = (IBlockData) iblockdata.cycle(BlockDoor.OPEN);
            world.setBlock(blockposition, iblockdata, 10);
            this.playSound(entityhuman, world, blockposition, (Boolean) iblockdata.getValue(BlockDoor.OPEN));
            world.gameEvent(entityhuman, (Holder) (this.isOpen(iblockdata) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE), blockposition);
            return EnumInteractionResult.SUCCESS;
        }
    }

    public boolean isOpen(IBlockData iblockdata) {
        return (Boolean) iblockdata.getValue(BlockDoor.OPEN);
    }

    public void setOpen(@Nullable Entity entity, World world, IBlockData iblockdata, BlockPosition blockposition, boolean flag) {
        if (iblockdata.is(this) && (Boolean) iblockdata.getValue(BlockDoor.OPEN) != flag) {
            world.setBlock(blockposition, (IBlockData) iblockdata.setValue(BlockDoor.OPEN, flag), 10);
            this.playSound(entity, world, blockposition, flag);
            world.gameEvent(entity, (Holder) (flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE), blockposition);
        }
    }

    @Override
    protected void neighborChanged(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, @Nullable Orientation orientation, boolean flag) {
        // CraftBukkit start
        BlockPosition otherHalf = blockposition.relative(iblockdata.getValue(BlockDoor.HALF) == BlockPropertyDoubleBlockHalf.LOWER ? EnumDirection.UP : EnumDirection.DOWN);

        org.bukkit.World bworld = world.getWorld();
        org.bukkit.block.Block bukkitBlock = bworld.getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
        org.bukkit.block.Block blockTop = bworld.getBlockAt(otherHalf.getX(), otherHalf.getY(), otherHalf.getZ());

        int power = bukkitBlock.getBlockPower();
        int powerTop = blockTop.getBlockPower();
        if (powerTop > power) power = powerTop;
        int oldPower = (Boolean) iblockdata.getValue(BlockDoor.POWERED) ? 15 : 0;

        if (oldPower == 0 ^ power == 0) {
            BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(bukkitBlock, oldPower, power);
            world.getCraftServer().getPluginManager().callEvent(eventRedstone);

            boolean flag1 = eventRedstone.getNewCurrent() > 0;
            // CraftBukkit end
            if (flag1 != (Boolean) iblockdata.getValue(BlockDoor.OPEN)) {
                this.playSound((Entity) null, world, blockposition, flag1);
                world.gameEvent((Entity) null, (Holder) (flag1 ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE), blockposition);
            }

            world.setBlock(blockposition, (IBlockData) ((IBlockData) iblockdata.setValue(BlockDoor.POWERED, flag1)).setValue(BlockDoor.OPEN, flag1), 2);
        }

    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        BlockPosition blockposition1 = blockposition.below();
        IBlockData iblockdata1 = iworldreader.getBlockState(blockposition1);

        return iblockdata.getValue(BlockDoor.HALF) == BlockPropertyDoubleBlockHalf.LOWER ? iblockdata1.isFaceSturdy(iworldreader, blockposition1, EnumDirection.UP) : iblockdata1.is(this);
    }

    private void playSound(@Nullable Entity entity, World world, BlockPosition blockposition, boolean flag) {
        world.playSound(entity, blockposition, flag ? this.type.doorOpen() : this.type.doorClose(), SoundCategory.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.1F + 0.9F);
    }

    @Override
    protected IBlockData rotate(IBlockData iblockdata, EnumBlockRotation enumblockrotation) {
        return (IBlockData) iblockdata.setValue(BlockDoor.FACING, enumblockrotation.rotate((EnumDirection) iblockdata.getValue(BlockDoor.FACING)));
    }

    @Override
    protected IBlockData mirror(IBlockData iblockdata, EnumBlockMirror enumblockmirror) {
        return enumblockmirror == EnumBlockMirror.NONE ? iblockdata : (IBlockData) iblockdata.rotate(enumblockmirror.getRotation((EnumDirection) iblockdata.getValue(BlockDoor.FACING))).cycle(BlockDoor.HINGE);
    }

    @Override
    protected long getSeed(IBlockData iblockdata, BlockPosition blockposition) {
        return MathHelper.getSeed(blockposition.getX(), blockposition.below(iblockdata.getValue(BlockDoor.HALF) == BlockPropertyDoubleBlockHalf.LOWER ? 0 : 1).getY(), blockposition.getZ());
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockDoor.HALF, BlockDoor.FACING, BlockDoor.OPEN, BlockDoor.HINGE, BlockDoor.POWERED);
    }

    public static boolean isWoodenDoor(World world, BlockPosition blockposition) {
        return isWoodenDoor(world.getBlockState(blockposition));
    }

    public static boolean isWoodenDoor(IBlockData iblockdata) {
        Block block = iblockdata.getBlock();
        boolean flag;

        if (block instanceof BlockDoor blockdoor) {
            if (blockdoor.type().canOpenByHand()) {
                flag = true;
                return flag;
            }
        }

        flag = false;
        return flag;
    }
}
