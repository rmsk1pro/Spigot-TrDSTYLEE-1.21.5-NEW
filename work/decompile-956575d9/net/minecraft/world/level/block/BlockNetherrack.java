package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockNetherrack extends Block implements IBlockFragilePlantElement {

    public static final MapCodec<BlockNetherrack> CODEC = simpleCodec(BlockNetherrack::new);

    @Override
    public MapCodec<BlockNetherrack> codec() {
        return BlockNetherrack.CODEC;
    }

    public BlockNetherrack(BlockBase.Info blockbase_info) {
        super(blockbase_info);
    }

    @Override
    public boolean isValidBonemealTarget(IWorldReader iworldreader, BlockPosition blockposition, IBlockData iblockdata) {
        if (!iworldreader.getBlockState(blockposition.above()).propagatesSkylightDown()) {
            return false;
        } else {
            for (BlockPosition blockposition1 : BlockPosition.betweenClosed(blockposition.offset(-1, -1, -1), blockposition.offset(1, 1, 1))) {
                if (iworldreader.getBlockState(blockposition1).is(TagsBlock.NYLIUM)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean isBonemealSuccess(World world, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer worldserver, RandomSource randomsource, BlockPosition blockposition, IBlockData iblockdata) {
        boolean flag = false;
        boolean flag1 = false;

        for (BlockPosition blockposition1 : BlockPosition.betweenClosed(blockposition.offset(-1, -1, -1), blockposition.offset(1, 1, 1))) {
            IBlockData iblockdata1 = worldserver.getBlockState(blockposition1);

            if (iblockdata1.is(Blocks.WARPED_NYLIUM)) {
                flag1 = true;
            }

            if (iblockdata1.is(Blocks.CRIMSON_NYLIUM)) {
                flag = true;
            }

            if (flag1 && flag) {
                break;
            }
        }

        if (flag1 && flag) {
            worldserver.setBlock(blockposition, randomsource.nextBoolean() ? Blocks.WARPED_NYLIUM.defaultBlockState() : Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
        } else if (flag1) {
            worldserver.setBlock(blockposition, Blocks.WARPED_NYLIUM.defaultBlockState(), 3);
        } else if (flag) {
            worldserver.setBlock(blockposition, Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
        }

    }

    @Override
    public IBlockFragilePlantElement.a getType() {
        return IBlockFragilePlantElement.a.NEIGHBOR_SPREADER;
    }
}
