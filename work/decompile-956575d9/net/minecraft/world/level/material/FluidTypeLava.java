package net.minecraft.world.level.material;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.BlockFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class FluidTypeLava extends FluidTypeFlowing {

    public static final float MIN_LEVEL_CUTOFF = 0.44444445F;

    public FluidTypeLava() {}

    @Override
    public FluidType getFlowing() {
        return FluidTypes.FLOWING_LAVA;
    }

    @Override
    public FluidType getSource() {
        return FluidTypes.LAVA;
    }

    @Override
    public Item getBucket() {
        return Items.LAVA_BUCKET;
    }

    @Override
    public void animateTick(World world, BlockPosition blockposition, Fluid fluid, RandomSource randomsource) {
        BlockPosition blockposition1 = blockposition.above();

        if (world.getBlockState(blockposition1).isAir() && !world.getBlockState(blockposition1).isSolidRender()) {
            if (randomsource.nextInt(100) == 0) {
                double d0 = (double) blockposition.getX() + randomsource.nextDouble();
                double d1 = (double) blockposition.getY() + 1.0D;
                double d2 = (double) blockposition.getZ() + randomsource.nextDouble();

                world.addParticle(Particles.LAVA, d0, d1, d2, 0.0D, 0.0D, 0.0D);
                world.playLocalSound(d0, d1, d2, SoundEffects.LAVA_POP, SoundCategory.AMBIENT, 0.2F + randomsource.nextFloat() * 0.2F, 0.9F + randomsource.nextFloat() * 0.15F, false);
            }

            if (randomsource.nextInt(200) == 0) {
                world.playLocalSound((double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ(), SoundEffects.LAVA_AMBIENT, SoundCategory.AMBIENT, 0.2F + randomsource.nextFloat() * 0.2F, 0.9F + randomsource.nextFloat() * 0.15F, false);
            }
        }

    }

    @Override
    public void randomTick(WorldServer worldserver, BlockPosition blockposition, Fluid fluid, RandomSource randomsource) {
        if (worldserver.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            if (worldserver.getGameRules().getBoolean(GameRules.RULE_ALLOWFIRETICKAWAYFROMPLAYERS) || worldserver.anyPlayerCloseEnoughForSpawning(blockposition)) {
                int i = randomsource.nextInt(3);

                if (i > 0) {
                    BlockPosition blockposition1 = blockposition;

                    for (int j = 0; j < i; ++j) {
                        blockposition1 = blockposition1.offset(randomsource.nextInt(3) - 1, 1, randomsource.nextInt(3) - 1);
                        if (!worldserver.isLoaded(blockposition1)) {
                            return;
                        }

                        IBlockData iblockdata = worldserver.getBlockState(blockposition1);

                        if (iblockdata.isAir()) {
                            if (this.hasFlammableNeighbours(worldserver, blockposition1)) {
                                worldserver.setBlockAndUpdate(blockposition1, BlockFireAbstract.getState(worldserver, blockposition1));
                                return;
                            }
                        } else if (iblockdata.blocksMotion()) {
                            return;
                        }
                    }
                } else {
                    for (int k = 0; k < 3; ++k) {
                        BlockPosition blockposition2 = blockposition.offset(randomsource.nextInt(3) - 1, 0, randomsource.nextInt(3) - 1);

                        if (!worldserver.isLoaded(blockposition2)) {
                            return;
                        }

                        if (worldserver.isEmptyBlock(blockposition2.above()) && this.isFlammable(worldserver, blockposition2)) {
                            worldserver.setBlockAndUpdate(blockposition2.above(), BlockFireAbstract.getState(worldserver, blockposition2));
                        }
                    }
                }

            }
        }
    }

    @Override
    protected void entityInside(World world, BlockPosition blockposition, Entity entity, InsideBlockEffectApplier insideblockeffectapplier) {
        insideblockeffectapplier.apply(InsideBlockEffectType.LAVA_IGNITE);
        insideblockeffectapplier.runAfter(InsideBlockEffectType.LAVA_IGNITE, Entity::lavaHurt);
    }

    private boolean hasFlammableNeighbours(IWorldReader iworldreader, BlockPosition blockposition) {
        for (EnumDirection enumdirection : EnumDirection.values()) {
            if (this.isFlammable(iworldreader, blockposition.relative(enumdirection))) {
                return true;
            }
        }

        return false;
    }

    private boolean isFlammable(IWorldReader iworldreader, BlockPosition blockposition) {
        return iworldreader.isInsideBuildHeight(blockposition.getY()) && !iworldreader.hasChunkAt(blockposition) ? false : iworldreader.getBlockState(blockposition).ignitedByLava();
    }

    @Nullable
    @Override
    public ParticleParam getDripParticle() {
        return Particles.DRIPPING_LAVA;
    }

    @Override
    protected void beforeDestroyingBlock(GeneratorAccess generatoraccess, BlockPosition blockposition, IBlockData iblockdata) {
        this.fizz(generatoraccess, blockposition);
    }

    @Override
    public int getSlopeFindDistance(IWorldReader iworldreader) {
        return iworldreader.dimensionType().ultraWarm() ? 4 : 2;
    }

    @Override
    public IBlockData createLegacyBlock(Fluid fluid) {
        return (IBlockData) Blocks.LAVA.defaultBlockState().setValue(BlockFluids.LEVEL, getLegacyLevel(fluid));
    }

    @Override
    public boolean isSame(FluidType fluidtype) {
        return fluidtype == FluidTypes.LAVA || fluidtype == FluidTypes.FLOWING_LAVA;
    }

    @Override
    public int getDropOff(IWorldReader iworldreader) {
        return iworldreader.dimensionType().ultraWarm() ? 1 : 2;
    }

    @Override
    public boolean canBeReplacedWith(Fluid fluid, IBlockAccess iblockaccess, BlockPosition blockposition, FluidType fluidtype, EnumDirection enumdirection) {
        return fluid.getHeight(iblockaccess, blockposition) >= 0.44444445F && fluidtype.is(TagsFluid.WATER);
    }

    @Override
    public int getTickDelay(IWorldReader iworldreader) {
        return iworldreader.dimensionType().ultraWarm() ? 10 : 30;
    }

    @Override
    public int getSpreadDelay(World world, BlockPosition blockposition, Fluid fluid, Fluid fluid1) {
        int i = this.getTickDelay(world);

        if (!fluid.isEmpty() && !fluid1.isEmpty() && !(Boolean) fluid.getValue(FluidTypeLava.FALLING) && !(Boolean) fluid1.getValue(FluidTypeLava.FALLING) && fluid1.getHeight(world, blockposition) > fluid.getHeight(world, blockposition) && world.getRandom().nextInt(4) != 0) {
            i *= 4;
        }

        return i;
    }

    private void fizz(GeneratorAccess generatoraccess, BlockPosition blockposition) {
        generatoraccess.levelEvent(1501, blockposition, 0);
    }

    @Override
    protected boolean canConvertToSource(WorldServer worldserver) {
        return worldserver.getGameRules().getBoolean(GameRules.RULE_LAVA_SOURCE_CONVERSION);
    }

    @Override
    protected void spreadTo(GeneratorAccess generatoraccess, BlockPosition blockposition, IBlockData iblockdata, EnumDirection enumdirection, Fluid fluid) {
        if (enumdirection == EnumDirection.DOWN) {
            Fluid fluid1 = generatoraccess.getFluidState(blockposition);

            if (this.is(TagsFluid.LAVA) && fluid1.is(TagsFluid.WATER)) {
                if (iblockdata.getBlock() instanceof BlockFluids) {
                    generatoraccess.setBlock(blockposition, Blocks.STONE.defaultBlockState(), 3);
                }

                this.fizz(generatoraccess, blockposition);
                return;
            }
        }

        super.spreadTo(generatoraccess, blockposition, iblockdata, enumdirection, fluid);
    }

    @Override
    protected boolean isRandomlyTicking() {
        return true;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public Optional<SoundEffect> getPickupSound() {
        return Optional.of(SoundEffects.BUCKET_FILL_LAVA);
    }

    public static class b extends FluidTypeLava {

        public b() {}

        @Override
        public int getAmount(Fluid fluid) {
            return 8;
        }

        @Override
        public boolean isSource(Fluid fluid) {
            return true;
        }
    }

    public static class a extends FluidTypeLava {

        public a() {}

        @Override
        protected void createFluidStateDefinition(BlockStateList.a<FluidType, Fluid> blockstatelist_a) {
            super.createFluidStateDefinition(blockstatelist_a);
            blockstatelist_a.add(FluidTypeLava.a.LEVEL);
        }

        @Override
        public int getAmount(Fluid fluid) {
            return (Integer) fluid.getValue(FluidTypeLava.a.LEVEL);
        }

        @Override
        public boolean isSource(Fluid fluid) {
            return false;
        }
    }
}
