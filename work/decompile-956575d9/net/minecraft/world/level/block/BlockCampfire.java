package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.item.crafting.RecipeCampfire;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityCampfire;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockCampfire extends BlockTileEntity implements IBlockWaterlogged {

    public static final MapCodec<BlockCampfire> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(Codec.BOOL.fieldOf("spawn_particles").forGetter((blockcampfire) -> {
            return blockcampfire.spawnParticles;
        }), Codec.intRange(0, 1000).fieldOf("fire_damage").forGetter((blockcampfire) -> {
            return blockcampfire.fireDamage;
        }), propertiesCodec()).apply(instance, BlockCampfire::new);
    });
    public static final BlockStateBoolean LIT = BlockProperties.LIT;
    public static final BlockStateBoolean SIGNAL_FIRE = BlockProperties.SIGNAL_FIRE;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final BlockStateEnum<EnumDirection> FACING = BlockProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.column(16.0D, 0.0D, 7.0D);
    private static final VoxelShape SHAPE_VIRTUAL_POST = Block.column(4.0D, 0.0D, 16.0D);
    private static final int SMOKE_DISTANCE = 5;
    private final boolean spawnParticles;
    private final int fireDamage;

    @Override
    public MapCodec<BlockCampfire> codec() {
        return BlockCampfire.CODEC;
    }

    public BlockCampfire(boolean flag, int i, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.spawnParticles = flag;
        this.fireDamage = i;
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(BlockCampfire.LIT, true)).setValue(BlockCampfire.SIGNAL_FIRE, false)).setValue(BlockCampfire.WATERLOGGED, false)).setValue(BlockCampfire.FACING, EnumDirection.NORTH));
    }

    @Override
    protected EnumInteractionResult useItemOn(ItemStack itemstack, IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, EnumHand enumhand, MovingObjectPositionBlock movingobjectpositionblock) {
        TileEntity tileentity = world.getBlockEntity(blockposition);

        if (tileentity instanceof TileEntityCampfire tileentitycampfire) {
            ItemStack itemstack1 = entityhuman.getItemInHand(enumhand);

            if (world.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(itemstack1)) {
                if (world instanceof WorldServer) {
                    WorldServer worldserver = (WorldServer) world;

                    if (tileentitycampfire.placeFood(worldserver, entityhuman, itemstack1)) {
                        entityhuman.awardStat(StatisticList.INTERACT_WITH_CAMPFIRE);
                        return EnumInteractionResult.SUCCESS_SERVER;
                    }
                }

                return EnumInteractionResult.CONSUME;
            }
        }

        return EnumInteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected void entityInside(IBlockData iblockdata, World world, BlockPosition blockposition, Entity entity, InsideBlockEffectApplier insideblockeffectapplier) {
        if ((Boolean) iblockdata.getValue(BlockCampfire.LIT) && entity instanceof EntityLiving) {
            entity.hurt(world.damageSources().campfire(), (float) this.fireDamage);
        }

        super.entityInside(iblockdata, world, blockposition, entity, insideblockeffectapplier);
    }

    @Nullable
    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        GeneratorAccess generatoraccess = blockactioncontext.getLevel();
        BlockPosition blockposition = blockactioncontext.getClickedPos();
        boolean flag = generatoraccess.getFluidState(blockposition).getType() == FluidTypes.WATER;

        return (IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) this.defaultBlockState().setValue(BlockCampfire.WATERLOGGED, flag)).setValue(BlockCampfire.SIGNAL_FIRE, this.isSmokeSource(generatoraccess.getBlockState(blockposition.below())))).setValue(BlockCampfire.LIT, !flag)).setValue(BlockCampfire.FACING, blockactioncontext.getHorizontalDirection());
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        if ((Boolean) iblockdata.getValue(BlockCampfire.WATERLOGGED)) {
            scheduledtickaccess.scheduleTick(blockposition, (FluidType) FluidTypes.WATER, FluidTypes.WATER.getTickDelay(iworldreader));
        }

        return enumdirection == EnumDirection.DOWN ? (IBlockData) iblockdata.setValue(BlockCampfire.SIGNAL_FIRE, this.isSmokeSource(iblockdata1)) : super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
    }

    private boolean isSmokeSource(IBlockData iblockdata) {
        return iblockdata.is(Blocks.HAY_BLOCK);
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockCampfire.SHAPE;
    }

    @Override
    public void animateTick(IBlockData iblockdata, World world, BlockPosition blockposition, RandomSource randomsource) {
        if ((Boolean) iblockdata.getValue(BlockCampfire.LIT)) {
            if (randomsource.nextInt(10) == 0) {
                world.playLocalSound((double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 0.5D, (double) blockposition.getZ() + 0.5D, SoundEffects.CAMPFIRE_CRACKLE, SoundCategory.BLOCKS, 0.5F + randomsource.nextFloat(), randomsource.nextFloat() * 0.7F + 0.6F, false);
            }

            if (this.spawnParticles && randomsource.nextInt(5) == 0) {
                for (int i = 0; i < randomsource.nextInt(1) + 1; ++i) {
                    world.addParticle(Particles.LAVA, (double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 0.5D, (double) blockposition.getZ() + 0.5D, (double) (randomsource.nextFloat() / 2.0F), 5.0E-5D, (double) (randomsource.nextFloat() / 2.0F));
                }
            }

        }
    }

    public static void dowse(@Nullable Entity entity, GeneratorAccess generatoraccess, BlockPosition blockposition, IBlockData iblockdata) {
        if (generatoraccess.isClientSide()) {
            for (int i = 0; i < 20; ++i) {
                makeParticles((World) generatoraccess, blockposition, (Boolean) iblockdata.getValue(BlockCampfire.SIGNAL_FIRE), true);
            }
        }

        generatoraccess.gameEvent(entity, (Holder) GameEvent.BLOCK_CHANGE, blockposition);
    }

    @Override
    public boolean placeLiquid(GeneratorAccess generatoraccess, BlockPosition blockposition, IBlockData iblockdata, Fluid fluid) {
        if (!(Boolean) iblockdata.getValue(BlockProperties.WATERLOGGED) && fluid.getType() == FluidTypes.WATER) {
            boolean flag = (Boolean) iblockdata.getValue(BlockCampfire.LIT);

            if (flag) {
                if (!generatoraccess.isClientSide()) {
                    generatoraccess.playSound((Entity) null, blockposition, SoundEffects.GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }

                dowse((Entity) null, generatoraccess, blockposition, iblockdata);
            }

            generatoraccess.setBlock(blockposition, (IBlockData) ((IBlockData) iblockdata.setValue(BlockCampfire.WATERLOGGED, true)).setValue(BlockCampfire.LIT, false), 3);
            generatoraccess.scheduleTick(blockposition, fluid.getType(), fluid.getType().getTickDelay(generatoraccess));
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onProjectileHit(World world, IBlockData iblockdata, MovingObjectPositionBlock movingobjectpositionblock, IProjectile iprojectile) {
        BlockPosition blockposition = movingobjectpositionblock.getBlockPos();

        if (world instanceof WorldServer worldserver) {
            if (iprojectile.isOnFire() && iprojectile.mayInteract(worldserver, blockposition) && !(Boolean) iblockdata.getValue(BlockCampfire.LIT) && !(Boolean) iblockdata.getValue(BlockCampfire.WATERLOGGED)) {
                world.setBlock(blockposition, (IBlockData) iblockdata.setValue(BlockProperties.LIT, true), 11);
            }
        }

    }

    public static void makeParticles(World world, BlockPosition blockposition, boolean flag, boolean flag1) {
        RandomSource randomsource = world.getRandom();
        ParticleType particletype = flag ? Particles.CAMPFIRE_SIGNAL_SMOKE : Particles.CAMPFIRE_COSY_SMOKE;

        world.addAlwaysVisibleParticle(particletype, true, (double) blockposition.getX() + 0.5D + randomsource.nextDouble() / 3.0D * (double) (randomsource.nextBoolean() ? 1 : -1), (double) blockposition.getY() + randomsource.nextDouble() + randomsource.nextDouble(), (double) blockposition.getZ() + 0.5D + randomsource.nextDouble() / 3.0D * (double) (randomsource.nextBoolean() ? 1 : -1), 0.0D, 0.07D, 0.0D);
        if (flag1) {
            world.addParticle(Particles.SMOKE, (double) blockposition.getX() + 0.5D + randomsource.nextDouble() / 4.0D * (double) (randomsource.nextBoolean() ? 1 : -1), (double) blockposition.getY() + 0.4D, (double) blockposition.getZ() + 0.5D + randomsource.nextDouble() / 4.0D * (double) (randomsource.nextBoolean() ? 1 : -1), 0.0D, 0.005D, 0.0D);
        }

    }

    public static boolean isSmokeyPos(World world, BlockPosition blockposition) {
        for (int i = 1; i <= 5; ++i) {
            BlockPosition blockposition1 = blockposition.below(i);
            IBlockData iblockdata = world.getBlockState(blockposition1);

            if (isLitCampfire(iblockdata)) {
                return true;
            }

            boolean flag = VoxelShapes.joinIsNotEmpty(BlockCampfire.SHAPE_VIRTUAL_POST, iblockdata.getCollisionShape(world, blockposition, VoxelShapeCollision.empty()), OperatorBoolean.AND);

            if (flag) {
                IBlockData iblockdata1 = world.getBlockState(blockposition1.below());

                return isLitCampfire(iblockdata1);
            }
        }

        return false;
    }

    public static boolean isLitCampfire(IBlockData iblockdata) {
        return iblockdata.hasProperty(BlockCampfire.LIT) && iblockdata.is(TagsBlock.CAMPFIRES) && (Boolean) iblockdata.getValue(BlockCampfire.LIT);
    }

    @Override
    protected Fluid getFluidState(IBlockData iblockdata) {
        return (Boolean) iblockdata.getValue(BlockCampfire.WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(iblockdata);
    }

    @Override
    protected IBlockData rotate(IBlockData iblockdata, EnumBlockRotation enumblockrotation) {
        return (IBlockData) iblockdata.setValue(BlockCampfire.FACING, enumblockrotation.rotate((EnumDirection) iblockdata.getValue(BlockCampfire.FACING)));
    }

    @Override
    protected IBlockData mirror(IBlockData iblockdata, EnumBlockMirror enumblockmirror) {
        return iblockdata.rotate(enumblockmirror.getRotation((EnumDirection) iblockdata.getValue(BlockCampfire.FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockCampfire.LIT, BlockCampfire.SIGNAL_FIRE, BlockCampfire.WATERLOGGED, BlockCampfire.FACING);
    }

    @Override
    public TileEntity newBlockEntity(BlockPosition blockposition, IBlockData iblockdata) {
        return new TileEntityCampfire(blockposition, iblockdata);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData iblockdata, TileEntityTypes<T> tileentitytypes) {
        if (world instanceof WorldServer worldserver) {
            if ((Boolean) iblockdata.getValue(BlockCampfire.LIT)) {
                CraftingManager.a<SingleRecipeInput, RecipeCampfire> craftingmanager_a = CraftingManager.<SingleRecipeInput, RecipeCampfire>createCheck(Recipes.CAMPFIRE_COOKING);

                return createTickerHelper(tileentitytypes, TileEntityTypes.CAMPFIRE, (world1, blockposition, iblockdata1, tileentitycampfire) -> {
                    TileEntityCampfire.cookTick(worldserver, blockposition, iblockdata1, tileentitycampfire, craftingmanager_a);
                });
            } else {
                return createTickerHelper(tileentitytypes, TileEntityTypes.CAMPFIRE, TileEntityCampfire::cooldownTick);
            }
        } else {
            return (Boolean) iblockdata.getValue(BlockCampfire.LIT) ? createTickerHelper(tileentitytypes, TileEntityTypes.CAMPFIRE, TileEntityCampfire::particleTick) : null;
        }
    }

    @Override
    protected boolean isPathfindable(IBlockData iblockdata, PathMode pathmode) {
        return false;
    }

    public static boolean canLight(IBlockData iblockdata) {
        return iblockdata.is(TagsBlock.CAMPFIRES, (blockbase_blockdata) -> {
            return blockbase_blockdata.hasProperty(BlockCampfire.WATERLOGGED) && blockbase_blockdata.hasProperty(BlockCampfire.LIT);
        }) && !(Boolean) iblockdata.getValue(BlockCampfire.WATERLOGGED) && !(Boolean) iblockdata.getValue(BlockCampfire.LIT);
    }
}
