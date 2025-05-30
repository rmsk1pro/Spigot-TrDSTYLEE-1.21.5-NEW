package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.monster.EntityRavager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockCrops extends VegetationBlock implements IBlockFragilePlantElement {

    public static final MapCodec<BlockCrops> CODEC = simpleCodec(BlockCrops::new);
    public static final int MAX_AGE = 7;
    public static final BlockStateInteger AGE = BlockProperties.AGE_7;
    private static final VoxelShape[] SHAPES = Block.boxes(7, (i) -> {
        return Block.column(16.0D, 0.0D, (double) (2 + i * 2));
    });

    @Override
    public MapCodec<? extends BlockCrops> codec() {
        return BlockCrops.CODEC;
    }

    protected BlockCrops(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) (this.stateDefinition.any()).setValue(this.getAgeProperty(), 0));
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockCrops.SHAPES[this.getAge(iblockdata)];
    }

    @Override
    protected boolean mayPlaceOn(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return iblockdata.is(Blocks.FARMLAND);
    }

    protected BlockStateInteger getAgeProperty() {
        return BlockCrops.AGE;
    }

    public int getMaxAge() {
        return 7;
    }

    public int getAge(IBlockData iblockdata) {
        return (Integer) iblockdata.getValue(this.getAgeProperty());
    }

    public IBlockData getStateForAge(int i) {
        return (IBlockData) this.defaultBlockState().setValue(this.getAgeProperty(), i);
    }

    public final boolean isMaxAge(IBlockData iblockdata) {
        return this.getAge(iblockdata) >= this.getMaxAge();
    }

    @Override
    protected boolean isRandomlyTicking(IBlockData iblockdata) {
        return !this.isMaxAge(iblockdata);
    }

    @Override
    protected void randomTick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        if (worldserver.getRawBrightness(blockposition, 0) >= 9) {
            int i = this.getAge(iblockdata);

            if (i < this.getMaxAge()) {
                float f = getGrowthSpeed(this, worldserver, blockposition);

                if (randomsource.nextInt((int) (25.0F / f) + 1) == 0) {
                    worldserver.setBlock(blockposition, this.getStateForAge(i + 1), 2);
                }
            }
        }

    }

    public void growCrops(World world, BlockPosition blockposition, IBlockData iblockdata) {
        int i = Math.min(this.getMaxAge(), this.getAge(iblockdata) + this.getBonemealAgeIncrease(world));

        world.setBlock(blockposition, this.getStateForAge(i), 2);
    }

    protected int getBonemealAgeIncrease(World world) {
        return MathHelper.nextInt(world.random, 2, 5);
    }

    protected static float getGrowthSpeed(Block block, IBlockAccess iblockaccess, BlockPosition blockposition) {
        float f = 1.0F;
        BlockPosition blockposition1 = blockposition.below();

        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                float f1 = 0.0F;
                IBlockData iblockdata = iblockaccess.getBlockState(blockposition1.offset(i, 0, j));

                if (iblockdata.is(Blocks.FARMLAND)) {
                    f1 = 1.0F;
                    if ((Integer) iblockdata.getValue(BlockSoil.MOISTURE) > 0) {
                        f1 = 3.0F;
                    }
                }

                if (i != 0 || j != 0) {
                    f1 /= 4.0F;
                }

                f += f1;
            }
        }

        BlockPosition blockposition2 = blockposition.north();
        BlockPosition blockposition3 = blockposition.south();
        BlockPosition blockposition4 = blockposition.west();
        BlockPosition blockposition5 = blockposition.east();
        boolean flag = iblockaccess.getBlockState(blockposition4).is(block) || iblockaccess.getBlockState(blockposition5).is(block);
        boolean flag1 = iblockaccess.getBlockState(blockposition2).is(block) || iblockaccess.getBlockState(blockposition3).is(block);

        if (flag && flag1) {
            f /= 2.0F;
        } else {
            boolean flag2 = iblockaccess.getBlockState(blockposition4.north()).is(block) || iblockaccess.getBlockState(blockposition5.north()).is(block) || iblockaccess.getBlockState(blockposition5.south()).is(block) || iblockaccess.getBlockState(blockposition4.south()).is(block);

            if (flag2) {
                f /= 2.0F;
            }
        }

        return f;
    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        return hasSufficientLight(iworldreader, blockposition) && super.canSurvive(iblockdata, iworldreader, blockposition);
    }

    protected static boolean hasSufficientLight(IWorldReader iworldreader, BlockPosition blockposition) {
        return iworldreader.getRawBrightness(blockposition, 0) >= 8;
    }

    @Override
    protected void entityInside(IBlockData iblockdata, World world, BlockPosition blockposition, Entity entity, InsideBlockEffectApplier insideblockeffectapplier) {
        if (world instanceof WorldServer worldserver) {
            if (entity instanceof EntityRavager && worldserver.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                worldserver.destroyBlock(blockposition, true, entity);
            }
        }

        super.entityInside(iblockdata, world, blockposition, entity, insideblockeffectapplier);
    }

    protected IMaterial getBaseSeedId() {
        return Items.WHEAT_SEEDS;
    }

    @Override
    protected ItemStack getCloneItemStack(IWorldReader iworldreader, BlockPosition blockposition, IBlockData iblockdata, boolean flag) {
        return new ItemStack(this.getBaseSeedId());
    }

    @Override
    public boolean isValidBonemealTarget(IWorldReader iworldreader, BlockPosition blockposition, IBlockData iblockdata) {
        return !this.isMaxAge(iblockdata);
    }

    @Override
    public boolean isBonemealSuccess(World world, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer worldserver, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        this.growCrops(worldserver, blockposition, iblockdata);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockCrops.AGE);
    }
}
