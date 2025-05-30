package net.minecraft.world.level.block;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryBlockID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.piglin.PiglinAI;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.IBlockDataHolder;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.slf4j.Logger;

public class Block extends BlockBase implements IMaterial {

    public static final MapCodec<Block> CODEC = simpleCodec(Block::new);
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Holder.c<Block> builtInRegistryHolder;
    public static final RegistryBlockID<IBlockData> BLOCK_STATE_REGISTRY = new RegistryBlockID<IBlockData>();
    private static final LoadingCache<VoxelShape, Boolean> SHAPE_FULL_BLOCK_CACHE = CacheBuilder.newBuilder().maximumSize(512L).weakKeys().build(new CacheLoader<VoxelShape, Boolean>() {
        public Boolean load(VoxelShape voxelshape) {
            return !VoxelShapes.joinIsNotEmpty(VoxelShapes.block(), voxelshape, OperatorBoolean.NOT_SAME);
        }
    });
    public static final int UPDATE_NEIGHBORS = 1;
    public static final int UPDATE_CLIENTS = 2;
    public static final int UPDATE_INVISIBLE = 4;
    public static final int UPDATE_IMMEDIATE = 8;
    public static final int UPDATE_KNOWN_SHAPE = 16;
    public static final int UPDATE_SUPPRESS_DROPS = 32;
    public static final int UPDATE_MOVE_BY_PISTON = 64;
    public static final int UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE = 128;
    public static final int UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS = 256;
    public static final int UPDATE_SKIP_ON_PLACE = 512;
    public static final int UPDATE_NONE = 260;
    public static final int UPDATE_ALL = 3;
    public static final int UPDATE_ALL_IMMEDIATE = 11;
    public static final int UPDATE_SKIP_ALL_SIDEEFFECTS = 816;
    public static final float INDESTRUCTIBLE = -1.0F;
    public static final float INSTANT = 0.0F;
    public static final int UPDATE_LIMIT = 512;
    protected final BlockStateList<Block, IBlockData> stateDefinition;
    private IBlockData defaultBlockState;
    @Nullable
    private Item item;
    private static final int CACHE_SIZE = 256;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.a>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
        Object2ByteLinkedOpenHashMap<Block.a> object2bytelinkedopenhashmap = new Object2ByteLinkedOpenHashMap<Block.a>(256, 0.25F) {
            protected void rehash(int i) {}
        };

        object2bytelinkedopenhashmap.defaultReturnValue((byte) 127);
        return object2bytelinkedopenhashmap;
    });

    @Override
    protected MapCodec<? extends Block> codec() {
        return Block.CODEC;
    }

    public static int getId(@Nullable IBlockData iblockdata) {
        if (iblockdata == null) {
            return 0;
        } else {
            int i = Block.BLOCK_STATE_REGISTRY.getId(iblockdata);

            return i == -1 ? 0 : i;
        }
    }

    public static IBlockData stateById(int i) {
        IBlockData iblockdata = (IBlockData) Block.BLOCK_STATE_REGISTRY.byId(i);

        return iblockdata == null ? Blocks.AIR.defaultBlockState() : iblockdata;
    }

    public static Block byItem(@Nullable Item item) {
        return item instanceof ItemBlock ? ((ItemBlock) item).getBlock() : Blocks.AIR;
    }

    public static IBlockData pushEntitiesUp(IBlockData iblockdata, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition) {
        VoxelShape voxelshape = VoxelShapes.joinUnoptimized(iblockdata.getCollisionShape(generatoraccess, blockposition), iblockdata1.getCollisionShape(generatoraccess, blockposition), OperatorBoolean.ONLY_SECOND).move((BaseBlockPosition) blockposition);

        if (voxelshape.isEmpty()) {
            return iblockdata1;
        } else {
            for (Entity entity : generatoraccess.getEntities((Entity) null, voxelshape.bounds())) {
                double d0 = VoxelShapes.collide(EnumDirection.EnumAxis.Y, entity.getBoundingBox().move(0.0D, 1.0D, 0.0D), List.of(voxelshape), -1.0D);

                entity.teleportRelative(0.0D, 1.0D + d0, 0.0D);
            }

            return iblockdata1;
        }
    }

    public static VoxelShape box(double d0, double d1, double d2, double d3, double d4, double d5) {
        return VoxelShapes.box(d0 / 16.0D, d1 / 16.0D, d2 / 16.0D, d3 / 16.0D, d4 / 16.0D, d5 / 16.0D);
    }

    public static VoxelShape[] boxes(int i, IntFunction<VoxelShape> intfunction) {
        return (VoxelShape[]) IntStream.rangeClosed(0, i).mapToObj(intfunction).toArray((j) -> {
            return new VoxelShape[j];
        });
    }

    public static VoxelShape cube(double d0) {
        return cube(d0, d0, d0);
    }

    public static VoxelShape cube(double d0, double d1, double d2) {
        double d3 = d1 / 2.0D;

        return column(d0, d2, 8.0D - d3, 8.0D + d3);
    }

    public static VoxelShape column(double d0, double d1, double d2) {
        return column(d0, d0, d1, d2);
    }

    public static VoxelShape column(double d0, double d1, double d2, double d3) {
        double d4 = d0 / 2.0D;
        double d5 = d1 / 2.0D;

        return box(8.0D - d4, d2, 8.0D - d5, 8.0D + d4, d3, 8.0D + d5);
    }

    public static VoxelShape boxZ(double d0, double d1, double d2) {
        return boxZ(d0, d0, d1, d2);
    }

    public static VoxelShape boxZ(double d0, double d1, double d2, double d3) {
        double d4 = d1 / 2.0D;

        return boxZ(d0, 8.0D - d4, 8.0D + d4, d2, d3);
    }

    public static VoxelShape boxZ(double d0, double d1, double d2, double d3, double d4) {
        double d5 = d0 / 2.0D;

        return box(8.0D - d5, d1, d3, 8.0D + d5, d2, d4);
    }

    public static IBlockData updateFromNeighbourShapes(IBlockData iblockdata, GeneratorAccess generatoraccess, BlockPosition blockposition) {
        IBlockData iblockdata1 = iblockdata;
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();

        for (EnumDirection enumdirection : Block.UPDATE_SHAPE_ORDER) {
            blockposition_mutableblockposition.setWithOffset(blockposition, enumdirection);
            iblockdata1 = iblockdata1.updateShape(generatoraccess, generatoraccess, blockposition, enumdirection, blockposition_mutableblockposition, generatoraccess.getBlockState(blockposition_mutableblockposition), generatoraccess.getRandom());
        }

        return iblockdata1;
    }

    public static void updateOrDestroy(IBlockData iblockdata, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, int i) {
        updateOrDestroy(iblockdata, iblockdata1, generatoraccess, blockposition, i, 512);
    }

    public static void updateOrDestroy(IBlockData iblockdata, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, int i, int j) {
        if (iblockdata1 != iblockdata) {
            if (iblockdata1.isAir()) {
                if (!generatoraccess.isClientSide()) {
                    generatoraccess.destroyBlock(blockposition, (i & 32) == 0, (Entity) null, j);
                }
            } else {
                generatoraccess.setBlock(blockposition, iblockdata1, i & -33, j);
            }
        }

    }

    public Block(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.builtInRegistryHolder = BuiltInRegistries.BLOCK.createIntrusiveHolder(this);
        BlockStateList.a<Block, IBlockData> blockstatelist_a = new BlockStateList.a<Block, IBlockData>(this);

        this.createBlockStateDefinition(blockstatelist_a);
        this.stateDefinition = blockstatelist_a.create(Block::defaultBlockState, IBlockData::new);
        this.registerDefaultState(this.stateDefinition.any());
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            String s = this.getClass().getSimpleName();

            if (!s.endsWith("Block")) {
                Block.LOGGER.error("Block classes should end with Block and {} doesn't.", s);
            }
        }

    }

    public static boolean isExceptionForConnection(IBlockData iblockdata) {
        return iblockdata.getBlock() instanceof BlockLeaves || iblockdata.is(Blocks.BARRIER) || iblockdata.is(Blocks.CARVED_PUMPKIN) || iblockdata.is(Blocks.JACK_O_LANTERN) || iblockdata.is(Blocks.MELON) || iblockdata.is(Blocks.PUMPKIN) || iblockdata.is(TagsBlock.SHULKER_BOXES);
    }

    public static boolean shouldRenderFace(IBlockData iblockdata, IBlockData iblockdata1, EnumDirection enumdirection) {
        VoxelShape voxelshape = iblockdata1.getFaceOcclusionShape(enumdirection.getOpposite());

        if (voxelshape == VoxelShapes.block()) {
            return false;
        } else if (iblockdata.skipRendering(iblockdata1, enumdirection)) {
            return false;
        } else if (voxelshape == VoxelShapes.empty()) {
            return true;
        } else {
            VoxelShape voxelshape1 = iblockdata.getFaceOcclusionShape(enumdirection);

            if (voxelshape1 == VoxelShapes.empty()) {
                return true;
            } else {
                Block.a block_a = new Block.a(voxelshape1, voxelshape);
                Object2ByteLinkedOpenHashMap<Block.a> object2bytelinkedopenhashmap = (Object2ByteLinkedOpenHashMap) Block.OCCLUSION_CACHE.get();
                byte b0 = object2bytelinkedopenhashmap.getAndMoveToFirst(block_a);

                if (b0 != 127) {
                    return b0 != 0;
                } else {
                    boolean flag = VoxelShapes.joinIsNotEmpty(voxelshape1, voxelshape, OperatorBoolean.ONLY_FIRST);

                    if (object2bytelinkedopenhashmap.size() == 256) {
                        object2bytelinkedopenhashmap.removeLastByte();
                    }

                    object2bytelinkedopenhashmap.putAndMoveToFirst(block_a, (byte) (flag ? 1 : 0));
                    return flag;
                }
            }
        }
    }

    public static boolean canSupportRigidBlock(IBlockAccess iblockaccess, BlockPosition blockposition) {
        return iblockaccess.getBlockState(blockposition).isFaceSturdy(iblockaccess, blockposition, EnumDirection.UP, EnumBlockSupport.RIGID);
    }

    public static boolean canSupportCenter(IWorldReader iworldreader, BlockPosition blockposition, EnumDirection enumdirection) {
        IBlockData iblockdata = iworldreader.getBlockState(blockposition);

        return enumdirection == EnumDirection.DOWN && iblockdata.is(TagsBlock.UNSTABLE_BOTTOM_CENTER) ? false : iblockdata.isFaceSturdy(iworldreader, blockposition, enumdirection, EnumBlockSupport.CENTER);
    }

    public static boolean isFaceFull(VoxelShape voxelshape, EnumDirection enumdirection) {
        VoxelShape voxelshape1 = voxelshape.getFaceShape(enumdirection);

        return isShapeFullBlock(voxelshape1);
    }

    public static boolean isShapeFullBlock(VoxelShape voxelshape) {
        return (Boolean) Block.SHAPE_FULL_BLOCK_CACHE.getUnchecked(voxelshape);
    }

    public void animateTick(IBlockData iblockdata, World world, BlockPosition blockposition, RandomSource randomsource) {}

    public void destroy(GeneratorAccess generatoraccess, BlockPosition blockposition, IBlockData iblockdata) {}

    public static List<ItemStack> getDrops(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, @Nullable TileEntity tileentity) {
        LootParams.a lootparams_a = (new LootParams.a(worldserver)).withParameter(LootContextParameters.ORIGIN, Vec3D.atCenterOf(blockposition)).withParameter(LootContextParameters.TOOL, ItemStack.EMPTY).withOptionalParameter(LootContextParameters.BLOCK_ENTITY, tileentity);

        return iblockdata.getDrops(lootparams_a);
    }

    public static List<ItemStack> getDrops(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, @Nullable TileEntity tileentity, @Nullable Entity entity, ItemStack itemstack) {
        LootParams.a lootparams_a = (new LootParams.a(worldserver)).withParameter(LootContextParameters.ORIGIN, Vec3D.atCenterOf(blockposition)).withParameter(LootContextParameters.TOOL, itemstack).withOptionalParameter(LootContextParameters.THIS_ENTITY, entity).withOptionalParameter(LootContextParameters.BLOCK_ENTITY, tileentity);

        return iblockdata.getDrops(lootparams_a);
    }

    public static void dropResources(IBlockData iblockdata, World world, BlockPosition blockposition) {
        if (world instanceof WorldServer) {
            getDrops(iblockdata, (WorldServer) world, blockposition, (TileEntity) null).forEach((itemstack) -> {
                popResource(world, blockposition, itemstack);
            });
            iblockdata.spawnAfterBreak((WorldServer) world, blockposition, ItemStack.EMPTY, true);
        }

    }

    public static void dropResources(IBlockData iblockdata, GeneratorAccess generatoraccess, BlockPosition blockposition, @Nullable TileEntity tileentity) {
        if (generatoraccess instanceof WorldServer) {
            getDrops(iblockdata, (WorldServer) generatoraccess, blockposition, tileentity).forEach((itemstack) -> {
                popResource((WorldServer) generatoraccess, blockposition, itemstack);
            });
            iblockdata.spawnAfterBreak((WorldServer) generatoraccess, blockposition, ItemStack.EMPTY, true);
        }

    }

    public static void dropResources(IBlockData iblockdata, World world, BlockPosition blockposition, @Nullable TileEntity tileentity, @Nullable Entity entity, ItemStack itemstack) {
        if (world instanceof WorldServer) {
            getDrops(iblockdata, (WorldServer) world, blockposition, tileentity, entity, itemstack).forEach((itemstack1) -> {
                popResource(world, blockposition, itemstack1);
            });
            iblockdata.spawnAfterBreak((WorldServer) world, blockposition, itemstack, true);
        }

    }

    public static void popResource(World world, BlockPosition blockposition, ItemStack itemstack) {
        double d0 = (double) EntityTypes.ITEM.getHeight() / 2.0D;
        double d1 = (double) blockposition.getX() + 0.5D + MathHelper.nextDouble(world.random, -0.25D, 0.25D);
        double d2 = (double) blockposition.getY() + 0.5D + MathHelper.nextDouble(world.random, -0.25D, 0.25D) - d0;
        double d3 = (double) blockposition.getZ() + 0.5D + MathHelper.nextDouble(world.random, -0.25D, 0.25D);

        popResource(world, () -> {
            return new EntityItem(world, d1, d2, d3, itemstack);
        }, itemstack);
    }

    public static void popResourceFromFace(World world, BlockPosition blockposition, EnumDirection enumdirection, ItemStack itemstack) {
        int i = enumdirection.getStepX();
        int j = enumdirection.getStepY();
        int k = enumdirection.getStepZ();
        double d0 = (double) EntityTypes.ITEM.getWidth() / 2.0D;
        double d1 = (double) EntityTypes.ITEM.getHeight() / 2.0D;
        double d2 = (double) blockposition.getX() + 0.5D + (i == 0 ? MathHelper.nextDouble(world.random, -0.25D, 0.25D) : (double) i * (0.5D + d0));
        double d3 = (double) blockposition.getY() + 0.5D + (j == 0 ? MathHelper.nextDouble(world.random, -0.25D, 0.25D) : (double) j * (0.5D + d1)) - d1;
        double d4 = (double) blockposition.getZ() + 0.5D + (k == 0 ? MathHelper.nextDouble(world.random, -0.25D, 0.25D) : (double) k * (0.5D + d0));
        double d5 = i == 0 ? MathHelper.nextDouble(world.random, -0.1D, 0.1D) : (double) i * 0.1D;
        double d6 = j == 0 ? MathHelper.nextDouble(world.random, 0.0D, 0.1D) : (double) j * 0.1D + 0.1D;
        double d7 = k == 0 ? MathHelper.nextDouble(world.random, -0.1D, 0.1D) : (double) k * 0.1D;

        popResource(world, () -> {
            return new EntityItem(world, d2, d3, d4, itemstack, d5, d6, d7);
        }, itemstack);
    }

    private static void popResource(World world, Supplier<EntityItem> supplier, ItemStack itemstack) {
        if (world instanceof WorldServer worldserver) {
            if (!itemstack.isEmpty() && worldserver.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                EntityItem entityitem = (EntityItem) supplier.get();

                entityitem.setDefaultPickUpDelay();
                // CraftBukkit start
                if (world.captureDrops != null) {
                    world.captureDrops.add(entityitem);
                } else {
                    world.addFreshEntity(entityitem);
                }
                // CraftBukkit end
                return;
            }
        }

    }

    public void popExperience(WorldServer worldserver, BlockPosition blockposition, int i) {
        if (worldserver.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            EntityExperienceOrb.award(worldserver, Vec3D.atCenterOf(blockposition), i);
        }

    }

    public float getExplosionResistance() {
        return this.explosionResistance;
    }

    public void wasExploded(WorldServer worldserver, BlockPosition blockposition, Explosion explosion) {}

    public void stepOn(World world, BlockPosition blockposition, IBlockData iblockdata, Entity entity) {}

    @Nullable
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        return this.defaultBlockState();
    }

    public void playerDestroy(World world, EntityHuman entityhuman, BlockPosition blockposition, IBlockData iblockdata, @Nullable TileEntity tileentity, ItemStack itemstack) {
        entityhuman.awardStat(StatisticList.BLOCK_MINED.get(this));
        entityhuman.causeFoodExhaustion(0.005F, org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.BLOCK_MINED); // CraftBukkit - EntityExhaustionEvent
        dropResources(iblockdata, world, blockposition, tileentity, entityhuman, itemstack);
    }

    public void setPlacedBy(World world, BlockPosition blockposition, IBlockData iblockdata, @Nullable EntityLiving entityliving, ItemStack itemstack) {}

    public boolean isPossibleToRespawnInThis(IBlockData iblockdata) {
        return !iblockdata.isSolid() && !iblockdata.liquid();
    }

    public IChatMutableComponent getName() {
        return IChatBaseComponent.translatable(this.getDescriptionId());
    }

    public void fallOn(World world, IBlockData iblockdata, BlockPosition blockposition, Entity entity, double d0) {
        entity.causeFallDamage(d0, 1.0F, entity.damageSources().fall());
    }

    public void updateEntityMovementAfterFallOn(IBlockAccess iblockaccess, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
    }

    public float getFriction() {
        return this.friction;
    }

    public float getSpeedFactor() {
        return this.speedFactor;
    }

    public float getJumpFactor() {
        return this.jumpFactor;
    }

    protected void spawnDestroyParticles(World world, EntityHuman entityhuman, BlockPosition blockposition, IBlockData iblockdata) {
        world.levelEvent(entityhuman, 2001, blockposition, getId(iblockdata));
    }

    public IBlockData playerWillDestroy(World world, BlockPosition blockposition, IBlockData iblockdata, EntityHuman entityhuman) {
        this.spawnDestroyParticles(world, entityhuman, blockposition, iblockdata);
        if (iblockdata.is(TagsBlock.GUARDED_BY_PIGLINS) && world instanceof WorldServer worldserver) {
            PiglinAI.angerNearbyPiglins(worldserver, entityhuman, false);
        }

        world.gameEvent(GameEvent.BLOCK_DESTROY, blockposition, GameEvent.a.of(entityhuman, iblockdata));
        return iblockdata;
    }

    public void handlePrecipitation(IBlockData iblockdata, World world, BlockPosition blockposition, BiomeBase.Precipitation biomebase_precipitation) {}

    public boolean dropFromExplosion(Explosion explosion) {
        return true;
    }

    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {}

    public BlockStateList<Block, IBlockData> getStateDefinition() {
        return this.stateDefinition;
    }

    protected final void registerDefaultState(IBlockData iblockdata) {
        this.defaultBlockState = iblockdata;
    }

    public final IBlockData defaultBlockState() {
        return this.defaultBlockState;
    }

    public final IBlockData withPropertiesOf(IBlockData iblockdata) {
        IBlockData iblockdata1 = this.defaultBlockState();

        for (IBlockState<?> iblockstate : iblockdata.getBlock().getStateDefinition().getProperties()) {
            if (iblockdata1.hasProperty(iblockstate)) {
                iblockdata1 = copyProperty(iblockdata, iblockdata1, iblockstate);
            }
        }

        return iblockdata1;
    }

    private static <T extends Comparable<T>> IBlockData copyProperty(IBlockData iblockdata, IBlockData iblockdata1, IBlockState<T> iblockstate) {
        return (IBlockData) iblockdata1.setValue(iblockstate, iblockdata.getValue(iblockstate));
    }

    @Override
    public Item asItem() {
        if (this.item == null) {
            this.item = Item.byBlock(this);
        }

        return this.item;
    }

    public boolean hasDynamicShape() {
        return this.dynamicShape;
    }

    public String toString() {
        return "Block{" + BuiltInRegistries.BLOCK.wrapAsHolder(this).getRegisteredName() + "}";
    }

    @Override
    protected Block asBlock() {
        return this;
    }

    protected Function<IBlockData, VoxelShape> getShapeForEachState(Function<IBlockData, VoxelShape> function) {
        ImmutableMap<IBlockData, VoxelShape> immutablemap = (ImmutableMap) this.stateDefinition.getPossibleStates().stream().collect(ImmutableMap.toImmutableMap(Function.identity(), function)); // CraftBukkit - decompile error

        Objects.requireNonNull(immutablemap);
        return immutablemap::get;
    }

    protected Function<IBlockData, VoxelShape> getShapeForEachState(Function<IBlockData, VoxelShape> function, IBlockState<?>... aiblockstate) {
        Map<? extends IBlockState<?>, Object> map = (Map) Arrays.stream(aiblockstate).collect(Collectors.toMap((iblockstate) -> {
            return iblockstate;
        }, (iblockstate) -> {
            return iblockstate.getPossibleValues().getFirst();
        }));
        ImmutableMap<IBlockData, VoxelShape> immutablemap = (ImmutableMap) this.stateDefinition.getPossibleStates().stream().filter((iblockdata) -> {
            return map.entrySet().stream().allMatch((entry) -> {
                return iblockdata.getValue((IBlockState) entry.getKey()) == entry.getValue();
            });
        }).collect(ImmutableMap.toImmutableMap(Function.identity(), function));

        return (iblockdata) -> {
            for (Map.Entry<? extends IBlockState<?>, Object> map_entry : map.entrySet()) {
                iblockdata = (IBlockData) setValueHelper(iblockdata, (IBlockState) map_entry.getKey(), map_entry.getValue());
            }

            return (VoxelShape) immutablemap.get(iblockdata);
        };
    }

    private static <S extends IBlockDataHolder<?, S>, T extends Comparable<T>> S setValueHelper(S s0, IBlockState<T> iblockstate, Object object) {
        return (S) (((IBlockDataHolder) s0).setValue(iblockstate, (Comparable) object));
    }

    /** @deprecated */
    @Deprecated
    public Holder.c<Block> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    // CraftBukkit start
    protected int tryDropExperience(WorldServer worldserver, BlockPosition blockposition, ItemStack itemstack, IntProvider intprovider) {
        int i = EnchantmentManager.processBlockExperience(worldserver, itemstack, intprovider.sample(worldserver.getRandom()));

        if (i > 0) {
            // this.popExperience(worldserver, blockposition, i);
            return i;
        }

        return 0;
    }

    public int getExpDrop(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, ItemStack itemstack, boolean flag) {
        return 0;
    }
    // CraftBukkit end

    // Spigot start
    public static float range(float min, float value, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
    // Spigot end

    private static record a(VoxelShape first, VoxelShape second) {

        public boolean equals(Object object) {
            boolean flag;

            if (object instanceof Block.a block_a) {
                if (this.first == block_a.first && this.second == block_a.second) {
                    flag = true;
                    return flag;
                }
            }

            flag = false;
            return flag;
        }

        public int hashCode() {
            return System.identityHashCode(this.first) * 31 + System.identityHashCode(this.second);
        }
    }
}
