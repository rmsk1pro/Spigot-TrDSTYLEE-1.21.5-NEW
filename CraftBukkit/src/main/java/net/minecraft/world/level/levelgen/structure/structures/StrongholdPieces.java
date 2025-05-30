package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.BlockButtonAbstract;
import net.minecraft.world.level.block.BlockDoor;
import net.minecraft.world.level.block.BlockEnderPortalFrame;
import net.minecraft.world.level.block.BlockFence;
import net.minecraft.world.level.block.BlockIronBars;
import net.minecraft.world.level.block.BlockLadder;
import net.minecraft.world.level.block.BlockStairs;
import net.minecraft.world.level.block.BlockStepAbstract;
import net.minecraft.world.level.block.BlockTorchWall;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityMobSpawner;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockPropertySlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.storage.loot.LootTables;

public class StrongholdPieces {

    private static final int SMALL_DOOR_WIDTH = 3;
    private static final int SMALL_DOOR_HEIGHT = 3;
    private static final int MAX_DEPTH = 50;
    private static final int LOWEST_Y_POSITION = 10;
    private static final boolean CHECK_AIR = true;
    public static final int MAGIC_START_Y = 64;
    private static final StrongholdPieces.f[] STRONGHOLD_PIECE_WEIGHTS = new StrongholdPieces.f[]{new StrongholdPieces.f(StrongholdPieces.n.class, 40, 0), new StrongholdPieces.f(StrongholdPieces.h.class, 5, 5), new StrongholdPieces.f(StrongholdPieces.d.class, 20, 0), new StrongholdPieces.f(StrongholdPieces.i.class, 20, 0), new StrongholdPieces.f(StrongholdPieces.j.class, 10, 6), new StrongholdPieces.f(StrongholdPieces.o.class, 5, 5), new StrongholdPieces.f(StrongholdPieces.l.class, 5, 5), new StrongholdPieces.f(StrongholdPieces.c.class, 5, 4), new StrongholdPieces.f(StrongholdPieces.a.class, 5, 4), new StrongholdPieces.f(StrongholdPieces.e.class, 10, 2) {
                @Override
                public boolean doPlace(int i) {
                    return super.doPlace(i) && i > 4;
                }
            }, new StrongholdPieces.f(StrongholdPieces.g.class, 20, 1) {
                @Override
                public boolean doPlace(int i) {
                    return super.doPlace(i) && i > 5;
                }
            } }; // CraftBukkit - fix decompile styling
    private static List<StrongholdPieces.f> currentPieces;
    static Class<? extends StrongholdPieces.p> imposedPiece;
    private static int totalWeight;
    static final StrongholdPieces.k SMOOTH_STONE_SELECTOR = new StrongholdPieces.k();

    public StrongholdPieces() {}

    public static void resetPieces() {
        StrongholdPieces.currentPieces = Lists.newArrayList();

        for (StrongholdPieces.f strongholdpieces_f : StrongholdPieces.STRONGHOLD_PIECE_WEIGHTS) {
            strongholdpieces_f.placeCount = 0;
            StrongholdPieces.currentPieces.add(strongholdpieces_f);
        }

        StrongholdPieces.imposedPiece = null;
    }

    private static boolean updatePieceWeight() {
        boolean flag = false;

        StrongholdPieces.totalWeight = 0;

        for (StrongholdPieces.f strongholdpieces_f : StrongholdPieces.currentPieces) {
            if (strongholdpieces_f.maxPlaceCount > 0 && strongholdpieces_f.placeCount < strongholdpieces_f.maxPlaceCount) {
                flag = true;
            }

            StrongholdPieces.totalWeight += strongholdpieces_f.weight;
        }

        return flag;
    }

    private static StrongholdPieces.p findAndCreatePieceFactory(Class<? extends StrongholdPieces.p> oclass, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, @Nullable EnumDirection enumdirection, int l) {
        StrongholdPieces.p strongholdpieces_p = null;

        if (oclass == StrongholdPieces.n.class) {
            strongholdpieces_p = StrongholdPieces.n.createPiece(structurepieceaccessor, randomsource, i, j, k, enumdirection, l);
        } else if (oclass == StrongholdPieces.h.class) {
            strongholdpieces_p = StrongholdPieces.h.createPiece(structurepieceaccessor, randomsource, i, j, k, enumdirection, l);
        } else if (oclass == StrongholdPieces.d.class) {
            strongholdpieces_p = StrongholdPieces.d.createPiece(structurepieceaccessor, randomsource, i, j, k, enumdirection, l);
        } else if (oclass == StrongholdPieces.i.class) {
            strongholdpieces_p = StrongholdPieces.i.createPiece(structurepieceaccessor, randomsource, i, j, k, enumdirection, l);
        } else if (oclass == StrongholdPieces.j.class) {
            strongholdpieces_p = StrongholdPieces.j.createPiece(structurepieceaccessor, randomsource, i, j, k, enumdirection, l);
        } else if (oclass == StrongholdPieces.o.class) {
            strongholdpieces_p = StrongholdPieces.o.createPiece(structurepieceaccessor, randomsource, i, j, k, enumdirection, l);
        } else if (oclass == StrongholdPieces.l.class) {
            strongholdpieces_p = StrongholdPieces.l.createPiece(structurepieceaccessor, randomsource, i, j, k, enumdirection, l);
        } else if (oclass == StrongholdPieces.c.class) {
            strongholdpieces_p = StrongholdPieces.c.createPiece(structurepieceaccessor, randomsource, i, j, k, enumdirection, l);
        } else if (oclass == StrongholdPieces.a.class) {
            strongholdpieces_p = StrongholdPieces.a.createPiece(structurepieceaccessor, randomsource, i, j, k, enumdirection, l);
        } else if (oclass == StrongholdPieces.e.class) {
            strongholdpieces_p = StrongholdPieces.e.createPiece(structurepieceaccessor, randomsource, i, j, k, enumdirection, l);
        } else if (oclass == StrongholdPieces.g.class) {
            strongholdpieces_p = StrongholdPieces.g.createPiece(structurepieceaccessor, i, j, k, enumdirection, l);
        }

        return strongholdpieces_p;
    }

    private static StrongholdPieces.p generatePieceFromSmallDoor(StrongholdPieces.m strongholdpieces_m, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection, int l) {
        if (!updatePieceWeight()) {
            return null;
        } else {
            if (StrongholdPieces.imposedPiece != null) {
                StrongholdPieces.p strongholdpieces_p = findAndCreatePieceFactory(StrongholdPieces.imposedPiece, structurepieceaccessor, randomsource, i, j, k, enumdirection, l);

                StrongholdPieces.imposedPiece = null;
                if (strongholdpieces_p != null) {
                    return strongholdpieces_p;
                }
            }

            int i1 = 0;

            while (i1 < 5) {
                ++i1;
                int j1 = randomsource.nextInt(StrongholdPieces.totalWeight);

                for (StrongholdPieces.f strongholdpieces_f : StrongholdPieces.currentPieces) {
                    j1 -= strongholdpieces_f.weight;
                    if (j1 < 0) {
                        if (!strongholdpieces_f.doPlace(l) || strongholdpieces_f == strongholdpieces_m.previousPiece) {
                            break;
                        }

                        StrongholdPieces.p strongholdpieces_p1 = findAndCreatePieceFactory(strongholdpieces_f.pieceClass, structurepieceaccessor, randomsource, i, j, k, enumdirection, l);

                        if (strongholdpieces_p1 != null) {
                            ++strongholdpieces_f.placeCount;
                            strongholdpieces_m.previousPiece = strongholdpieces_f;
                            if (!strongholdpieces_f.isValid()) {
                                StrongholdPieces.currentPieces.remove(strongholdpieces_f);
                            }

                            return strongholdpieces_p1;
                        }
                    }
                }
            }

            StructureBoundingBox structureboundingbox = StrongholdPieces.b.findPieceBox(structurepieceaccessor, randomsource, i, j, k, enumdirection);

            if (structureboundingbox != null && structureboundingbox.minY() > 1) {
                return new StrongholdPieces.b(l, structureboundingbox, enumdirection);
            } else {
                return null;
            }
        }
    }

    static StructurePiece generateAndAddPiece(StrongholdPieces.m strongholdpieces_m, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, @Nullable EnumDirection enumdirection, int l) {
        if (l > 50) {
            return null;
        } else if (Math.abs(i - strongholdpieces_m.getBoundingBox().minX()) <= 112 && Math.abs(k - strongholdpieces_m.getBoundingBox().minZ()) <= 112) {
            StructurePiece structurepiece = generatePieceFromSmallDoor(strongholdpieces_m, structurepieceaccessor, randomsource, i, j, k, enumdirection, l + 1);

            if (structurepiece != null) {
                structurepieceaccessor.addPiece(structurepiece);
                strongholdpieces_m.pendingChildren.add(structurepiece);
            }

            return structurepiece;
        } else {
            return null;
        }
    }

    private static class f {

        public final Class<? extends StrongholdPieces.p> pieceClass;
        public final int weight;
        public int placeCount;
        public final int maxPlaceCount;

        public f(Class<? extends StrongholdPieces.p> oclass, int i, int j) {
            this.pieceClass = oclass;
            this.weight = i;
            this.maxPlaceCount = j;
        }

        public boolean doPlace(int i) {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }

        public boolean isValid() {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }
    }

    private abstract static class p extends StructurePiece {

        protected StrongholdPieces.p.a entryDoor;

        protected p(WorldGenFeatureStructurePieceType worldgenfeaturestructurepiecetype, int i, StructureBoundingBox structureboundingbox) {
            super(worldgenfeaturestructurepiecetype, i, structureboundingbox);
            this.entryDoor = StrongholdPieces.p.a.OPENING;
        }

        public p(WorldGenFeatureStructurePieceType worldgenfeaturestructurepiecetype, NBTTagCompound nbttagcompound) {
            super(worldgenfeaturestructurepiecetype, nbttagcompound);
            this.entryDoor = StrongholdPieces.p.a.OPENING;
            this.entryDoor = (StrongholdPieces.p.a) nbttagcompound.read("EntryDoor", StrongholdPieces.p.a.LEGACY_CODEC).orElseThrow();
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext structurepieceserializationcontext, NBTTagCompound nbttagcompound) {
            nbttagcompound.store("EntryDoor", StrongholdPieces.p.a.LEGACY_CODEC, this.entryDoor);
        }

        protected void generateSmallDoor(GeneratorAccessSeed generatoraccessseed, RandomSource randomsource, StructureBoundingBox structureboundingbox, StrongholdPieces.p.a strongholdpieces_p_a, int i, int j, int k) {
            switch (strongholdpieces_p_a.ordinal()) {
                case 0:
                    this.generateBox(generatoraccessseed, structureboundingbox, i, j, k, i + 3 - 1, j + 3 - 1, k, StrongholdPieces.p.CAVE_AIR, StrongholdPieces.p.CAVE_AIR, false);
                    break;
                case 1:
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i, j, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i, j + 1, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i, j + 2, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i + 1, j + 2, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i + 2, j + 2, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i + 2, j + 1, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i + 2, j, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.OAK_DOOR.defaultBlockState(), i + 1, j, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.OAK_DOOR.defaultBlockState().setValue(BlockDoor.HALF, BlockPropertyDoubleBlockHalf.UPPER), i + 1, j + 1, k, structureboundingbox);
                    break;
                case 2:
                    this.placeBlock(generatoraccessseed, Blocks.CAVE_AIR.defaultBlockState(), i + 1, j, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.CAVE_AIR.defaultBlockState(), i + 1, j + 1, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.WEST, true), i, j, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.WEST, true), i, j + 1, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.EAST, true)).setValue(BlockIronBars.WEST, true), i, j + 2, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.EAST, true)).setValue(BlockIronBars.WEST, true), i + 1, j + 2, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.EAST, true)).setValue(BlockIronBars.WEST, true), i + 2, j + 2, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.EAST, true), i + 2, j + 1, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.EAST, true), i + 2, j, k, structureboundingbox);
                    break;
                case 3:
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i, j, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i, j + 1, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i, j + 2, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i + 1, j + 2, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i + 2, j + 2, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i + 2, j + 1, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), i + 2, j, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.IRON_DOOR.defaultBlockState(), i + 1, j, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.IRON_DOOR.defaultBlockState().setValue(BlockDoor.HALF, BlockPropertyDoubleBlockHalf.UPPER), i + 1, j + 1, k, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.STONE_BUTTON.defaultBlockState().setValue(BlockButtonAbstract.FACING, EnumDirection.NORTH), i + 2, j + 1, k + 1, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.STONE_BUTTON.defaultBlockState().setValue(BlockButtonAbstract.FACING, EnumDirection.SOUTH), i + 2, j + 1, k - 1, structureboundingbox);
            }

        }

        protected StrongholdPieces.p.a randomSmallDoor(RandomSource randomsource) {
            int i = randomsource.nextInt(5);

            switch (i) {
                case 0:
                case 1:
                default:
                    return StrongholdPieces.p.a.OPENING;
                case 2:
                    return StrongholdPieces.p.a.WOOD_DOOR;
                case 3:
                    return StrongholdPieces.p.a.GRATES;
                case 4:
                    return StrongholdPieces.p.a.IRON_DOOR;
            }
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildForward(StrongholdPieces.m strongholdpieces_m, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j) {
            EnumDirection enumdirection = this.getOrientation();

            if (enumdirection != null) {
                switch (enumdirection) {
                    case NORTH:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.minX() + i, this.boundingBox.minY() + j, this.boundingBox.minZ() - 1, enumdirection, this.getGenDepth());
                    case SOUTH:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.minX() + i, this.boundingBox.minY() + j, this.boundingBox.maxZ() + 1, enumdirection, this.getGenDepth());
                    case WEST:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.minX() - 1, this.boundingBox.minY() + j, this.boundingBox.minZ() + i, enumdirection, this.getGenDepth());
                    case EAST:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.maxX() + 1, this.boundingBox.minY() + j, this.boundingBox.minZ() + i, enumdirection, this.getGenDepth());
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildLeft(StrongholdPieces.m strongholdpieces_m, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j) {
            EnumDirection enumdirection = this.getOrientation();

            if (enumdirection != null) {
                switch (enumdirection) {
                    case NORTH:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.minX() - 1, this.boundingBox.minY() + i, this.boundingBox.minZ() + j, EnumDirection.WEST, this.getGenDepth());
                    case SOUTH:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.minX() - 1, this.boundingBox.minY() + i, this.boundingBox.minZ() + j, EnumDirection.WEST, this.getGenDepth());
                    case WEST:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.minX() + j, this.boundingBox.minY() + i, this.boundingBox.minZ() - 1, EnumDirection.NORTH, this.getGenDepth());
                    case EAST:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.minX() + j, this.boundingBox.minY() + i, this.boundingBox.minZ() - 1, EnumDirection.NORTH, this.getGenDepth());
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildRight(StrongholdPieces.m strongholdpieces_m, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j) {
            EnumDirection enumdirection = this.getOrientation();

            if (enumdirection != null) {
                switch (enumdirection) {
                    case NORTH:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.maxX() + 1, this.boundingBox.minY() + i, this.boundingBox.minZ() + j, EnumDirection.EAST, this.getGenDepth());
                    case SOUTH:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.maxX() + 1, this.boundingBox.minY() + i, this.boundingBox.minZ() + j, EnumDirection.EAST, this.getGenDepth());
                    case WEST:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.minX() + j, this.boundingBox.minY() + i, this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, this.getGenDepth());
                    case EAST:
                        return StrongholdPieces.generateAndAddPiece(strongholdpieces_m, structurepieceaccessor, randomsource, this.boundingBox.minX() + j, this.boundingBox.minY() + i, this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, this.getGenDepth());
                }
            }

            return null;
        }

        protected static boolean isOkBox(StructureBoundingBox structureboundingbox) {
            return structureboundingbox != null && structureboundingbox.minY() > 10;
        }

        protected static enum a {

            OPENING, WOOD_DOOR, GRATES, IRON_DOOR;

            /** @deprecated */
            @Deprecated
            public static final Codec<StrongholdPieces.p.a> LEGACY_CODEC = ExtraCodecs.<StrongholdPieces.p.a>legacyEnum(StrongholdPieces.p.a::valueOf);

            private a() {}
        }
    }

    public static class b extends StrongholdPieces.p {

        private final int steps;

        public b(int i, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_FILLER_CORRIDOR, i, structureboundingbox);
            this.setOrientation(enumdirection);
            this.steps = enumdirection != EnumDirection.NORTH && enumdirection != EnumDirection.SOUTH ? structureboundingbox.getXSpan() : structureboundingbox.getZSpan();
        }

        public b(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_FILLER_CORRIDOR, nbttagcompound);
            this.steps = nbttagcompound.getIntOr("Steps", 0);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext structurepieceserializationcontext, NBTTagCompound nbttagcompound) {
            super.addAdditionalSaveData(structurepieceserializationcontext, nbttagcompound);
            nbttagcompound.putInt("Steps", this.steps);
        }

        public static StructureBoundingBox findPieceBox(StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection) {
            int l = 3;
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -1, -1, 0, 5, 5, 4, enumdirection);
            StructurePiece structurepiece = structurepieceaccessor.findCollisionPiece(structureboundingbox);

            if (structurepiece == null) {
                return null;
            } else {
                if (structurepiece.getBoundingBox().minY() == structureboundingbox.minY()) {
                    for (int i1 = 2; i1 >= 1; --i1) {
                        structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -1, -1, 0, 5, 5, i1, enumdirection);
                        if (!structurepiece.getBoundingBox().intersects(structureboundingbox)) {
                            return StructureBoundingBox.orientBox(i, j, k, -1, -1, 0, 5, 5, i1 + 1, enumdirection);
                        }
                    }
                }

                return null;
            }
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            for (int i = 0; i < this.steps; ++i) {
                this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 0, 0, i, structureboundingbox);
                this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 1, 0, i, structureboundingbox);
                this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 2, 0, i, structureboundingbox);
                this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 3, 0, i, structureboundingbox);
                this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 4, 0, i, structureboundingbox);

                for (int j = 1; j <= 3; ++j) {
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 0, j, i, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.CAVE_AIR.defaultBlockState(), 1, j, i, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.CAVE_AIR.defaultBlockState(), 2, j, i, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.CAVE_AIR.defaultBlockState(), 3, j, i, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 4, j, i, structureboundingbox);
                }

                this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 0, 4, i, structureboundingbox);
                this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, i, structureboundingbox);
                this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, i, structureboundingbox);
                this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 3, 4, i, structureboundingbox);
                this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 4, 4, i, structureboundingbox);
            }

        }
    }

    public static class l extends StrongholdPieces.p {

        private static final int WIDTH = 5;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 5;
        private final boolean isSource;

        public l(WorldGenFeatureStructurePieceType worldgenfeaturestructurepiecetype, int i, int j, int k, EnumDirection enumdirection) {
            super(worldgenfeaturestructurepiecetype, i, makeBoundingBox(j, 64, k, enumdirection, 5, 11, 5));
            this.isSource = true;
            this.setOrientation(enumdirection);
            this.entryDoor = StrongholdPieces.p.a.OPENING;
        }

        public l(int i, RandomSource randomsource, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_STAIRS_DOWN, i, structureboundingbox);
            this.isSource = false;
            this.setOrientation(enumdirection);
            this.entryDoor = this.randomSmallDoor(randomsource);
        }

        public l(WorldGenFeatureStructurePieceType worldgenfeaturestructurepiecetype, NBTTagCompound nbttagcompound) {
            super(worldgenfeaturestructurepiecetype, nbttagcompound);
            this.isSource = nbttagcompound.getBooleanOr("Source", false);
        }

        public l(NBTTagCompound nbttagcompound) {
            this(WorldGenFeatureStructurePieceType.STRONGHOLD_STAIRS_DOWN, nbttagcompound);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext structurepieceserializationcontext, NBTTagCompound nbttagcompound) {
            super.addAdditionalSaveData(structurepieceserializationcontext, nbttagcompound);
            nbttagcompound.putBoolean("Source", this.isSource);
        }

        @Override
        public void addChildren(StructurePiece structurepiece, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource) {
            if (this.isSource) {
                StrongholdPieces.imposedPiece = StrongholdPieces.c.class;
            }

            this.generateSmallDoorChildForward((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 1);
        }

        public static StrongholdPieces.l createPiece(StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection, int l) {
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -1, -7, 0, 5, 11, 5, enumdirection);

            return isOkBox(structureboundingbox) && structurepieceaccessor.findCollisionPiece(structureboundingbox) == null ? new StrongholdPieces.l(l, randomsource, structureboundingbox, enumdirection) : null;
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            this.generateBox(generatoraccessseed, structureboundingbox, 0, 0, 0, 4, 10, 4, true, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, this.entryDoor, 1, 7, 0);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, StrongholdPieces.p.a.OPENING, 1, 1, 4);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 2, 6, 1, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 1, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 6, 1, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 2, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, 3, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 5, 3, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, 3, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 3, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 4, 3, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 2, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 3, 2, 1, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 3, 1, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 2, 2, 1, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 1, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 2, 1, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 2, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 1, 3, structureboundingbox);
        }
    }

    public static class m extends StrongholdPieces.l {

        public StrongholdPieces.f previousPiece;
        @Nullable
        public StrongholdPieces.g portalRoomPiece;
        public final List<StructurePiece> pendingChildren = Lists.newArrayList();

        public m(RandomSource randomsource, int i, int j) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_START, 0, i, j, getRandomHorizontalDirection(randomsource));
        }

        public m(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_START, nbttagcompound);
        }

        @Override
        public BlockPosition getLocatorPosition() {
            return this.portalRoomPiece != null ? this.portalRoomPiece.getLocatorPosition() : super.getLocatorPosition();
        }
    }

    public static class n extends StrongholdPieces.p {

        private static final int WIDTH = 5;
        private static final int HEIGHT = 5;
        private static final int DEPTH = 7;
        private final boolean leftChild;
        private final boolean rightChild;

        public n(int i, RandomSource randomsource, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_STRAIGHT, i, structureboundingbox);
            this.setOrientation(enumdirection);
            this.entryDoor = this.randomSmallDoor(randomsource);
            this.leftChild = randomsource.nextInt(2) == 0;
            this.rightChild = randomsource.nextInt(2) == 0;
        }

        public n(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_STRAIGHT, nbttagcompound);
            this.leftChild = nbttagcompound.getBooleanOr("Left", false);
            this.rightChild = nbttagcompound.getBooleanOr("Right", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext structurepieceserializationcontext, NBTTagCompound nbttagcompound) {
            super.addAdditionalSaveData(structurepieceserializationcontext, nbttagcompound);
            nbttagcompound.putBoolean("Left", this.leftChild);
            nbttagcompound.putBoolean("Right", this.rightChild);
        }

        @Override
        public void addChildren(StructurePiece structurepiece, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource) {
            this.generateSmallDoorChildForward((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 1);
            if (this.leftChild) {
                this.generateSmallDoorChildLeft((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 2);
            }

            if (this.rightChild) {
                this.generateSmallDoorChildRight((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 2);
            }

        }

        public static StrongholdPieces.n createPiece(StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection, int l) {
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -1, -1, 0, 5, 5, 7, enumdirection);

            return isOkBox(structureboundingbox) && structurepieceaccessor.findCollisionPiece(structureboundingbox) == null ? new StrongholdPieces.n(l, randomsource, structureboundingbox, enumdirection) : null;
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            this.generateBox(generatoraccessseed, structureboundingbox, 0, 0, 0, 4, 4, 6, true, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, this.entryDoor, 1, 1, 0);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, StrongholdPieces.p.a.OPENING, 1, 1, 6);
            IBlockData iblockdata = (IBlockData) Blocks.WALL_TORCH.defaultBlockState().setValue(BlockTorchWall.FACING, EnumDirection.EAST);
            IBlockData iblockdata1 = (IBlockData) Blocks.WALL_TORCH.defaultBlockState().setValue(BlockTorchWall.FACING, EnumDirection.WEST);

            this.maybeGenerateBlock(generatoraccessseed, structureboundingbox, randomsource, 0.1F, 1, 2, 1, iblockdata);
            this.maybeGenerateBlock(generatoraccessseed, structureboundingbox, randomsource, 0.1F, 3, 2, 1, iblockdata1);
            this.maybeGenerateBlock(generatoraccessseed, structureboundingbox, randomsource, 0.1F, 1, 2, 5, iblockdata);
            this.maybeGenerateBlock(generatoraccessseed, structureboundingbox, randomsource, 0.1F, 3, 2, 5, iblockdata1);
            if (this.leftChild) {
                this.generateBox(generatoraccessseed, structureboundingbox, 0, 1, 2, 0, 3, 4, StrongholdPieces.n.CAVE_AIR, StrongholdPieces.n.CAVE_AIR, false);
            }

            if (this.rightChild) {
                this.generateBox(generatoraccessseed, structureboundingbox, 4, 1, 2, 4, 3, 4, StrongholdPieces.n.CAVE_AIR, StrongholdPieces.n.CAVE_AIR, false);
            }

        }
    }

    public static class a extends StrongholdPieces.p {

        private static final int WIDTH = 5;
        private static final int HEIGHT = 5;
        private static final int DEPTH = 7;
        private boolean hasPlacedChest;

        public a(int i, RandomSource randomsource, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_CHEST_CORRIDOR, i, structureboundingbox);
            this.setOrientation(enumdirection);
            this.entryDoor = this.randomSmallDoor(randomsource);
        }

        public a(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_CHEST_CORRIDOR, nbttagcompound);
            this.hasPlacedChest = nbttagcompound.getBooleanOr("Chest", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext structurepieceserializationcontext, NBTTagCompound nbttagcompound) {
            super.addAdditionalSaveData(structurepieceserializationcontext, nbttagcompound);
            nbttagcompound.putBoolean("Chest", this.hasPlacedChest);
        }

        @Override
        public void addChildren(StructurePiece structurepiece, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource) {
            this.generateSmallDoorChildForward((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 1);
        }

        public static StrongholdPieces.a createPiece(StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection, int l) {
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -1, -1, 0, 5, 5, 7, enumdirection);

            return isOkBox(structureboundingbox) && structurepieceaccessor.findCollisionPiece(structureboundingbox) == null ? new StrongholdPieces.a(l, randomsource, structureboundingbox, enumdirection) : null;
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            this.generateBox(generatoraccessseed, structureboundingbox, 0, 0, 0, 4, 4, 6, true, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, this.entryDoor, 1, 1, 0);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, StrongholdPieces.p.a.OPENING, 1, 1, 6);
            this.generateBox(generatoraccessseed, structureboundingbox, 3, 1, 2, 3, 1, 4, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), false);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 1, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 5, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 2, structureboundingbox);
            this.placeBlock(generatoraccessseed, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 4, structureboundingbox);

            for (int i = 2; i <= 4; ++i) {
                this.placeBlock(generatoraccessseed, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2, 1, i, structureboundingbox);
            }

            if (!this.hasPlacedChest && structureboundingbox.isInside(this.getWorldPos(3, 2, 3))) {
                this.hasPlacedChest = true;
                this.createChest(generatoraccessseed, structureboundingbox, randomsource, 3, 2, 3, LootTables.STRONGHOLD_CORRIDOR);
            }

        }
    }

    public static class o extends StrongholdPieces.p {

        private static final int WIDTH = 5;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 8;

        public o(int i, RandomSource randomsource, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, i, structureboundingbox);
            this.setOrientation(enumdirection);
            this.entryDoor = this.randomSmallDoor(randomsource);
        }

        public o(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, nbttagcompound);
        }

        @Override
        public void addChildren(StructurePiece structurepiece, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource) {
            this.generateSmallDoorChildForward((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 1);
        }

        public static StrongholdPieces.o createPiece(StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection, int l) {
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -1, -7, 0, 5, 11, 8, enumdirection);

            return isOkBox(structureboundingbox) && structurepieceaccessor.findCollisionPiece(structureboundingbox) == null ? new StrongholdPieces.o(l, randomsource, structureboundingbox, enumdirection) : null;
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            this.generateBox(generatoraccessseed, structureboundingbox, 0, 0, 0, 4, 10, 7, true, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, this.entryDoor, 1, 7, 0);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, StrongholdPieces.p.a.OPENING, 1, 1, 7);
            IBlockData iblockdata = (IBlockData) Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(BlockStairs.FACING, EnumDirection.SOUTH);

            for (int i = 0; i < 6; ++i) {
                this.placeBlock(generatoraccessseed, iblockdata, 1, 6 - i, 1 + i, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata, 2, 6 - i, 1 + i, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata, 3, 6 - i, 1 + i, structureboundingbox);
                if (i < 5) {
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5 - i, 1 + i, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 2, 5 - i, 1 + i, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 3, 5 - i, 1 + i, structureboundingbox);
                }
            }

        }
    }

    public abstract static class q extends StrongholdPieces.p {

        protected static final int WIDTH = 5;
        protected static final int HEIGHT = 5;
        protected static final int DEPTH = 5;

        protected q(WorldGenFeatureStructurePieceType worldgenfeaturestructurepiecetype, int i, StructureBoundingBox structureboundingbox) {
            super(worldgenfeaturestructurepiecetype, i, structureboundingbox);
        }

        public q(WorldGenFeatureStructurePieceType worldgenfeaturestructurepiecetype, NBTTagCompound nbttagcompound) {
            super(worldgenfeaturestructurepiecetype, nbttagcompound);
        }
    }

    public static class d extends StrongholdPieces.q {

        public d(int i, RandomSource randomsource, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_LEFT_TURN, i, structureboundingbox);
            this.setOrientation(enumdirection);
            this.entryDoor = this.randomSmallDoor(randomsource);
        }

        public d(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_LEFT_TURN, nbttagcompound);
        }

        @Override
        public void addChildren(StructurePiece structurepiece, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource) {
            EnumDirection enumdirection = this.getOrientation();

            if (enumdirection != EnumDirection.NORTH && enumdirection != EnumDirection.EAST) {
                this.generateSmallDoorChildRight((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 1);
            } else {
                this.generateSmallDoorChildLeft((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 1);
            }

        }

        public static StrongholdPieces.d createPiece(StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection, int l) {
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -1, -1, 0, 5, 5, 5, enumdirection);

            return isOkBox(structureboundingbox) && structurepieceaccessor.findCollisionPiece(structureboundingbox) == null ? new StrongholdPieces.d(l, randomsource, structureboundingbox, enumdirection) : null;
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            this.generateBox(generatoraccessseed, structureboundingbox, 0, 0, 0, 4, 4, 4, true, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, this.entryDoor, 1, 1, 0);
            EnumDirection enumdirection = this.getOrientation();

            if (enumdirection != EnumDirection.NORTH && enumdirection != EnumDirection.EAST) {
                this.generateBox(generatoraccessseed, structureboundingbox, 4, 1, 1, 4, 3, 3, StrongholdPieces.d.CAVE_AIR, StrongholdPieces.d.CAVE_AIR, false);
            } else {
                this.generateBox(generatoraccessseed, structureboundingbox, 0, 1, 1, 0, 3, 3, StrongholdPieces.d.CAVE_AIR, StrongholdPieces.d.CAVE_AIR, false);
            }

        }
    }

    public static class i extends StrongholdPieces.q {

        public i(int i, RandomSource randomsource, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_RIGHT_TURN, i, structureboundingbox);
            this.setOrientation(enumdirection);
            this.entryDoor = this.randomSmallDoor(randomsource);
        }

        public i(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_RIGHT_TURN, nbttagcompound);
        }

        @Override
        public void addChildren(StructurePiece structurepiece, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource) {
            EnumDirection enumdirection = this.getOrientation();

            if (enumdirection != EnumDirection.NORTH && enumdirection != EnumDirection.EAST) {
                this.generateSmallDoorChildLeft((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 1);
            } else {
                this.generateSmallDoorChildRight((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 1);
            }

        }

        public static StrongholdPieces.i createPiece(StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection, int l) {
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -1, -1, 0, 5, 5, 5, enumdirection);

            return isOkBox(structureboundingbox) && structurepieceaccessor.findCollisionPiece(structureboundingbox) == null ? new StrongholdPieces.i(l, randomsource, structureboundingbox, enumdirection) : null;
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            this.generateBox(generatoraccessseed, structureboundingbox, 0, 0, 0, 4, 4, 4, true, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, this.entryDoor, 1, 1, 0);
            EnumDirection enumdirection = this.getOrientation();

            if (enumdirection != EnumDirection.NORTH && enumdirection != EnumDirection.EAST) {
                this.generateBox(generatoraccessseed, structureboundingbox, 0, 1, 1, 0, 3, 3, StrongholdPieces.i.CAVE_AIR, StrongholdPieces.i.CAVE_AIR, false);
            } else {
                this.generateBox(generatoraccessseed, structureboundingbox, 4, 1, 1, 4, 3, 3, StrongholdPieces.i.CAVE_AIR, StrongholdPieces.i.CAVE_AIR, false);
            }

        }
    }

    public static class j extends StrongholdPieces.p {

        protected static final int WIDTH = 11;
        protected static final int HEIGHT = 7;
        protected static final int DEPTH = 11;
        protected final int type;

        public j(int i, RandomSource randomsource, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_ROOM_CROSSING, i, structureboundingbox);
            this.setOrientation(enumdirection);
            this.entryDoor = this.randomSmallDoor(randomsource);
            this.type = randomsource.nextInt(5);
        }

        public j(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_ROOM_CROSSING, nbttagcompound);
            this.type = nbttagcompound.getIntOr("Type", 0);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext structurepieceserializationcontext, NBTTagCompound nbttagcompound) {
            super.addAdditionalSaveData(structurepieceserializationcontext, nbttagcompound);
            nbttagcompound.putInt("Type", this.type);
        }

        @Override
        public void addChildren(StructurePiece structurepiece, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource) {
            this.generateSmallDoorChildForward((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 4, 1);
            this.generateSmallDoorChildLeft((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 4);
            this.generateSmallDoorChildRight((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 4);
        }

        public static StrongholdPieces.j createPiece(StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection, int l) {
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -4, -1, 0, 11, 7, 11, enumdirection);

            return isOkBox(structureboundingbox) && structurepieceaccessor.findCollisionPiece(structureboundingbox) == null ? new StrongholdPieces.j(l, randomsource, structureboundingbox, enumdirection) : null;
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            this.generateBox(generatoraccessseed, structureboundingbox, 0, 0, 0, 10, 6, 10, true, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, this.entryDoor, 4, 1, 0);
            this.generateBox(generatoraccessseed, structureboundingbox, 4, 1, 10, 6, 3, 10, StrongholdPieces.j.CAVE_AIR, StrongholdPieces.j.CAVE_AIR, false);
            this.generateBox(generatoraccessseed, structureboundingbox, 0, 1, 4, 0, 3, 6, StrongholdPieces.j.CAVE_AIR, StrongholdPieces.j.CAVE_AIR, false);
            this.generateBox(generatoraccessseed, structureboundingbox, 10, 1, 4, 10, 3, 6, StrongholdPieces.j.CAVE_AIR, StrongholdPieces.j.CAVE_AIR, false);
            switch (this.type) {
                case 0:
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.WALL_TORCH.defaultBlockState().setValue(BlockTorchWall.FACING, EnumDirection.WEST), 4, 3, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.WALL_TORCH.defaultBlockState().setValue(BlockTorchWall.FACING, EnumDirection.EAST), 6, 3, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.WALL_TORCH.defaultBlockState().setValue(BlockTorchWall.FACING, EnumDirection.SOUTH), 5, 3, 4, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.WALL_TORCH.defaultBlockState().setValue(BlockTorchWall.FACING, EnumDirection.NORTH), 5, 3, 6, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 4, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 6, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 4, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 6, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 4, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 6, structureboundingbox);
                    break;
                case 1:
                    for (int i = 0; i < 5; ++i) {
                        this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 3, 1, 3 + i, structureboundingbox);
                        this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 7, 1, 3 + i, structureboundingbox);
                        this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 3 + i, 1, 3, structureboundingbox);
                        this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 3 + i, 1, 7, structureboundingbox);
                    }

                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.WATER.defaultBlockState(), 5, 4, 5, structureboundingbox);
                    break;
                case 2:
                    for (int j = 1; j <= 9; ++j) {
                        this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 1, 3, j, structureboundingbox);
                        this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 9, 3, j, structureboundingbox);
                    }

                    for (int k = 1; k <= 9; ++k) {
                        this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), k, 3, 1, structureboundingbox);
                        this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), k, 3, 9, structureboundingbox);
                    }

                    this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 4, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 6, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 4, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 6, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 4, 1, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 6, 1, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 4, 3, 5, structureboundingbox);
                    this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 6, 3, 5, structureboundingbox);

                    for (int l = 1; l <= 3; ++l) {
                        this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 4, l, 4, structureboundingbox);
                        this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 6, l, 4, structureboundingbox);
                        this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 4, l, 6, structureboundingbox);
                        this.placeBlock(generatoraccessseed, Blocks.COBBLESTONE.defaultBlockState(), 6, l, 6, structureboundingbox);
                    }

                    this.placeBlock(generatoraccessseed, Blocks.WALL_TORCH.defaultBlockState(), 5, 3, 5, structureboundingbox);

                    for (int i1 = 2; i1 <= 8; ++i1) {
                        this.placeBlock(generatoraccessseed, Blocks.OAK_PLANKS.defaultBlockState(), 2, 3, i1, structureboundingbox);
                        this.placeBlock(generatoraccessseed, Blocks.OAK_PLANKS.defaultBlockState(), 3, 3, i1, structureboundingbox);
                        if (i1 <= 3 || i1 >= 7) {
                            this.placeBlock(generatoraccessseed, Blocks.OAK_PLANKS.defaultBlockState(), 4, 3, i1, structureboundingbox);
                            this.placeBlock(generatoraccessseed, Blocks.OAK_PLANKS.defaultBlockState(), 5, 3, i1, structureboundingbox);
                            this.placeBlock(generatoraccessseed, Blocks.OAK_PLANKS.defaultBlockState(), 6, 3, i1, structureboundingbox);
                        }

                        this.placeBlock(generatoraccessseed, Blocks.OAK_PLANKS.defaultBlockState(), 7, 3, i1, structureboundingbox);
                        this.placeBlock(generatoraccessseed, Blocks.OAK_PLANKS.defaultBlockState(), 8, 3, i1, structureboundingbox);
                    }

                    IBlockData iblockdata = (IBlockData) Blocks.LADDER.defaultBlockState().setValue(BlockLadder.FACING, EnumDirection.WEST);

                    this.placeBlock(generatoraccessseed, iblockdata, 9, 1, 3, structureboundingbox);
                    this.placeBlock(generatoraccessseed, iblockdata, 9, 2, 3, structureboundingbox);
                    this.placeBlock(generatoraccessseed, iblockdata, 9, 3, 3, structureboundingbox);
                    this.createChest(generatoraccessseed, structureboundingbox, randomsource, 3, 4, 8, LootTables.STRONGHOLD_CROSSING);
            }

        }
    }

    public static class h extends StrongholdPieces.p {

        protected static final int WIDTH = 9;
        protected static final int HEIGHT = 5;
        protected static final int DEPTH = 11;

        public h(int i, RandomSource randomsource, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_PRISON_HALL, i, structureboundingbox);
            this.setOrientation(enumdirection);
            this.entryDoor = this.randomSmallDoor(randomsource);
        }

        public h(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_PRISON_HALL, nbttagcompound);
        }

        @Override
        public void addChildren(StructurePiece structurepiece, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource) {
            this.generateSmallDoorChildForward((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 1, 1);
        }

        public static StrongholdPieces.h createPiece(StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection, int l) {
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -1, -1, 0, 9, 5, 11, enumdirection);

            return isOkBox(structureboundingbox) && structurepieceaccessor.findCollisionPiece(structureboundingbox) == null ? new StrongholdPieces.h(l, randomsource, structureboundingbox, enumdirection) : null;
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            this.generateBox(generatoraccessseed, structureboundingbox, 0, 0, 0, 8, 4, 10, true, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, this.entryDoor, 1, 1, 0);
            this.generateBox(generatoraccessseed, structureboundingbox, 1, 1, 10, 3, 3, 10, StrongholdPieces.h.CAVE_AIR, StrongholdPieces.h.CAVE_AIR, false);
            this.generateBox(generatoraccessseed, structureboundingbox, 4, 1, 1, 4, 3, 1, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 4, 1, 3, 4, 3, 3, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 4, 1, 7, 4, 3, 7, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 4, 1, 9, 4, 3, 9, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);

            for (int i = 1; i <= 3; ++i) {
                this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.NORTH, true)).setValue(BlockIronBars.SOUTH, true), 4, i, 4, structureboundingbox);
                this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.NORTH, true)).setValue(BlockIronBars.SOUTH, true)).setValue(BlockIronBars.EAST, true), 4, i, 5, structureboundingbox);
                this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.NORTH, true)).setValue(BlockIronBars.SOUTH, true), 4, i, 6, structureboundingbox);
                this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.WEST, true)).setValue(BlockIronBars.EAST, true), 5, i, 5, structureboundingbox);
                this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.WEST, true)).setValue(BlockIronBars.EAST, true), 6, i, 5, structureboundingbox);
                this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.WEST, true)).setValue(BlockIronBars.EAST, true), 7, i, 5, structureboundingbox);
            }

            this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.NORTH, true)).setValue(BlockIronBars.SOUTH, true), 4, 3, 2, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.NORTH, true)).setValue(BlockIronBars.SOUTH, true), 4, 3, 8, structureboundingbox);
            IBlockData iblockdata = (IBlockData) Blocks.IRON_DOOR.defaultBlockState().setValue(BlockDoor.FACING, EnumDirection.WEST);
            IBlockData iblockdata1 = (IBlockData) ((IBlockData) Blocks.IRON_DOOR.defaultBlockState().setValue(BlockDoor.FACING, EnumDirection.WEST)).setValue(BlockDoor.HALF, BlockPropertyDoubleBlockHalf.UPPER);

            this.placeBlock(generatoraccessseed, iblockdata, 4, 1, 2, structureboundingbox);
            this.placeBlock(generatoraccessseed, iblockdata1, 4, 2, 2, structureboundingbox);
            this.placeBlock(generatoraccessseed, iblockdata, 4, 1, 8, structureboundingbox);
            this.placeBlock(generatoraccessseed, iblockdata1, 4, 2, 8, structureboundingbox);
        }
    }

    public static class e extends StrongholdPieces.p {

        protected static final int WIDTH = 14;
        protected static final int HEIGHT = 6;
        protected static final int TALL_HEIGHT = 11;
        protected static final int DEPTH = 15;
        private final boolean isTall;

        public e(int i, RandomSource randomsource, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_LIBRARY, i, structureboundingbox);
            this.setOrientation(enumdirection);
            this.entryDoor = this.randomSmallDoor(randomsource);
            this.isTall = structureboundingbox.getYSpan() > 6;
        }

        public e(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_LIBRARY, nbttagcompound);
            this.isTall = nbttagcompound.getBooleanOr("Tall", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext structurepieceserializationcontext, NBTTagCompound nbttagcompound) {
            super.addAdditionalSaveData(structurepieceserializationcontext, nbttagcompound);
            nbttagcompound.putBoolean("Tall", this.isTall);
        }

        public static StrongholdPieces.e createPiece(StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection, int l) {
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -4, -1, 0, 14, 11, 15, enumdirection);

            if (!isOkBox(structureboundingbox) || structurepieceaccessor.findCollisionPiece(structureboundingbox) != null) {
                structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -4, -1, 0, 14, 6, 15, enumdirection);
                if (!isOkBox(structureboundingbox) || structurepieceaccessor.findCollisionPiece(structureboundingbox) != null) {
                    return null;
                }
            }

            return new StrongholdPieces.e(l, randomsource, structureboundingbox, enumdirection);
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            int i = 11;

            if (!this.isTall) {
                i = 6;
            }

            this.generateBox(generatoraccessseed, structureboundingbox, 0, 0, 0, 13, i - 1, 14, true, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, this.entryDoor, 4, 1, 0);
            this.generateMaybeBox(generatoraccessseed, structureboundingbox, randomsource, 0.07F, 2, 1, 1, 11, 4, 13, Blocks.COBWEB.defaultBlockState(), Blocks.COBWEB.defaultBlockState(), false, false);
            int j = 1;
            int k = 12;

            for (int l = 1; l <= 13; ++l) {
                if ((l - 1) % 4 == 0) {
                    this.generateBox(generatoraccessseed, structureboundingbox, 1, 1, l, 1, 4, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                    this.generateBox(generatoraccessseed, structureboundingbox, 12, 1, l, 12, 4, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.WALL_TORCH.defaultBlockState().setValue(BlockTorchWall.FACING, EnumDirection.EAST), 2, 3, l, structureboundingbox);
                    this.placeBlock(generatoraccessseed, (IBlockData) Blocks.WALL_TORCH.defaultBlockState().setValue(BlockTorchWall.FACING, EnumDirection.WEST), 11, 3, l, structureboundingbox);
                    if (this.isTall) {
                        this.generateBox(generatoraccessseed, structureboundingbox, 1, 6, l, 1, 9, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                        this.generateBox(generatoraccessseed, structureboundingbox, 12, 6, l, 12, 9, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                    }
                } else {
                    this.generateBox(generatoraccessseed, structureboundingbox, 1, 1, l, 1, 4, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                    this.generateBox(generatoraccessseed, structureboundingbox, 12, 1, l, 12, 4, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                    if (this.isTall) {
                        this.generateBox(generatoraccessseed, structureboundingbox, 1, 6, l, 1, 9, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                        this.generateBox(generatoraccessseed, structureboundingbox, 12, 6, l, 12, 9, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                    }
                }
            }

            for (int i1 = 3; i1 < 12; i1 += 2) {
                this.generateBox(generatoraccessseed, structureboundingbox, 3, 1, i1, 4, 3, i1, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                this.generateBox(generatoraccessseed, structureboundingbox, 6, 1, i1, 7, 3, i1, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                this.generateBox(generatoraccessseed, structureboundingbox, 9, 1, i1, 10, 3, i1, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
            }

            if (this.isTall) {
                this.generateBox(generatoraccessseed, structureboundingbox, 1, 5, 1, 3, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.generateBox(generatoraccessseed, structureboundingbox, 10, 5, 1, 12, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.generateBox(generatoraccessseed, structureboundingbox, 4, 5, 1, 9, 5, 2, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.generateBox(generatoraccessseed, structureboundingbox, 4, 5, 12, 9, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.placeBlock(generatoraccessseed, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 11, structureboundingbox);
                this.placeBlock(generatoraccessseed, Blocks.OAK_PLANKS.defaultBlockState(), 8, 5, 11, structureboundingbox);
                this.placeBlock(generatoraccessseed, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 10, structureboundingbox);
                IBlockData iblockdata = (IBlockData) ((IBlockData) Blocks.OAK_FENCE.defaultBlockState().setValue(BlockFence.WEST, true)).setValue(BlockFence.EAST, true);
                IBlockData iblockdata1 = (IBlockData) ((IBlockData) Blocks.OAK_FENCE.defaultBlockState().setValue(BlockFence.NORTH, true)).setValue(BlockFence.SOUTH, true);

                this.generateBox(generatoraccessseed, structureboundingbox, 3, 6, 3, 3, 6, 11, iblockdata1, iblockdata1, false);
                this.generateBox(generatoraccessseed, structureboundingbox, 10, 6, 3, 10, 6, 9, iblockdata1, iblockdata1, false);
                this.generateBox(generatoraccessseed, structureboundingbox, 4, 6, 2, 9, 6, 2, iblockdata, iblockdata, false);
                this.generateBox(generatoraccessseed, structureboundingbox, 4, 6, 12, 7, 6, 12, iblockdata, iblockdata, false);
                this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.OAK_FENCE.defaultBlockState().setValue(BlockFence.NORTH, true)).setValue(BlockFence.EAST, true), 3, 6, 2, structureboundingbox);
                this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.OAK_FENCE.defaultBlockState().setValue(BlockFence.SOUTH, true)).setValue(BlockFence.EAST, true), 3, 6, 12, structureboundingbox);
                this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.OAK_FENCE.defaultBlockState().setValue(BlockFence.NORTH, true)).setValue(BlockFence.WEST, true), 10, 6, 2, structureboundingbox);

                for (int j1 = 0; j1 <= 2; ++j1) {
                    this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.OAK_FENCE.defaultBlockState().setValue(BlockFence.SOUTH, true)).setValue(BlockFence.WEST, true), 8 + j1, 6, 12 - j1, structureboundingbox);
                    if (j1 != 2) {
                        this.placeBlock(generatoraccessseed, (IBlockData) ((IBlockData) Blocks.OAK_FENCE.defaultBlockState().setValue(BlockFence.NORTH, true)).setValue(BlockFence.EAST, true), 8 + j1, 6, 11 - j1, structureboundingbox);
                    }
                }

                IBlockData iblockdata2 = (IBlockData) Blocks.LADDER.defaultBlockState().setValue(BlockLadder.FACING, EnumDirection.SOUTH);

                this.placeBlock(generatoraccessseed, iblockdata2, 10, 1, 13, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata2, 10, 2, 13, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata2, 10, 3, 13, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata2, 10, 4, 13, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata2, 10, 5, 13, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata2, 10, 6, 13, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata2, 10, 7, 13, structureboundingbox);
                int k1 = 7;
                int l1 = 7;
                IBlockData iblockdata3 = (IBlockData) Blocks.OAK_FENCE.defaultBlockState().setValue(BlockFence.EAST, true);

                this.placeBlock(generatoraccessseed, iblockdata3, 6, 9, 7, structureboundingbox);
                IBlockData iblockdata4 = (IBlockData) Blocks.OAK_FENCE.defaultBlockState().setValue(BlockFence.WEST, true);

                this.placeBlock(generatoraccessseed, iblockdata4, 7, 9, 7, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata3, 6, 8, 7, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata4, 7, 8, 7, structureboundingbox);
                IBlockData iblockdata5 = (IBlockData) ((IBlockData) iblockdata1.setValue(BlockFence.WEST, true)).setValue(BlockFence.EAST, true);

                this.placeBlock(generatoraccessseed, iblockdata5, 6, 7, 7, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata5, 7, 7, 7, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata3, 5, 7, 7, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata4, 8, 7, 7, structureboundingbox);
                this.placeBlock(generatoraccessseed, (IBlockData) iblockdata3.setValue(BlockFence.NORTH, true), 6, 7, 6, structureboundingbox);
                this.placeBlock(generatoraccessseed, (IBlockData) iblockdata3.setValue(BlockFence.SOUTH, true), 6, 7, 8, structureboundingbox);
                this.placeBlock(generatoraccessseed, (IBlockData) iblockdata4.setValue(BlockFence.NORTH, true), 7, 7, 6, structureboundingbox);
                this.placeBlock(generatoraccessseed, (IBlockData) iblockdata4.setValue(BlockFence.SOUTH, true), 7, 7, 8, structureboundingbox);
                IBlockData iblockdata6 = Blocks.TORCH.defaultBlockState();

                this.placeBlock(generatoraccessseed, iblockdata6, 5, 8, 7, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata6, 8, 8, 7, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata6, 6, 8, 6, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata6, 6, 8, 8, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata6, 7, 8, 6, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata6, 7, 8, 8, structureboundingbox);
            }

            this.createChest(generatoraccessseed, structureboundingbox, randomsource, 3, 3, 5, LootTables.STRONGHOLD_LIBRARY);
            if (this.isTall) {
                this.placeBlock(generatoraccessseed, StrongholdPieces.e.CAVE_AIR, 12, 9, 1, structureboundingbox);
                this.createChest(generatoraccessseed, structureboundingbox, randomsource, 12, 8, 1, LootTables.STRONGHOLD_LIBRARY);
            }

        }
    }

    public static class c extends StrongholdPieces.p {

        protected static final int WIDTH = 10;
        protected static final int HEIGHT = 9;
        protected static final int DEPTH = 11;
        private final boolean leftLow;
        private final boolean leftHigh;
        private final boolean rightLow;
        private final boolean rightHigh;

        public c(int i, RandomSource randomsource, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_FIVE_CROSSING, i, structureboundingbox);
            this.setOrientation(enumdirection);
            this.entryDoor = this.randomSmallDoor(randomsource);
            this.leftLow = randomsource.nextBoolean();
            this.leftHigh = randomsource.nextBoolean();
            this.rightLow = randomsource.nextBoolean();
            this.rightHigh = randomsource.nextInt(3) > 0;
        }

        public c(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_FIVE_CROSSING, nbttagcompound);
            this.leftLow = nbttagcompound.getBooleanOr("leftLow", false);
            this.leftHigh = nbttagcompound.getBooleanOr("leftHigh", false);
            this.rightLow = nbttagcompound.getBooleanOr("rightLow", false);
            this.rightHigh = nbttagcompound.getBooleanOr("rightHigh", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext structurepieceserializationcontext, NBTTagCompound nbttagcompound) {
            super.addAdditionalSaveData(structurepieceserializationcontext, nbttagcompound);
            nbttagcompound.putBoolean("leftLow", this.leftLow);
            nbttagcompound.putBoolean("leftHigh", this.leftHigh);
            nbttagcompound.putBoolean("rightLow", this.rightLow);
            nbttagcompound.putBoolean("rightHigh", this.rightHigh);
        }

        @Override
        public void addChildren(StructurePiece structurepiece, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource) {
            int i = 3;
            int j = 5;
            EnumDirection enumdirection = this.getOrientation();

            if (enumdirection == EnumDirection.WEST || enumdirection == EnumDirection.NORTH) {
                i = 8 - i;
                j = 8 - j;
            }

            this.generateSmallDoorChildForward((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, 5, 1);
            if (this.leftLow) {
                this.generateSmallDoorChildLeft((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, i, 1);
            }

            if (this.leftHigh) {
                this.generateSmallDoorChildLeft((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, j, 7);
            }

            if (this.rightLow) {
                this.generateSmallDoorChildRight((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, i, 1);
            }

            if (this.rightHigh) {
                this.generateSmallDoorChildRight((StrongholdPieces.m) structurepiece, structurepieceaccessor, randomsource, j, 7);
            }

        }

        public static StrongholdPieces.c createPiece(StructurePieceAccessor structurepieceaccessor, RandomSource randomsource, int i, int j, int k, EnumDirection enumdirection, int l) {
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -4, -3, 0, 10, 9, 11, enumdirection);

            return isOkBox(structureboundingbox) && structurepieceaccessor.findCollisionPiece(structureboundingbox) == null ? new StrongholdPieces.c(l, randomsource, structureboundingbox, enumdirection) : null;
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            this.generateBox(generatoraccessseed, structureboundingbox, 0, 0, 0, 9, 8, 10, true, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, this.entryDoor, 4, 3, 0);
            if (this.leftLow) {
                this.generateBox(generatoraccessseed, structureboundingbox, 0, 3, 1, 0, 5, 3, StrongholdPieces.c.CAVE_AIR, StrongholdPieces.c.CAVE_AIR, false);
            }

            if (this.rightLow) {
                this.generateBox(generatoraccessseed, structureboundingbox, 9, 3, 1, 9, 5, 3, StrongholdPieces.c.CAVE_AIR, StrongholdPieces.c.CAVE_AIR, false);
            }

            if (this.leftHigh) {
                this.generateBox(generatoraccessseed, structureboundingbox, 0, 5, 7, 0, 7, 9, StrongholdPieces.c.CAVE_AIR, StrongholdPieces.c.CAVE_AIR, false);
            }

            if (this.rightHigh) {
                this.generateBox(generatoraccessseed, structureboundingbox, 9, 5, 7, 9, 7, 9, StrongholdPieces.c.CAVE_AIR, StrongholdPieces.c.CAVE_AIR, false);
            }

            this.generateBox(generatoraccessseed, structureboundingbox, 5, 1, 10, 7, 3, 10, StrongholdPieces.c.CAVE_AIR, StrongholdPieces.c.CAVE_AIR, false);
            this.generateBox(generatoraccessseed, structureboundingbox, 1, 2, 1, 8, 2, 6, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 4, 1, 5, 4, 4, 9, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 8, 1, 5, 8, 4, 9, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 1, 4, 7, 3, 4, 9, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 1, 3, 5, 3, 3, 6, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 1, 3, 4, 3, 3, 4, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(generatoraccessseed, structureboundingbox, 1, 4, 6, 3, 4, 6, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(generatoraccessseed, structureboundingbox, 5, 1, 7, 7, 1, 8, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 5, 1, 9, 7, 1, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(generatoraccessseed, structureboundingbox, 5, 2, 7, 7, 2, 7, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(generatoraccessseed, structureboundingbox, 4, 5, 7, 4, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(generatoraccessseed, structureboundingbox, 8, 5, 7, 8, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(generatoraccessseed, structureboundingbox, 5, 5, 7, 7, 5, 9, (IBlockData) Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(BlockStepAbstract.TYPE, BlockPropertySlabType.DOUBLE), (IBlockData) Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(BlockStepAbstract.TYPE, BlockPropertySlabType.DOUBLE), false);
            this.placeBlock(generatoraccessseed, (IBlockData) Blocks.WALL_TORCH.defaultBlockState().setValue(BlockTorchWall.FACING, EnumDirection.SOUTH), 6, 5, 6, structureboundingbox);
        }
    }

    public static class g extends StrongholdPieces.p {

        protected static final int WIDTH = 11;
        protected static final int HEIGHT = 8;
        protected static final int DEPTH = 16;
        private boolean hasPlacedSpawner;

        public g(int i, StructureBoundingBox structureboundingbox, EnumDirection enumdirection) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_PORTAL_ROOM, i, structureboundingbox);
            this.setOrientation(enumdirection);
        }

        public g(NBTTagCompound nbttagcompound) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_PORTAL_ROOM, nbttagcompound);
            this.hasPlacedSpawner = nbttagcompound.getBooleanOr("Mob", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext structurepieceserializationcontext, NBTTagCompound nbttagcompound) {
            super.addAdditionalSaveData(structurepieceserializationcontext, nbttagcompound);
            nbttagcompound.putBoolean("Mob", this.hasPlacedSpawner);
        }

        @Override
        public void addChildren(StructurePiece structurepiece, StructurePieceAccessor structurepieceaccessor, RandomSource randomsource) {
            if (structurepiece != null) {
                ((StrongholdPieces.m) structurepiece).portalRoomPiece = this;
            }

        }

        public static StrongholdPieces.g createPiece(StructurePieceAccessor structurepieceaccessor, int i, int j, int k, EnumDirection enumdirection, int l) {
            StructureBoundingBox structureboundingbox = StructureBoundingBox.orientBox(i, j, k, -4, -1, 0, 11, 8, 16, enumdirection);

            return isOkBox(structureboundingbox) && structurepieceaccessor.findCollisionPiece(structureboundingbox) == null ? new StrongholdPieces.g(l, structureboundingbox, enumdirection) : null;
        }

        @Override
        public void postProcess(GeneratorAccessSeed generatoraccessseed, StructureManager structuremanager, ChunkGenerator chunkgenerator, RandomSource randomsource, StructureBoundingBox structureboundingbox, ChunkCoordIntPair chunkcoordintpair, BlockPosition blockposition) {
            this.generateBox(generatoraccessseed, structureboundingbox, 0, 0, 0, 10, 7, 15, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(generatoraccessseed, randomsource, structureboundingbox, StrongholdPieces.p.a.GRATES, 4, 1, 0);
            int i = 6;

            this.generateBox(generatoraccessseed, structureboundingbox, 1, 6, 1, 1, 6, 14, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 9, 6, 1, 9, 6, 14, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 2, 6, 1, 8, 6, 2, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 2, 6, 14, 8, 6, 14, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 1, 1, 1, 2, 1, 4, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 8, 1, 1, 9, 1, 4, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 1, 1, 1, 1, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
            this.generateBox(generatoraccessseed, structureboundingbox, 9, 1, 1, 9, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
            this.generateBox(generatoraccessseed, structureboundingbox, 3, 1, 8, 7, 1, 12, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 4, 1, 9, 6, 1, 11, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
            IBlockData iblockdata = (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.NORTH, true)).setValue(BlockIronBars.SOUTH, true);
            IBlockData iblockdata1 = (IBlockData) ((IBlockData) Blocks.IRON_BARS.defaultBlockState().setValue(BlockIronBars.WEST, true)).setValue(BlockIronBars.EAST, true);

            for (int j = 3; j < 14; j += 2) {
                this.generateBox(generatoraccessseed, structureboundingbox, 0, 3, j, 0, 4, j, iblockdata, iblockdata, false);
                this.generateBox(generatoraccessseed, structureboundingbox, 10, 3, j, 10, 4, j, iblockdata, iblockdata, false);
            }

            for (int k = 2; k < 9; k += 2) {
                this.generateBox(generatoraccessseed, structureboundingbox, k, 3, 15, k, 4, 15, iblockdata1, iblockdata1, false);
            }

            IBlockData iblockdata2 = (IBlockData) Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(BlockStairs.FACING, EnumDirection.NORTH);

            this.generateBox(generatoraccessseed, structureboundingbox, 4, 1, 5, 6, 1, 7, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 4, 2, 6, 6, 2, 7, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(generatoraccessseed, structureboundingbox, 4, 3, 7, 6, 3, 7, false, randomsource, StrongholdPieces.SMOOTH_STONE_SELECTOR);

            for (int l = 4; l <= 6; ++l) {
                this.placeBlock(generatoraccessseed, iblockdata2, l, 1, 4, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata2, l, 2, 5, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata2, l, 3, 6, structureboundingbox);
            }

            IBlockData iblockdata3 = (IBlockData) Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(BlockEnderPortalFrame.FACING, EnumDirection.NORTH);
            IBlockData iblockdata4 = (IBlockData) Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(BlockEnderPortalFrame.FACING, EnumDirection.SOUTH);
            IBlockData iblockdata5 = (IBlockData) Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(BlockEnderPortalFrame.FACING, EnumDirection.EAST);
            IBlockData iblockdata6 = (IBlockData) Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(BlockEnderPortalFrame.FACING, EnumDirection.WEST);
            boolean flag = true;
            boolean[] aboolean = new boolean[12];

            for (int i1 = 0; i1 < aboolean.length; ++i1) {
                aboolean[i1] = randomsource.nextFloat() > 0.9F;
                flag &= aboolean[i1];
            }

            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata3.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[0]), 4, 3, 8, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata3.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[1]), 5, 3, 8, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata3.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[2]), 6, 3, 8, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata4.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[3]), 4, 3, 12, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata4.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[4]), 5, 3, 12, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata4.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[5]), 6, 3, 12, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata5.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[6]), 3, 3, 9, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata5.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[7]), 3, 3, 10, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata5.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[8]), 3, 3, 11, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata6.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[9]), 7, 3, 9, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata6.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[10]), 7, 3, 10, structureboundingbox);
            this.placeBlock(generatoraccessseed, (IBlockData) iblockdata6.setValue(BlockEnderPortalFrame.HAS_EYE, aboolean[11]), 7, 3, 11, structureboundingbox);
            if (flag) {
                IBlockData iblockdata7 = Blocks.END_PORTAL.defaultBlockState();

                this.placeBlock(generatoraccessseed, iblockdata7, 4, 3, 9, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata7, 5, 3, 9, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata7, 6, 3, 9, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata7, 4, 3, 10, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata7, 5, 3, 10, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata7, 6, 3, 10, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata7, 4, 3, 11, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata7, 5, 3, 11, structureboundingbox);
                this.placeBlock(generatoraccessseed, iblockdata7, 6, 3, 11, structureboundingbox);
            }

            if (!this.hasPlacedSpawner) {
                BlockPosition blockposition1 = this.getWorldPos(5, 3, 6);

                if (structureboundingbox.isInside(blockposition1)) {
                    this.hasPlacedSpawner = true;
                    // CraftBukkit start
                    /*
                    generatoraccessseed.setBlock(blockposition1, Blocks.SPAWNER.defaultBlockState(), 2);
                    TileEntity tileentity = generatoraccessseed.getBlockEntity(blockposition1);

                    if (tileentity instanceof TileEntityMobSpawner) {
                        TileEntityMobSpawner tileentitymobspawner = (TileEntityMobSpawner) tileentity;

                        tileentitymobspawner.setEntityId(EntityTypes.SILVERFISH, randomsource);
                    }
                    */
                    placeCraftSpawner(generatoraccessseed, blockposition1, org.bukkit.entity.EntityType.SILVERFISH, 2);
                    // CraftBukkit end
                }
            }

        }
    }

    private static class k extends StructurePiece.StructurePieceBlockSelector {

        k() {}

        @Override
        public void next(RandomSource randomsource, int i, int j, int k, boolean flag) {
            if (flag) {
                float f = randomsource.nextFloat();

                if (f < 0.2F) {
                    this.next = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                } else if (f < 0.5F) {
                    this.next = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                } else if (f < 0.55F) {
                    this.next = Blocks.INFESTED_STONE_BRICKS.defaultBlockState();
                } else {
                    this.next = Blocks.STONE_BRICKS.defaultBlockState();
                }
            } else {
                this.next = Blocks.CAVE_AIR.defaultBlockState();
            }

        }
    }
}
