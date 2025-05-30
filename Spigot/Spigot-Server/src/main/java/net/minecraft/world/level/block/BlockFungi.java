package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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

public class BlockFungi extends VegetationBlock implements IBlockFragilePlantElement {

    public static final MapCodec<BlockFungi> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter((blockfungi) -> {
            return blockfungi.feature;
        }), BuiltInRegistries.BLOCK.byNameCodec().fieldOf("grows_on").forGetter((blockfungi) -> {
            return blockfungi.requiredBlock;
        }), propertiesCodec()).apply(instance, BlockFungi::new);
    });
    private static final double BONEMEAL_SUCCESS_PROBABILITY = 0.4D;
    private static final VoxelShape SHAPE = Block.column(8.0D, 0.0D, 9.0D);
    private final Block requiredBlock;
    private final ResourceKey<WorldGenFeatureConfigured<?, ?>> feature;

    @Override
    public MapCodec<BlockFungi> codec() {
        return BlockFungi.CODEC;
    }

    protected BlockFungi(ResourceKey<WorldGenFeatureConfigured<?, ?>> resourcekey, Block block, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.feature = resourcekey;
        this.requiredBlock = block;
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockFungi.SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return iblockdata.is(TagsBlock.NYLIUM) || iblockdata.is(Blocks.MYCELIUM) || iblockdata.is(Blocks.SOUL_SOIL) || super.mayPlaceOn(iblockdata, iblockaccess, blockposition);
    }

    private Optional<? extends Holder<WorldGenFeatureConfigured<?, ?>>> getFeature(IWorldReader iworldreader) {
        return iworldreader.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(this.feature);
    }

    @Override
    public boolean isValidBonemealTarget(IWorldReader iworldreader, BlockPosition blockposition, IBlockData iblockdata) {
        IBlockData iblockdata1 = iworldreader.getBlockState(blockposition.below());

        return iblockdata1.is(this.requiredBlock);
    }

    @Override
    public boolean isBonemealSuccess(World world, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        return (double) randomsource.nextFloat() < 0.4D;
    }

    @Override
    public void performBonemeal(WorldServer worldserver, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        this.getFeature(worldserver).ifPresent((holder) -> {
            // CraftBukkit start
            if (this == Blocks.WARPED_FUNGUS) {
                BlockSapling.treeType = org.bukkit.TreeType.WARPED_FUNGUS;
            } else if (this == Blocks.CRIMSON_FUNGUS) {
                BlockSapling.treeType = org.bukkit.TreeType.CRIMSON_FUNGUS;
            }
            // CraftBukkit end
            ((WorldGenFeatureConfigured) holder.value()).place(worldserver, worldserver.getChunkSource().getGenerator(), randomsource, blockposition);
        });
    }
}
