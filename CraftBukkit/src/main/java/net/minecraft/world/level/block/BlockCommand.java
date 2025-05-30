package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.UtilColor;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.CommandBlockListenerAbstract;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityCommand;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import org.slf4j.Logger;

import org.bukkit.event.block.BlockRedstoneEvent; // CraftBukkit

public class BlockCommand extends BlockTileEntity implements GameMasterBlock {

    public static final MapCodec<BlockCommand> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(Codec.BOOL.fieldOf("automatic").forGetter((blockcommand) -> {
            return blockcommand.automatic;
        }), propertiesCodec()).apply(instance, BlockCommand::new);
    });
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final BlockStateEnum<EnumDirection> FACING = BlockDirectional.FACING;
    public static final BlockStateBoolean CONDITIONAL = BlockProperties.CONDITIONAL;
    private final boolean automatic;

    @Override
    public MapCodec<BlockCommand> codec() {
        return BlockCommand.CODEC;
    }

    public BlockCommand(boolean flag, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) ((IBlockData) (this.stateDefinition.any()).setValue(BlockCommand.FACING, EnumDirection.NORTH)).setValue(BlockCommand.CONDITIONAL, false));
        this.automatic = flag;
    }

    @Override
    public TileEntity newBlockEntity(BlockPosition blockposition, IBlockData iblockdata) {
        TileEntityCommand tileentitycommand = new TileEntityCommand(blockposition, iblockdata);

        tileentitycommand.setAutomatic(this.automatic);
        return tileentitycommand;
    }

    @Override
    protected void neighborChanged(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, @Nullable Orientation orientation, boolean flag) {
        if (!world.isClientSide) {
            TileEntity tileentity = world.getBlockEntity(blockposition);

            if (tileentity instanceof TileEntityCommand) {
                TileEntityCommand tileentitycommand = (TileEntityCommand) tileentity;

                this.setPoweredAndUpdate(world, blockposition, tileentitycommand, world.hasNeighborSignal(blockposition));
            }

        }
    }

    private void setPoweredAndUpdate(World world, BlockPosition blockposition, TileEntityCommand tileentitycommand, boolean flag) {
        boolean flag1 = tileentitycommand.isPowered();
        // CraftBukkit start
        org.bukkit.block.Block bukkitBlock = world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
        int old = flag1 ? 15 : 0;
        int current = flag ? 15 : 0;

        BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(bukkitBlock, old, current);
        world.getCraftServer().getPluginManager().callEvent(eventRedstone);
        flag = eventRedstone.getNewCurrent() > 0;
        // CraftBukkit end

        if (flag != flag1) {
            tileentitycommand.setPowered(flag);
            if (flag) {
                if (tileentitycommand.isAutomatic() || tileentitycommand.getMode() == TileEntityCommand.Type.SEQUENCE) {
                    return;
                }

                tileentitycommand.markConditionMet();
                world.scheduleTick(blockposition, (Block) this, 1);
            }

        }
    }

    @Override
    protected void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        TileEntity tileentity = worldserver.getBlockEntity(blockposition);

        if (tileentity instanceof TileEntityCommand tileentitycommand) {
            CommandBlockListenerAbstract commandblocklistenerabstract = tileentitycommand.getCommandBlock();
            boolean flag = !UtilColor.isNullOrEmpty(commandblocklistenerabstract.getCommand());
            TileEntityCommand.Type tileentitycommand_type = tileentitycommand.getMode();
            boolean flag1 = tileentitycommand.wasConditionMet();

            if (tileentitycommand_type == TileEntityCommand.Type.AUTO) {
                tileentitycommand.markConditionMet();
                if (flag1) {
                    this.execute(iblockdata, worldserver, blockposition, commandblocklistenerabstract, flag);
                } else if (tileentitycommand.isConditional()) {
                    commandblocklistenerabstract.setSuccessCount(0);
                }

                if (tileentitycommand.isPowered() || tileentitycommand.isAutomatic()) {
                    worldserver.scheduleTick(blockposition, (Block) this, 1);
                }
            } else if (tileentitycommand_type == TileEntityCommand.Type.REDSTONE) {
                if (flag1) {
                    this.execute(iblockdata, worldserver, blockposition, commandblocklistenerabstract, flag);
                } else if (tileentitycommand.isConditional()) {
                    commandblocklistenerabstract.setSuccessCount(0);
                }
            }

            worldserver.updateNeighbourForOutputSignal(blockposition, this);
        }

    }

    private void execute(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, CommandBlockListenerAbstract commandblocklistenerabstract, boolean flag) {
        if (flag) {
            commandblocklistenerabstract.performCommand(worldserver);
        } else {
            commandblocklistenerabstract.setSuccessCount(0);
        }

        executeChain(worldserver, blockposition, (EnumDirection) iblockdata.getValue(BlockCommand.FACING));
    }

    @Override
    protected EnumInteractionResult useWithoutItem(IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, MovingObjectPositionBlock movingobjectpositionblock) {
        TileEntity tileentity = world.getBlockEntity(blockposition);

        if (tileentity instanceof TileEntityCommand && entityhuman.canUseGameMasterBlocks()) {
            entityhuman.openCommandBlock((TileEntityCommand) tileentity);
            return EnumInteractionResult.SUCCESS;
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(IBlockData iblockdata) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(IBlockData iblockdata, World world, BlockPosition blockposition) {
        TileEntity tileentity = world.getBlockEntity(blockposition);

        return tileentity instanceof TileEntityCommand ? ((TileEntityCommand) tileentity).getCommandBlock().getSuccessCount() : 0;
    }

    @Override
    public void setPlacedBy(World world, BlockPosition blockposition, IBlockData iblockdata, EntityLiving entityliving, ItemStack itemstack) {
        TileEntity tileentity = world.getBlockEntity(blockposition);

        if (tileentity instanceof TileEntityCommand tileentitycommand) {
            CommandBlockListenerAbstract commandblocklistenerabstract = tileentitycommand.getCommandBlock();

            if (world instanceof WorldServer worldserver) {
                if (!itemstack.has(DataComponents.BLOCK_ENTITY_DATA)) {
                    commandblocklistenerabstract.setTrackOutput(worldserver.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK));
                    tileentitycommand.setAutomatic(this.automatic);
                }

                boolean flag = world.hasNeighborSignal(blockposition);

                this.setPoweredAndUpdate(world, blockposition, tileentitycommand, flag);
            }

        }
    }

    @Override
    protected IBlockData rotate(IBlockData iblockdata, EnumBlockRotation enumblockrotation) {
        return (IBlockData) iblockdata.setValue(BlockCommand.FACING, enumblockrotation.rotate((EnumDirection) iblockdata.getValue(BlockCommand.FACING)));
    }

    @Override
    protected IBlockData mirror(IBlockData iblockdata, EnumBlockMirror enumblockmirror) {
        return iblockdata.rotate(enumblockmirror.getRotation((EnumDirection) iblockdata.getValue(BlockCommand.FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockCommand.FACING, BlockCommand.CONDITIONAL);
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        return (IBlockData) this.defaultBlockState().setValue(BlockCommand.FACING, blockactioncontext.getNearestLookingDirection().getOpposite());
    }

    private static void executeChain(WorldServer worldserver, BlockPosition blockposition, EnumDirection enumdirection) {
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = blockposition.mutable();
        GameRules gamerules = worldserver.getGameRules();

        int i;
        IBlockData iblockdata;

        for (i = gamerules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH); i-- > 0; enumdirection = (EnumDirection) iblockdata.getValue(BlockCommand.FACING)) {
            blockposition_mutableblockposition.move(enumdirection);
            iblockdata = worldserver.getBlockState(blockposition_mutableblockposition);
            Block block = iblockdata.getBlock();

            if (!iblockdata.is(Blocks.CHAIN_COMMAND_BLOCK)) {
                break;
            }

            TileEntity tileentity = worldserver.getBlockEntity(blockposition_mutableblockposition);

            if (!(tileentity instanceof TileEntityCommand)) {
                break;
            }

            TileEntityCommand tileentitycommand = (TileEntityCommand) tileentity;

            if (tileentitycommand.getMode() != TileEntityCommand.Type.SEQUENCE) {
                break;
            }

            if (tileentitycommand.isPowered() || tileentitycommand.isAutomatic()) {
                CommandBlockListenerAbstract commandblocklistenerabstract = tileentitycommand.getCommandBlock();

                if (tileentitycommand.markConditionMet()) {
                    if (!commandblocklistenerabstract.performCommand(worldserver)) {
                        break;
                    }

                    worldserver.updateNeighbourForOutputSignal(blockposition_mutableblockposition, block);
                } else if (tileentitycommand.isConditional()) {
                    commandblocklistenerabstract.setSuccessCount(0);
                }
            }
        }

        if (i <= 0) {
            int j = Math.max(gamerules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH), 0);

            BlockCommand.LOGGER.warn("Command Block chain tried to execute more than {} steps!", j);
        }

    }
}
