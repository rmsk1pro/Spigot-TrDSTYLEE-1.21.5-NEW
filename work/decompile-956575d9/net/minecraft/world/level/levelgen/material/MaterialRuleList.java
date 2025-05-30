package net.minecraft.world.level.levelgen.material;

import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;

public record MaterialRuleList(NoiseChunk.c[] materialRuleList) implements NoiseChunk.c {

    @Nullable
    @Override
    public IBlockData calculate(DensityFunction.b densityfunction_b) {
        for (NoiseChunk.c noisechunk_c : this.materialRuleList) {
            IBlockData iblockdata = noisechunk_c.calculate(densityfunction_b);

            if (iblockdata != null) {
                return iblockdata;
            }
        }

        return null;
    }
}
