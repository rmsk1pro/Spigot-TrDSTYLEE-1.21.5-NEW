package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;

public class ChunkSection {

    public static final int SECTION_WIDTH = 16;
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_SIZE = 4096;
    public static final int BIOME_CONTAINER_BITS = 2;
    private short nonEmptyBlockCount;
    private short tickingBlockCount;
    private short tickingFluidCount;
    private final DataPaletteBlock<IBlockData> states;
    private PalettedContainerRO<Holder<BiomeBase>> biomes;

    private ChunkSection(ChunkSection chunksection) {
        this.nonEmptyBlockCount = chunksection.nonEmptyBlockCount;
        this.tickingBlockCount = chunksection.tickingBlockCount;
        this.tickingFluidCount = chunksection.tickingFluidCount;
        this.states = chunksection.states.copy();
        this.biomes = chunksection.biomes.copy();
    }

    public ChunkSection(DataPaletteBlock<IBlockData> datapaletteblock, PalettedContainerRO<Holder<BiomeBase>> palettedcontainerro) {
        this.states = datapaletteblock;
        this.biomes = palettedcontainerro;
        this.recalcBlockCounts();
    }

    public ChunkSection(IRegistry<BiomeBase> iregistry) {
        this.states = new DataPaletteBlock<IBlockData>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), DataPaletteBlock.d.SECTION_STATES);
        this.biomes = new DataPaletteBlock<Holder<BiomeBase>>(iregistry.asHolderIdMap(), iregistry.getOrThrow(Biomes.PLAINS), DataPaletteBlock.d.SECTION_BIOMES);
    }

    public IBlockData getBlockState(int i, int j, int k) {
        return (IBlockData) this.states.get(i, j, k);
    }

    public Fluid getFluidState(int i, int j, int k) {
        return ((IBlockData) this.states.get(i, j, k)).getFluidState();
    }

    public void acquire() {
        this.states.acquire();
    }

    public void release() {
        this.states.release();
    }

    public IBlockData setBlockState(int i, int j, int k, IBlockData iblockdata) {
        return this.setBlockState(i, j, k, iblockdata, true);
    }

    public IBlockData setBlockState(int i, int j, int k, IBlockData iblockdata, boolean flag) {
        IBlockData iblockdata1;

        if (flag) {
            iblockdata1 = this.states.getAndSet(i, j, k, iblockdata);
        } else {
            iblockdata1 = this.states.getAndSetUnchecked(i, j, k, iblockdata);
        }

        Fluid fluid = iblockdata1.getFluidState();
        Fluid fluid1 = iblockdata.getFluidState();

        if (!iblockdata1.isAir()) {
            --this.nonEmptyBlockCount;
            if (iblockdata1.isRandomlyTicking()) {
                --this.tickingBlockCount;
            }
        }

        if (!fluid.isEmpty()) {
            --this.tickingFluidCount;
        }

        if (!iblockdata.isAir()) {
            ++this.nonEmptyBlockCount;
            if (iblockdata.isRandomlyTicking()) {
                ++this.tickingBlockCount;
            }
        }

        if (!fluid1.isEmpty()) {
            ++this.tickingFluidCount;
        }

        return iblockdata1;
    }

    public boolean hasOnlyAir() {
        return this.nonEmptyBlockCount == 0;
    }

    public boolean isRandomlyTicking() {
        return this.isRandomlyTickingBlocks() || this.isRandomlyTickingFluids();
    }

    public boolean isRandomlyTickingBlocks() {
        return this.tickingBlockCount > 0;
    }

    public boolean isRandomlyTickingFluids() {
        return this.tickingFluidCount > 0;
    }

    public void recalcBlockCounts() {
        class a implements DataPaletteBlock.b<IBlockData> {

            public int nonEmptyBlockCount;
            public int tickingBlockCount;
            public int tickingFluidCount;

            a() {}

            public void accept(IBlockData iblockdata, int i) {
                Fluid fluid = iblockdata.getFluidState();

                if (!iblockdata.isAir()) {
                    this.nonEmptyBlockCount += i;
                    if (iblockdata.isRandomlyTicking()) {
                        this.tickingBlockCount += i;
                    }
                }

                if (!fluid.isEmpty()) {
                    this.nonEmptyBlockCount += i;
                    if (fluid.isRandomlyTicking()) {
                        this.tickingFluidCount += i;
                    }
                }

            }
        }

        a a0 = new a();

        this.states.count(a0);
        this.nonEmptyBlockCount = (short) a0.nonEmptyBlockCount;
        this.tickingBlockCount = (short) a0.tickingBlockCount;
        this.tickingFluidCount = (short) a0.tickingFluidCount;
    }

    public DataPaletteBlock<IBlockData> getStates() {
        return this.states;
    }

    public PalettedContainerRO<Holder<BiomeBase>> getBiomes() {
        return this.biomes;
    }

    public void read(PacketDataSerializer packetdataserializer) {
        this.nonEmptyBlockCount = packetdataserializer.readShort();
        this.states.read(packetdataserializer);
        DataPaletteBlock<Holder<BiomeBase>> datapaletteblock = this.biomes.recreate();

        datapaletteblock.read(packetdataserializer);
        this.biomes = datapaletteblock;
    }

    public void readBiomes(PacketDataSerializer packetdataserializer) {
        DataPaletteBlock<Holder<BiomeBase>> datapaletteblock = this.biomes.recreate();

        datapaletteblock.read(packetdataserializer);
        this.biomes = datapaletteblock;
    }

    public void write(PacketDataSerializer packetdataserializer) {
        packetdataserializer.writeShort(this.nonEmptyBlockCount);
        this.states.write(packetdataserializer);
        this.biomes.write(packetdataserializer);
    }

    public int getSerializedSize() {
        return 2 + this.states.getSerializedSize() + this.biomes.getSerializedSize();
    }

    public boolean maybeHas(Predicate<IBlockData> predicate) {
        return this.states.maybeHas(predicate);
    }

    public Holder<BiomeBase> getNoiseBiome(int i, int j, int k) {
        return this.biomes.get(i, j, k);
    }

    public void fillBiomesFromNoise(BiomeResolver biomeresolver, Climate.Sampler climate_sampler, int i, int j, int k) {
        DataPaletteBlock<Holder<BiomeBase>> datapaletteblock = this.biomes.recreate();
        int l = 4;

        for (int i1 = 0; i1 < 4; ++i1) {
            for (int j1 = 0; j1 < 4; ++j1) {
                for (int k1 = 0; k1 < 4; ++k1) {
                    datapaletteblock.getAndSetUnchecked(i1, j1, k1, biomeresolver.getNoiseBiome(i + i1, j + j1, k + k1, climate_sampler));
                }
            }
        }

        this.biomes = datapaletteblock;
    }

    public ChunkSection copy() {
        return new ChunkSection(this);
    }
}
