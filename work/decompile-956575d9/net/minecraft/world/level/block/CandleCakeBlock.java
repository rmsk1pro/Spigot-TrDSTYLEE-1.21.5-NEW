package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class CandleCakeBlock extends AbstractCandleBlock {

    public static final MapCodec<CandleCakeBlock> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("candle").forGetter((candlecakeblock) -> {
            return candlecakeblock.candleBlock;
        }), propertiesCodec()).apply(instance, CandleCakeBlock::new);
    });
    public static final BlockStateBoolean LIT = AbstractCandleBlock.LIT;
    private static final VoxelShape SHAPE = VoxelShapes.or(Block.column(2.0D, 8.0D, 14.0D), Block.column(14.0D, 0.0D, 8.0D));
    private static final Map<CandleBlock, CandleCakeBlock> BY_CANDLE = Maps.newHashMap();
    private static final Iterable<Vec3D> PARTICLE_OFFSETS = List.of((new Vec3D(8.0D, 16.0D, 8.0D)).scale(0.0625D));
    private final CandleBlock candleBlock;

    @Override
    public MapCodec<CandleCakeBlock> codec() {
        return CandleCakeBlock.CODEC;
    }

    protected CandleCakeBlock(Block block, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) (this.stateDefinition.any()).setValue(CandleCakeBlock.LIT, false));
        if (block instanceof CandleBlock candleblock) {
            CandleCakeBlock.BY_CANDLE.put(candleblock, this);
            this.candleBlock = candleblock;
        } else {
            String s = String.valueOf(CandleBlock.class);

            throw new IllegalArgumentException("Expected block to be of " + s + " was " + String.valueOf(block.getClass()));
        }
    }

    @Override
    protected Iterable<Vec3D> getParticleOffsets(IBlockData iblockdata) {
        return CandleCakeBlock.PARTICLE_OFFSETS;
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return CandleCakeBlock.SHAPE;
    }

    @Override
    protected EnumInteractionResult useItemOn(ItemStack itemstack, IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, EnumHand enumhand, MovingObjectPositionBlock movingobjectpositionblock) {
        if (!itemstack.is(Items.FLINT_AND_STEEL) && !itemstack.is(Items.FIRE_CHARGE)) {
            if (candleHit(movingobjectpositionblock) && itemstack.isEmpty() && (Boolean) iblockdata.getValue(CandleCakeBlock.LIT)) {
                extinguish(entityhuman, iblockdata, world, blockposition);
                return EnumInteractionResult.SUCCESS;
            } else {
                return super.useItemOn(itemstack, iblockdata, world, blockposition, entityhuman, enumhand, movingobjectpositionblock);
            }
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    protected EnumInteractionResult useWithoutItem(IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, MovingObjectPositionBlock movingobjectpositionblock) {
        EnumInteractionResult enuminteractionresult = BlockCake.eat(world, blockposition, Blocks.CAKE.defaultBlockState(), entityhuman);

        if (enuminteractionresult.consumesAction()) {
            dropResources(iblockdata, world, blockposition);
        }

        return enuminteractionresult;
    }

    private static boolean candleHit(MovingObjectPositionBlock movingobjectpositionblock) {
        return movingobjectpositionblock.getLocation().y - (double) movingobjectpositionblock.getBlockPos().getY() > 0.5D;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(CandleCakeBlock.LIT);
    }

    @Override
    protected ItemStack getCloneItemStack(IWorldReader iworldreader, BlockPosition blockposition, IBlockData iblockdata, boolean flag) {
        return new ItemStack(Blocks.CAKE);
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        return enumdirection == EnumDirection.DOWN && !iblockdata.canSurvive(iworldreader, blockposition) ? Blocks.AIR.defaultBlockState() : super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        return iworldreader.getBlockState(blockposition.below()).isSolid();
    }

    @Override
    protected int getAnalogOutputSignal(IBlockData iblockdata, World world, BlockPosition blockposition) {
        return BlockCake.FULL_CAKE_SIGNAL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(IBlockData iblockdata) {
        return true;
    }

    @Override
    protected boolean isPathfindable(IBlockData iblockdata, PathMode pathmode) {
        return false;
    }

    public static IBlockData byCandle(CandleBlock candleblock) {
        return ((CandleCakeBlock) CandleCakeBlock.BY_CANDLE.get(candleblock)).defaultBlockState();
    }

    public static boolean canLight(IBlockData iblockdata) {
        return iblockdata.is(TagsBlock.CANDLE_CAKES, (blockbase_blockdata) -> {
            return blockbase_blockdata.hasProperty(CandleCakeBlock.LIT) && !(Boolean) iblockdata.getValue(CandleCakeBlock.LIT);
        });
    }
}
