package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class SculkShriekerBlock extends BlockTileEntity implements IBlockWaterlogged {

    public static final MapCodec<SculkShriekerBlock> CODEC = simpleCodec(SculkShriekerBlock::new);
    public static final BlockStateBoolean SHRIEKING = BlockProperties.SHRIEKING;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final BlockStateBoolean CAN_SUMMON = BlockProperties.CAN_SUMMON;
    private static final VoxelShape SHAPE_COLLISION = Block.column(16.0D, 0.0D, 8.0D);
    public static final double TOP_Y = SculkShriekerBlock.SHAPE_COLLISION.max(EnumDirection.EnumAxis.Y);

    @Override
    public MapCodec<SculkShriekerBlock> codec() {
        return SculkShriekerBlock.CODEC;
    }

    public SculkShriekerBlock(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(SculkShriekerBlock.SHRIEKING, false)).setValue(SculkShriekerBlock.WATERLOGGED, false)).setValue(SculkShriekerBlock.CAN_SUMMON, false));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(SculkShriekerBlock.SHRIEKING);
        blockstatelist_a.add(SculkShriekerBlock.WATERLOGGED);
        blockstatelist_a.add(SculkShriekerBlock.CAN_SUMMON);
    }

    @Override
    public void stepOn(World world, BlockPosition blockposition, IBlockData iblockdata, Entity entity) {
        if (world instanceof WorldServer worldserver) {
            EntityPlayer entityplayer = SculkShriekerBlockEntity.tryGetPlayer(entity);

            if (entityplayer != null) {
                worldserver.getBlockEntity(blockposition, TileEntityTypes.SCULK_SHRIEKER).ifPresent((sculkshriekerblockentity) -> {
                    sculkshriekerblockentity.tryShriek(worldserver, entityplayer);
                });
            }
        }

        super.stepOn(world, blockposition, iblockdata, entity);
    }

    @Override
    protected void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        if ((Boolean) iblockdata.getValue(SculkShriekerBlock.SHRIEKING)) {
            worldserver.setBlock(blockposition, (IBlockData) iblockdata.setValue(SculkShriekerBlock.SHRIEKING, false), 3);
            worldserver.getBlockEntity(blockposition, TileEntityTypes.SCULK_SHRIEKER).ifPresent((sculkshriekerblockentity) -> {
                sculkshriekerblockentity.tryRespond(worldserver);
            });
        }

    }

    @Override
    protected VoxelShape getCollisionShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return SculkShriekerBlock.SHAPE_COLLISION;
    }

    @Override
    protected VoxelShape getOcclusionShape(IBlockData iblockdata) {
        return SculkShriekerBlock.SHAPE_COLLISION;
    }

    @Override
    protected boolean useShapeForLightOcclusion(IBlockData iblockdata) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(BlockPosition blockposition, IBlockData iblockdata) {
        return new SculkShriekerBlockEntity(blockposition, iblockdata);
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        if ((Boolean) iblockdata.getValue(SculkShriekerBlock.WATERLOGGED)) {
            scheduledtickaccess.scheduleTick(blockposition, (FluidType) FluidTypes.WATER, FluidTypes.WATER.getTickDelay(iworldreader));
        }

        return super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
    }

    @Nullable
    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        return (IBlockData) this.defaultBlockState().setValue(SculkShriekerBlock.WATERLOGGED, blockactioncontext.getLevel().getFluidState(blockactioncontext.getClickedPos()).getType() == FluidTypes.WATER);
    }

    @Override
    protected Fluid getFluidState(IBlockData iblockdata) {
        return (Boolean) iblockdata.getValue(SculkShriekerBlock.WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(iblockdata);
    }

    @Override
    protected void spawnAfterBreak(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, ItemStack itemstack, boolean flag) {
        super.spawnAfterBreak(iblockdata, worldserver, blockposition, itemstack, flag);
        if (flag) {
            this.tryDropExperience(worldserver, blockposition, itemstack, ConstantInt.of(5));
        }

    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData iblockdata, TileEntityTypes<T> tileentitytypes) {
        return !world.isClientSide ? BlockTileEntity.createTickerHelper(tileentitytypes, TileEntityTypes.SCULK_SHRIEKER, (world1, blockposition, iblockdata1, sculkshriekerblockentity) -> {
            VibrationSystem.c.tick(world1, sculkshriekerblockentity.getVibrationData(), sculkshriekerblockentity.getVibrationUser());
        }) : null;
    }
}
