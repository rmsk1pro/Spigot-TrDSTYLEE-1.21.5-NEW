package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntitySign;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyWood;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class CeilingHangingSignBlock extends BlockSign {

    public static final MapCodec<CeilingHangingSignBlock> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(BlockPropertyWood.CODEC.fieldOf("wood_type").forGetter(BlockSign::type), propertiesCodec()).apply(instance, CeilingHangingSignBlock::new);
    });
    public static final BlockStateInteger ROTATION = BlockProperties.ROTATION_16;
    public static final BlockStateBoolean ATTACHED = BlockProperties.ATTACHED;
    private static final VoxelShape SHAPE_DEFAULT = Block.column(10.0D, 0.0D, 16.0D);
    private static final Map<Integer, VoxelShape> SHAPES = (Map) VoxelShapes.rotateHorizontal(Block.column(14.0D, 2.0D, 0.0D, 10.0D)).entrySet().stream().collect(Collectors.toMap((entry) -> {
        return RotationSegment.convertToSegment((EnumDirection) entry.getKey());
    }, Entry::getValue));

    @Override
    public MapCodec<CeilingHangingSignBlock> codec() {
        return CeilingHangingSignBlock.CODEC;
    }

    public CeilingHangingSignBlock(BlockPropertyWood blockpropertywood, BlockBase.Info blockbase_info) {
        super(blockpropertywood, blockbase_info.sound(blockpropertywood.hangingSignSoundType()));
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(CeilingHangingSignBlock.ROTATION, 0)).setValue(CeilingHangingSignBlock.ATTACHED, false)).setValue(CeilingHangingSignBlock.WATERLOGGED, false));
    }

    @Override
    protected EnumInteractionResult useItemOn(ItemStack itemstack, IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, EnumHand enumhand, MovingObjectPositionBlock movingobjectpositionblock) {
        TileEntity tileentity = world.getBlockEntity(blockposition);

        if (tileentity instanceof TileEntitySign tileentitysign) {
            if (this.shouldTryToChainAnotherHangingSign(entityhuman, movingobjectpositionblock, tileentitysign, itemstack)) {
                return EnumInteractionResult.PASS;
            }
        }

        return super.useItemOn(itemstack, iblockdata, world, blockposition, entityhuman, enumhand, movingobjectpositionblock);
    }

    private boolean shouldTryToChainAnotherHangingSign(EntityHuman entityhuman, MovingObjectPositionBlock movingobjectpositionblock, TileEntitySign tileentitysign, ItemStack itemstack) {
        return !tileentitysign.canExecuteClickCommands(tileentitysign.isFacingFrontText(entityhuman), entityhuman) && itemstack.getItem() instanceof HangingSignItem && movingobjectpositionblock.getDirection().equals(EnumDirection.DOWN);
    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        return iworldreader.getBlockState(blockposition.above()).isFaceSturdy(iworldreader, blockposition.above(), EnumDirection.DOWN, EnumBlockSupport.CENTER);
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        World world = blockactioncontext.getLevel();
        Fluid fluid = world.getFluidState(blockactioncontext.getClickedPos());
        BlockPosition blockposition = blockactioncontext.getClickedPos().above();
        IBlockData iblockdata = world.getBlockState(blockposition);
        boolean flag = iblockdata.is(TagsBlock.ALL_HANGING_SIGNS);
        EnumDirection enumdirection = EnumDirection.fromYRot((double) blockactioncontext.getRotation());
        boolean flag1 = !Block.isFaceFull(iblockdata.getCollisionShape(world, blockposition), EnumDirection.DOWN) || blockactioncontext.isSecondaryUseActive();

        if (flag && !blockactioncontext.isSecondaryUseActive()) {
            if (iblockdata.hasProperty(WallHangingSignBlock.FACING)) {
                EnumDirection enumdirection1 = (EnumDirection) iblockdata.getValue(WallHangingSignBlock.FACING);

                if (enumdirection1.getAxis().test(enumdirection)) {
                    flag1 = false;
                }
            } else if (iblockdata.hasProperty(CeilingHangingSignBlock.ROTATION)) {
                Optional<EnumDirection> optional = RotationSegment.convertToDirection((Integer) iblockdata.getValue(CeilingHangingSignBlock.ROTATION));

                if (optional.isPresent() && ((EnumDirection) optional.get()).getAxis().test(enumdirection)) {
                    flag1 = false;
                }
            }
        }

        int i = !flag1 ? RotationSegment.convertToSegment(enumdirection.getOpposite()) : RotationSegment.convertToSegment(blockactioncontext.getRotation() + 180.0F);

        return (IBlockData) ((IBlockData) ((IBlockData) this.defaultBlockState().setValue(CeilingHangingSignBlock.ATTACHED, flag1)).setValue(CeilingHangingSignBlock.ROTATION, i)).setValue(CeilingHangingSignBlock.WATERLOGGED, fluid.getType() == FluidTypes.WATER);
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return (VoxelShape) CeilingHangingSignBlock.SHAPES.getOrDefault(iblockdata.getValue(CeilingHangingSignBlock.ROTATION), CeilingHangingSignBlock.SHAPE_DEFAULT);
    }

    @Override
    protected VoxelShape getBlockSupportShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return this.getShape(iblockdata, iblockaccess, blockposition, VoxelShapeCollision.empty());
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        return enumdirection == EnumDirection.UP && !this.canSurvive(iblockdata, iworldreader, blockposition) ? Blocks.AIR.defaultBlockState() : super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
    }

    @Override
    public float getYRotationDegrees(IBlockData iblockdata) {
        return RotationSegment.convertToDegrees((Integer) iblockdata.getValue(CeilingHangingSignBlock.ROTATION));
    }

    @Override
    protected IBlockData rotate(IBlockData iblockdata, EnumBlockRotation enumblockrotation) {
        return (IBlockData) iblockdata.setValue(CeilingHangingSignBlock.ROTATION, enumblockrotation.rotate((Integer) iblockdata.getValue(CeilingHangingSignBlock.ROTATION), 16));
    }

    @Override
    protected IBlockData mirror(IBlockData iblockdata, EnumBlockMirror enumblockmirror) {
        return (IBlockData) iblockdata.setValue(CeilingHangingSignBlock.ROTATION, enumblockmirror.mirror((Integer) iblockdata.getValue(CeilingHangingSignBlock.ROTATION), 16));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(CeilingHangingSignBlock.ROTATION, CeilingHangingSignBlock.ATTACHED, CeilingHangingSignBlock.WATERLOGGED);
    }

    @Override
    public TileEntity newBlockEntity(BlockPosition blockposition, IBlockData iblockdata) {
        return new HangingSignBlockEntity(blockposition, iblockdata);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData iblockdata, TileEntityTypes<T> tileentitytypes) {
        return null; // Craftbukkit - remove unnecessary sign ticking
    }
}
