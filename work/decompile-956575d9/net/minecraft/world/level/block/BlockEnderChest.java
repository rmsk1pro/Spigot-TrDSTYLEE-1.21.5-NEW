package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.monster.piglin.PiglinAI;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.InventoryEnderChest;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.entity.TileEntityEnderChest;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockEnderChest extends BlockChestAbstract<TileEntityEnderChest> implements IBlockWaterlogged {

    public static final MapCodec<BlockEnderChest> CODEC = simpleCodec(BlockEnderChest::new);
    public static final BlockStateEnum<EnumDirection> FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(14.0D, 0.0D, 14.0D);
    private static final IChatBaseComponent CONTAINER_TITLE = IChatBaseComponent.translatable("container.enderchest");

    @Override
    public MapCodec<BlockEnderChest> codec() {
        return BlockEnderChest.CODEC;
    }

    protected BlockEnderChest(BlockBase.Info blockbase_info) {
        super(blockbase_info, () -> {
            return TileEntityTypes.ENDER_CHEST;
        });
        this.registerDefaultState((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(BlockEnderChest.FACING, EnumDirection.NORTH)).setValue(BlockEnderChest.WATERLOGGED, false));
    }

    @Override
    public DoubleBlockFinder.Result<? extends TileEntityChest> combine(IBlockData iblockdata, World world, BlockPosition blockposition, boolean flag) {
        return DoubleBlockFinder.Combiner::acceptNone;
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockEnderChest.SHAPE;
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        Fluid fluid = blockactioncontext.getLevel().getFluidState(blockactioncontext.getClickedPos());

        return (IBlockData) ((IBlockData) this.defaultBlockState().setValue(BlockEnderChest.FACING, blockactioncontext.getHorizontalDirection().getOpposite())).setValue(BlockEnderChest.WATERLOGGED, fluid.getType() == FluidTypes.WATER);
    }

    @Override
    protected EnumInteractionResult useWithoutItem(IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, MovingObjectPositionBlock movingobjectpositionblock) {
        InventoryEnderChest inventoryenderchest = entityhuman.getEnderChestInventory();
        TileEntity tileentity = world.getBlockEntity(blockposition);

        if (inventoryenderchest != null && tileentity instanceof TileEntityEnderChest tileentityenderchest) {
            BlockPosition blockposition1 = blockposition.above();

            if (world.getBlockState(blockposition1).isRedstoneConductor(world, blockposition1)) {
                return EnumInteractionResult.SUCCESS;
            } else {
                if (world instanceof WorldServer) {
                    WorldServer worldserver = (WorldServer) world;

                    inventoryenderchest.setActiveChest(tileentityenderchest);
                    entityhuman.openMenu(new TileInventory((i, playerinventory, entityhuman1) -> {
                        return ContainerChest.threeRows(i, playerinventory, inventoryenderchest);
                    }, BlockEnderChest.CONTAINER_TITLE));
                    entityhuman.awardStat(StatisticList.OPEN_ENDERCHEST);
                    PiglinAI.angerNearbyPiglins(worldserver, entityhuman, true);
                }

                return EnumInteractionResult.SUCCESS;
            }
        } else {
            return EnumInteractionResult.SUCCESS;
        }
    }

    @Override
    public TileEntity newBlockEntity(BlockPosition blockposition, IBlockData iblockdata) {
        return new TileEntityEnderChest(blockposition, iblockdata);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData iblockdata, TileEntityTypes<T> tileentitytypes) {
        return world.isClientSide ? createTickerHelper(tileentitytypes, TileEntityTypes.ENDER_CHEST, TileEntityEnderChest::lidAnimateTick) : null;
    }

    @Override
    public void animateTick(IBlockData iblockdata, World world, BlockPosition blockposition, RandomSource randomsource) {
        for (int i = 0; i < 3; ++i) {
            int j = randomsource.nextInt(2) * 2 - 1;
            int k = randomsource.nextInt(2) * 2 - 1;
            double d0 = (double) blockposition.getX() + 0.5D + 0.25D * (double) j;
            double d1 = (double) ((float) blockposition.getY() + randomsource.nextFloat());
            double d2 = (double) blockposition.getZ() + 0.5D + 0.25D * (double) k;
            double d3 = (double) (randomsource.nextFloat() * (float) j);
            double d4 = ((double) randomsource.nextFloat() - 0.5D) * 0.125D;
            double d5 = (double) (randomsource.nextFloat() * (float) k);

            world.addParticle(Particles.PORTAL, d0, d1, d2, d3, d4, d5);
        }

    }

    @Override
    protected IBlockData rotate(IBlockData iblockdata, EnumBlockRotation enumblockrotation) {
        return (IBlockData) iblockdata.setValue(BlockEnderChest.FACING, enumblockrotation.rotate((EnumDirection) iblockdata.getValue(BlockEnderChest.FACING)));
    }

    @Override
    protected IBlockData mirror(IBlockData iblockdata, EnumBlockMirror enumblockmirror) {
        return iblockdata.rotate(enumblockmirror.getRotation((EnumDirection) iblockdata.getValue(BlockEnderChest.FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockEnderChest.FACING, BlockEnderChest.WATERLOGGED);
    }

    @Override
    protected Fluid getFluidState(IBlockData iblockdata) {
        return (Boolean) iblockdata.getValue(BlockEnderChest.WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(iblockdata);
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        if ((Boolean) iblockdata.getValue(BlockEnderChest.WATERLOGGED)) {
            scheduledtickaccess.scheduleTick(blockposition, (FluidType) FluidTypes.WATER, FluidTypes.WATER.getTickDelay(iworldreader));
        }

        return super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
    }

    @Override
    protected boolean isPathfindable(IBlockData iblockdata, PathMode pathmode) {
        return false;
    }

    @Override
    protected void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        TileEntity tileentity = worldserver.getBlockEntity(blockposition);

        if (tileentity instanceof TileEntityEnderChest) {
            ((TileEntityEnderChest) tileentity).recheckOpen();
        }

    }
}
