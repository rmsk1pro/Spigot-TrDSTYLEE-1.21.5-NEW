package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

// CraftBukkit start
import org.bukkit.TreeType;
// CraftBukkit end

public class BlockMushroom extends VegetationBlock implements IBlockFragilePlantElement {

    public static final MapCodec<BlockMushroom> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter((blockmushroom) -> {
            return blockmushroom.feature;
        }), propertiesCodec()).apply(instance, BlockMushroom::new);
    });
    private static final VoxelShape SHAPE = Block.column(6.0D, 0.0D, 6.0D);
    private final ResourceKey<WorldGenFeatureConfigured<?, ?>> feature;

    @Override
    public MapCodec<BlockMushroom> codec() {
        return BlockMushroom.CODEC;
    }

    public BlockMushroom(ResourceKey<WorldGenFeatureConfigured<?, ?>> resourcekey, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.feature = resourcekey;
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockMushroom.SHAPE;
    }

    @Override
    protected void randomTick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        if (randomsource.nextFloat() < (worldserver.spigotConfig.mushroomModifier / (100.0f * 25))) { // Spigot - SPIGOT-7159: Better modifier resolution
            int i = 5;
            int j = 4;

            for (BlockPosition blockposition1 : BlockPosition.betweenClosed(blockposition.offset(-4, -1, -4), blockposition.offset(4, 1, 4))) {
                if (worldserver.getBlockState(blockposition1).is(this)) {
                    --i;
                    if (i <= 0) {
                        return;
                    }
                }
            }

            BlockPosition blockposition2 = blockposition.offset(randomsource.nextInt(3) - 1, randomsource.nextInt(2) - randomsource.nextInt(2), randomsource.nextInt(3) - 1);

            for (int k = 0; k < 4; ++k) {
                if (worldserver.isEmptyBlock(blockposition2) && iblockdata.canSurvive(worldserver, blockposition2)) {
                    blockposition = blockposition2;
                }

                blockposition2 = blockposition.offset(randomsource.nextInt(3) - 1, randomsource.nextInt(2) - randomsource.nextInt(2), randomsource.nextInt(3) - 1);
            }

            if (worldserver.isEmptyBlock(blockposition2) && iblockdata.canSurvive(worldserver, blockposition2)) {
                org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockSpreadEvent(worldserver, blockposition, blockposition2, iblockdata, 2); // CraftBukkit
            }
        }

    }

    @Override
    protected boolean mayPlaceOn(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return iblockdata.isSolidRender();
    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        BlockPosition blockposition1 = blockposition.below();
        IBlockData iblockdata1 = iworldreader.getBlockState(blockposition1);

        return iblockdata1.is(TagsBlock.MUSHROOM_GROW_BLOCK) ? true : iworldreader.getRawBrightness(blockposition, 0) < 13 && this.mayPlaceOn(iblockdata1, iworldreader, blockposition1);
    }

    public boolean growMushroom(WorldServer worldserver, BlockPosition blockposition, IBlockData iblockdata, RandomSource randomsource) {
        Optional<? extends Holder<WorldGenFeatureConfigured<?, ?>>> optional = worldserver.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(this.feature);

        if (optional.isEmpty()) {
            return false;
        } else {
            worldserver.removeBlock(blockposition, false);
            BlockSapling.treeType = (this == Blocks.BROWN_MUSHROOM) ? TreeType.BROWN_MUSHROOM : TreeType.RED_MUSHROOM; // CraftBukkit
            if (((WorldGenFeatureConfigured) ((Holder) optional.get()).value()).place(worldserver, worldserver.getChunkSource().getGenerator(), randomsource, blockposition)) {
                return true;
            } else {
                worldserver.setBlock(blockposition, iblockdata, 3);
                return false;
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(IWorldReader iworldreader, BlockPosition blockposition, IBlockData iblockdata) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(World world, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        return (double) randomsource.nextFloat() < 0.4D;
    }

    @Override
    public void performBonemeal(WorldServer worldserver, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        this.growMushroom(worldserver, blockposition, iblockdata, randomsource);
    }
}
