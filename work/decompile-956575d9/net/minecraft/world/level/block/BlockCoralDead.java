package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockCoralDead extends BlockCoralBase {

    public static final MapCodec<BlockCoralDead> CODEC = simpleCodec(BlockCoralDead::new);
    private static final VoxelShape SHAPE = Block.column(12.0D, 0.0D, 15.0D);

    @Override
    public MapCodec<BlockCoralDead> codec() {
        return BlockCoralDead.CODEC;
    }

    protected BlockCoralDead(BlockBase.Info blockbase_info) {
        super(blockbase_info);
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockCoralDead.SHAPE;
    }
}
