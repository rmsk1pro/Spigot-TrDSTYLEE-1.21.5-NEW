package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.Particles;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.entity.TileEntity;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class DecoratedPotBlock extends BlockTileEntity implements IBlockWaterlogged {

    public static final MapCodec<DecoratedPotBlock> CODEC = simpleCodec(DecoratedPotBlock::new);
    public static final MinecraftKey SHERDS_DYNAMIC_DROP_ID = MinecraftKey.withDefaultNamespace("sherds");
    public static final BlockStateEnum<EnumDirection> HORIZONTAL_FACING = BlockProperties.HORIZONTAL_FACING;
    public static final BlockStateBoolean CRACKED = BlockProperties.CRACKED;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(14.0D, 0.0D, 16.0D);

    @Override
    public MapCodec<DecoratedPotBlock> codec() {
        return DecoratedPotBlock.CODEC;
    }

    protected DecoratedPotBlock(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(DecoratedPotBlock.HORIZONTAL_FACING, EnumDirection.NORTH)).setValue(DecoratedPotBlock.WATERLOGGED, false)).setValue(DecoratedPotBlock.CRACKED, false));
    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        if ((Boolean) iblockdata.getValue(DecoratedPotBlock.WATERLOGGED)) {
            scheduledtickaccess.scheduleTick(blockposition, (FluidType) FluidTypes.WATER, FluidTypes.WATER.getTickDelay(iworldreader));
        }

        return super.updateShape(iblockdata, iworldreader, scheduledtickaccess, blockposition, enumdirection, blockposition1, iblockdata1, randomsource);
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        Fluid fluid = blockactioncontext.getLevel().getFluidState(blockactioncontext.getClickedPos());

        return (IBlockData) ((IBlockData) ((IBlockData) this.defaultBlockState().setValue(DecoratedPotBlock.HORIZONTAL_FACING, blockactioncontext.getHorizontalDirection())).setValue(DecoratedPotBlock.WATERLOGGED, fluid.getType() == FluidTypes.WATER)).setValue(DecoratedPotBlock.CRACKED, false);
    }

    @Override
    protected EnumInteractionResult useItemOn(ItemStack itemstack, IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, EnumHand enumhand, MovingObjectPositionBlock movingobjectpositionblock) {
        TileEntity tileentity = world.getBlockEntity(blockposition);

        if (tileentity instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
            if (world.isClientSide) {
                return EnumInteractionResult.SUCCESS;
            } else {
                ItemStack itemstack1 = decoratedpotblockentity.getTheItem();

                if (!itemstack.isEmpty() && (itemstack1.isEmpty() || ItemStack.isSameItemSameComponents(itemstack1, itemstack) && itemstack1.getCount() < itemstack1.getMaxStackSize())) {
                    decoratedpotblockentity.wobble(DecoratedPotBlockEntity.a.POSITIVE);
                    entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                    ItemStack itemstack2 = itemstack.consumeAndReturn(1, entityhuman);
                    float f;

                    if (decoratedpotblockentity.isEmpty()) {
                        decoratedpotblockentity.setTheItem(itemstack2);
                        f = (float) itemstack2.getCount() / (float) itemstack2.getMaxStackSize();
                    } else {
                        itemstack1.grow(1);
                        f = (float) itemstack1.getCount() / (float) itemstack1.getMaxStackSize();
                    }

                    world.playSound((Entity) null, blockposition, SoundEffects.DECORATED_POT_INSERT, SoundCategory.BLOCKS, 1.0F, 0.7F + 0.5F * f);
                    if (world instanceof WorldServer) {
                        WorldServer worldserver = (WorldServer) world;

                        worldserver.sendParticles(Particles.DUST_PLUME, (double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 1.2D, (double) blockposition.getZ() + 0.5D, 7, 0.0D, 0.0D, 0.0D, 0.0D);
                    }

                    decoratedpotblockentity.setChanged();
                    world.gameEvent(entityhuman, (Holder) GameEvent.BLOCK_CHANGE, blockposition);
                    return EnumInteractionResult.SUCCESS;
                } else {
                    return EnumInteractionResult.TRY_WITH_EMPTY_HAND;
                }
            }
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    protected EnumInteractionResult useWithoutItem(IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, MovingObjectPositionBlock movingobjectpositionblock) {
        TileEntity tileentity = world.getBlockEntity(blockposition);

        if (tileentity instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
            world.playSound((Entity) null, blockposition, SoundEffects.DECORATED_POT_INSERT_FAIL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            decoratedpotblockentity.wobble(DecoratedPotBlockEntity.a.NEGATIVE);
            world.gameEvent(entityhuman, (Holder) GameEvent.BLOCK_CHANGE, blockposition);
            return EnumInteractionResult.SUCCESS;
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    protected boolean isPathfindable(IBlockData iblockdata, PathMode pathmode) {
        return false;
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return DecoratedPotBlock.SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(DecoratedPotBlock.HORIZONTAL_FACING, DecoratedPotBlock.WATERLOGGED, DecoratedPotBlock.CRACKED);
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(BlockPosition blockposition, IBlockData iblockdata) {
        return new DecoratedPotBlockEntity(blockposition, iblockdata);
    }

    @Override
    protected void affectNeighborsAfterRemoval(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, boolean flag) {
        InventoryUtils.updateNeighboursAfterDestroy(iblockdata, worldserver, blockposition);
    }

    @Override
    protected List<ItemStack> getDrops(IBlockData iblockdata, LootParams.a lootparams_a) {
        TileEntity tileentity = (TileEntity) lootparams_a.getOptionalParameter(LootContextParameters.BLOCK_ENTITY);

        if (tileentity instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
            lootparams_a.withDynamicDrop(DecoratedPotBlock.SHERDS_DYNAMIC_DROP_ID, (consumer) -> {
                for (Item item : decoratedpotblockentity.getDecorations().ordered()) {
                    consumer.accept(item.getDefaultInstance());
                }

            });
        }

        return super.getDrops(iblockdata, lootparams_a);
    }

    @Override
    public IBlockData playerWillDestroy(World world, BlockPosition blockposition, IBlockData iblockdata, EntityHuman entityhuman) {
        ItemStack itemstack = entityhuman.getMainHandItem();
        IBlockData iblockdata1 = iblockdata;

        if (itemstack.is(TagsItem.BREAKS_DECORATED_POTS) && !EnchantmentManager.hasTag(itemstack, EnchantmentTags.PREVENTS_DECORATED_POT_SHATTERING)) {
            iblockdata1 = (IBlockData) iblockdata.setValue(DecoratedPotBlock.CRACKED, true);
            world.setBlock(blockposition, iblockdata1, 260);
        }

        return super.playerWillDestroy(world, blockposition, iblockdata1, entityhuman);
    }

    @Override
    protected Fluid getFluidState(IBlockData iblockdata) {
        return (Boolean) iblockdata.getValue(DecoratedPotBlock.WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(iblockdata);
    }

    @Override
    protected SoundEffectType getSoundType(IBlockData iblockdata) {
        return (Boolean) iblockdata.getValue(DecoratedPotBlock.CRACKED) ? SoundEffectType.DECORATED_POT_CRACKED : SoundEffectType.DECORATED_POT;
    }

    @Override
    protected void onProjectileHit(World world, IBlockData iblockdata, MovingObjectPositionBlock movingobjectpositionblock, IProjectile iprojectile) {
        BlockPosition blockposition = movingobjectpositionblock.getBlockPos();

        if (world instanceof WorldServer worldserver) {
            if (iprojectile.mayInteract(worldserver, blockposition) && iprojectile.mayBreak(worldserver)) {
                // CraftBukkit start - call EntityChangeBlockEvent
                if (!org.bukkit.craftbukkit.event.CraftEventFactory.callEntityChangeBlockEvent(iprojectile, blockposition, this.getFluidState(iblockdata).createLegacyBlock())) {
                    return;
                }
                // CraftBukkit end
                world.setBlock(blockposition, (IBlockData) iblockdata.setValue(DecoratedPotBlock.CRACKED, true), 260);
                world.destroyBlock(blockposition, true, iprojectile);
            }
        }

    }

    @Override
    protected ItemStack getCloneItemStack(IWorldReader iworldreader, BlockPosition blockposition, IBlockData iblockdata, boolean flag) {
        TileEntity tileentity = iworldreader.getBlockEntity(blockposition);

        if (tileentity instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
            PotDecorations potdecorations = decoratedpotblockentity.getDecorations();

            return DecoratedPotBlockEntity.createDecoratedPotItem(potdecorations);
        } else {
            return super.getCloneItemStack(iworldreader, blockposition, iblockdata, flag);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(IBlockData iblockdata) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(IBlockData iblockdata, World world, BlockPosition blockposition) {
        return Container.getRedstoneSignalFromBlockEntity(world.getBlockEntity(blockposition));
    }

    @Override
    protected IBlockData rotate(IBlockData iblockdata, EnumBlockRotation enumblockrotation) {
        return (IBlockData) iblockdata.setValue(DecoratedPotBlock.HORIZONTAL_FACING, enumblockrotation.rotate((EnumDirection) iblockdata.getValue(DecoratedPotBlock.HORIZONTAL_FACING)));
    }

    @Override
    protected IBlockData mirror(IBlockData iblockdata, EnumBlockMirror enumblockmirror) {
        return iblockdata.rotate(enumblockmirror.getRotation((EnumDirection) iblockdata.getValue(DecoratedPotBlock.HORIZONTAL_FACING)));
    }
}
