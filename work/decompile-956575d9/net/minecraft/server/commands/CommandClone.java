package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentDimension;
import net.minecraft.commands.arguments.blocks.ArgumentBlockPredicate;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;

public class CommandClone {

    private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(IChatBaseComponent.translatable("commands.clone.overlap"));
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((object, object1) -> {
        return IChatBaseComponent.translatableEscape("commands.clone.toobig", object, object1);
    });
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(IChatBaseComponent.translatable("commands.clone.failed"));
    public static final Predicate<ShapeDetectorBlock> FILTER_AIR = (shapedetectorblock) -> {
        return !shapedetectorblock.getState().isAir();
    };

    public CommandClone() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher, CommandBuildContext commandbuildcontext) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("clone").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(beginEndDestinationAndModeSuffix(commandbuildcontext, (commandcontext) -> {
            return ((CommandListenerWrapper) commandcontext.getSource()).getLevel();
        }))).then(net.minecraft.commands.CommandDispatcher.literal("from").then(net.minecraft.commands.CommandDispatcher.argument("sourceDimension", ArgumentDimension.dimension()).then(beginEndDestinationAndModeSuffix(commandbuildcontext, (commandcontext) -> {
            return ArgumentDimension.getDimension(commandcontext, "sourceDimension");
        })))));
    }

    private static ArgumentBuilder<CommandListenerWrapper, ?> beginEndDestinationAndModeSuffix(CommandBuildContext commandbuildcontext, InCommandFunction<CommandContext<CommandListenerWrapper>, WorldServer> incommandfunction) {
        return net.minecraft.commands.CommandDispatcher.argument("begin", ArgumentPosition.blockPos()).then(((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("end", ArgumentPosition.blockPos()).then(destinationAndStrictSuffix(commandbuildcontext, incommandfunction, (commandcontext) -> {
            return ((CommandListenerWrapper) commandcontext.getSource()).getLevel();
        }))).then(net.minecraft.commands.CommandDispatcher.literal("to").then(net.minecraft.commands.CommandDispatcher.argument("targetDimension", ArgumentDimension.dimension()).then(destinationAndStrictSuffix(commandbuildcontext, incommandfunction, (commandcontext) -> {
            return ArgumentDimension.getDimension(commandcontext, "targetDimension");
        })))));
    }

    private static CommandClone.c getLoadedDimensionAndPosition(CommandContext<CommandListenerWrapper> commandcontext, WorldServer worldserver, String s) throws CommandSyntaxException {
        BlockPosition blockposition = ArgumentPosition.getLoadedBlockPos(commandcontext, worldserver, s);

        return new CommandClone.c(worldserver, blockposition);
    }

    private static ArgumentBuilder<CommandListenerWrapper, ?> destinationAndStrictSuffix(CommandBuildContext commandbuildcontext, InCommandFunction<CommandContext<CommandListenerWrapper>, WorldServer> incommandfunction, InCommandFunction<CommandContext<CommandListenerWrapper>, WorldServer> incommandfunction1) {
        InCommandFunction<CommandContext<CommandListenerWrapper>, CommandClone.c> incommandfunction2 = (commandcontext) -> {
            return getLoadedDimensionAndPosition(commandcontext, incommandfunction.apply(commandcontext), "begin");
        };
        InCommandFunction<CommandContext<CommandListenerWrapper>, CommandClone.c> incommandfunction3 = (commandcontext) -> {
            return getLoadedDimensionAndPosition(commandcontext, incommandfunction.apply(commandcontext), "end");
        };
        InCommandFunction<CommandContext<CommandListenerWrapper>, CommandClone.c> incommandfunction4 = (commandcontext) -> {
            return getLoadedDimensionAndPosition(commandcontext, incommandfunction1.apply(commandcontext), "destination");
        };

        return modeSuffix(commandbuildcontext, incommandfunction2, incommandfunction3, incommandfunction4, false, net.minecraft.commands.CommandDispatcher.argument("destination", ArgumentPosition.blockPos())).then(modeSuffix(commandbuildcontext, incommandfunction2, incommandfunction3, incommandfunction4, true, net.minecraft.commands.CommandDispatcher.literal("strict")));
    }

    private static ArgumentBuilder<CommandListenerWrapper, ?> modeSuffix(CommandBuildContext commandbuildcontext, InCommandFunction<CommandContext<CommandListenerWrapper>, CommandClone.c> incommandfunction, InCommandFunction<CommandContext<CommandListenerWrapper>, CommandClone.c> incommandfunction1, InCommandFunction<CommandContext<CommandListenerWrapper>, CommandClone.c> incommandfunction2, boolean flag, ArgumentBuilder<CommandListenerWrapper, ?> argumentbuilder) {
        return argumentbuilder.executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), incommandfunction.apply(commandcontext), incommandfunction1.apply(commandcontext), incommandfunction2.apply(commandcontext), (shapedetectorblock) -> {
                return true;
            }, CommandClone.Mode.NORMAL, flag);
        }).then(wrapWithCloneMode(incommandfunction, incommandfunction1, incommandfunction2, (commandcontext) -> {
            return (shapedetectorblock) -> {
                return true;
            };
        }, flag, net.minecraft.commands.CommandDispatcher.literal("replace"))).then(wrapWithCloneMode(incommandfunction, incommandfunction1, incommandfunction2, (commandcontext) -> {
            return CommandClone.FILTER_AIR;
        }, flag, net.minecraft.commands.CommandDispatcher.literal("masked"))).then(net.minecraft.commands.CommandDispatcher.literal("filtered").then(wrapWithCloneMode(incommandfunction, incommandfunction1, incommandfunction2, (commandcontext) -> {
            return ArgumentBlockPredicate.getBlockPredicate(commandcontext, "filter");
        }, flag, net.minecraft.commands.CommandDispatcher.argument("filter", ArgumentBlockPredicate.blockPredicate(commandbuildcontext)))));
    }

    private static ArgumentBuilder<CommandListenerWrapper, ?> wrapWithCloneMode(InCommandFunction<CommandContext<CommandListenerWrapper>, CommandClone.c> incommandfunction, InCommandFunction<CommandContext<CommandListenerWrapper>, CommandClone.c> incommandfunction1, InCommandFunction<CommandContext<CommandListenerWrapper>, CommandClone.c> incommandfunction2, InCommandFunction<CommandContext<CommandListenerWrapper>, Predicate<ShapeDetectorBlock>> incommandfunction3, boolean flag, ArgumentBuilder<CommandListenerWrapper, ?> argumentbuilder) {
        return argumentbuilder.executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), incommandfunction.apply(commandcontext), incommandfunction1.apply(commandcontext), incommandfunction2.apply(commandcontext), incommandfunction3.apply(commandcontext), CommandClone.Mode.NORMAL, flag);
        }).then(net.minecraft.commands.CommandDispatcher.literal("force").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), incommandfunction.apply(commandcontext), incommandfunction1.apply(commandcontext), incommandfunction2.apply(commandcontext), incommandfunction3.apply(commandcontext), CommandClone.Mode.FORCE, flag);
        })).then(net.minecraft.commands.CommandDispatcher.literal("move").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), incommandfunction.apply(commandcontext), incommandfunction1.apply(commandcontext), incommandfunction2.apply(commandcontext), incommandfunction3.apply(commandcontext), CommandClone.Mode.MOVE, flag);
        })).then(net.minecraft.commands.CommandDispatcher.literal("normal").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), incommandfunction.apply(commandcontext), incommandfunction1.apply(commandcontext), incommandfunction2.apply(commandcontext), incommandfunction3.apply(commandcontext), CommandClone.Mode.NORMAL, flag);
        }));
    }

    private static int clone(CommandListenerWrapper commandlistenerwrapper, CommandClone.c commandclone_c, CommandClone.c commandclone_c1, CommandClone.c commandclone_c2, Predicate<ShapeDetectorBlock> predicate, CommandClone.Mode commandclone_mode, boolean flag) throws CommandSyntaxException {
        BlockPosition blockposition = commandclone_c.position();
        BlockPosition blockposition1 = commandclone_c1.position();
        StructureBoundingBox structureboundingbox = StructureBoundingBox.fromCorners(blockposition, blockposition1);
        BlockPosition blockposition2 = commandclone_c2.position();
        BlockPosition blockposition3 = blockposition2.offset(structureboundingbox.getLength());
        StructureBoundingBox structureboundingbox1 = StructureBoundingBox.fromCorners(blockposition2, blockposition3);
        WorldServer worldserver = commandclone_c.dimension();
        WorldServer worldserver1 = commandclone_c2.dimension();

        if (!commandclone_mode.canOverlap() && worldserver == worldserver1 && structureboundingbox1.intersects(structureboundingbox)) {
            throw CommandClone.ERROR_OVERLAP.create();
        } else {
            int i = structureboundingbox.getXSpan() * structureboundingbox.getYSpan() * structureboundingbox.getZSpan();
            int j = commandlistenerwrapper.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);

            if (i > j) {
                throw CommandClone.ERROR_AREA_TOO_LARGE.create(j, i);
            } else if (worldserver.hasChunksAt(blockposition, blockposition1) && worldserver1.hasChunksAt(blockposition2, blockposition3)) {
                if (worldserver1.isDebug()) {
                    throw CommandClone.ERROR_FAILED.create();
                } else {
                    List<CommandClone.CommandCloneStoredTileEntity> list = Lists.newArrayList();
                    List<CommandClone.CommandCloneStoredTileEntity> list1 = Lists.newArrayList();
                    List<CommandClone.CommandCloneStoredTileEntity> list2 = Lists.newArrayList();
                    Deque<BlockPosition> deque = Lists.newLinkedList();
                    BlockPosition blockposition4 = new BlockPosition(structureboundingbox1.minX() - structureboundingbox.minX(), structureboundingbox1.minY() - structureboundingbox.minY(), structureboundingbox1.minZ() - structureboundingbox.minZ());

                    for (int k = structureboundingbox.minZ(); k <= structureboundingbox.maxZ(); ++k) {
                        for (int l = structureboundingbox.minY(); l <= structureboundingbox.maxY(); ++l) {
                            for (int i1 = structureboundingbox.minX(); i1 <= structureboundingbox.maxX(); ++i1) {
                                BlockPosition blockposition5 = new BlockPosition(i1, l, k);
                                BlockPosition blockposition6 = blockposition5.offset(blockposition4);
                                ShapeDetectorBlock shapedetectorblock = new ShapeDetectorBlock(worldserver, blockposition5, false);
                                IBlockData iblockdata = shapedetectorblock.getState();

                                if (predicate.test(shapedetectorblock)) {
                                    TileEntity tileentity = worldserver.getBlockEntity(blockposition5);

                                    if (tileentity != null) {
                                        CommandClone.a commandclone_a = new CommandClone.a(tileentity.saveCustomOnly(commandlistenerwrapper.registryAccess()), tileentity.components());

                                        list1.add(new CommandClone.CommandCloneStoredTileEntity(blockposition6, iblockdata, commandclone_a));
                                        deque.addLast(blockposition5);
                                    } else if (!iblockdata.isSolidRender() && !iblockdata.isCollisionShapeFullBlock(worldserver, blockposition5)) {
                                        list2.add(new CommandClone.CommandCloneStoredTileEntity(blockposition6, iblockdata, (CommandClone.a) null));
                                        deque.addFirst(blockposition5);
                                    } else {
                                        list.add(new CommandClone.CommandCloneStoredTileEntity(blockposition6, iblockdata, (CommandClone.a) null));
                                        deque.addLast(blockposition5);
                                    }
                                }
                            }
                        }
                    }

                    int j1 = 2 | (flag ? 816 : 0);

                    if (commandclone_mode == CommandClone.Mode.MOVE) {
                        for (BlockPosition blockposition7 : deque) {
                            worldserver.setBlock(blockposition7, Blocks.BARRIER.defaultBlockState(), j1 | 816);
                        }

                        int k1 = flag ? j1 : 3;

                        for (BlockPosition blockposition8 : deque) {
                            worldserver.setBlock(blockposition8, Blocks.AIR.defaultBlockState(), k1);
                        }
                    }

                    List<CommandClone.CommandCloneStoredTileEntity> list3 = Lists.newArrayList();

                    list3.addAll(list);
                    list3.addAll(list1);
                    list3.addAll(list2);
                    List<CommandClone.CommandCloneStoredTileEntity> list4 = Lists.reverse(list3);

                    for (CommandClone.CommandCloneStoredTileEntity commandclone_commandclonestoredtileentity : list4) {
                        worldserver1.setBlock(commandclone_commandclonestoredtileentity.pos, Blocks.BARRIER.defaultBlockState(), j1 | 816);
                    }

                    int l1 = 0;

                    for (CommandClone.CommandCloneStoredTileEntity commandclone_commandclonestoredtileentity1 : list3) {
                        if (worldserver1.setBlock(commandclone_commandclonestoredtileentity1.pos, commandclone_commandclonestoredtileentity1.state, j1)) {
                            ++l1;
                        }
                    }

                    for (CommandClone.CommandCloneStoredTileEntity commandclone_commandclonestoredtileentity2 : list1) {
                        TileEntity tileentity1 = worldserver1.getBlockEntity(commandclone_commandclonestoredtileentity2.pos);

                        if (commandclone_commandclonestoredtileentity2.blockEntityInfo != null && tileentity1 != null) {
                            tileentity1.loadCustomOnly(commandclone_commandclonestoredtileentity2.blockEntityInfo.tag, worldserver1.registryAccess());
                            tileentity1.setComponents(commandclone_commandclonestoredtileentity2.blockEntityInfo.components);
                            tileentity1.setChanged();
                        }

                        worldserver1.setBlock(commandclone_commandclonestoredtileentity2.pos, commandclone_commandclonestoredtileentity2.state, j1);
                    }

                    if (!flag) {
                        for (CommandClone.CommandCloneStoredTileEntity commandclone_commandclonestoredtileentity3 : list4) {
                            worldserver1.updateNeighborsAt(commandclone_commandclonestoredtileentity3.pos, commandclone_commandclonestoredtileentity3.state.getBlock());
                        }
                    }

                    worldserver1.getBlockTicks().copyAreaFrom(worldserver.getBlockTicks(), structureboundingbox, blockposition4);
                    if (l1 == 0) {
                        throw CommandClone.ERROR_FAILED.create();
                    } else {
                        commandlistenerwrapper.sendSuccess(() -> {
                            return IChatBaseComponent.translatable("commands.clone.success", l1);
                        }, true);
                        return l1;
                    }
                }
            } else {
                throw ArgumentPosition.ERROR_NOT_LOADED.create();
            }
        }
    }

    private static record c(WorldServer dimension, BlockPosition position) {

    }

    private static enum Mode {

        FORCE(true), MOVE(true), NORMAL(false);

        private final boolean canOverlap;

        private Mode(final boolean flag) {
            this.canOverlap = flag;
        }

        public boolean canOverlap() {
            return this.canOverlap;
        }
    }

    private static record a(NBTTagCompound tag, DataComponentMap components) {

    }

    private static record CommandCloneStoredTileEntity(BlockPosition pos, IBlockData state, @Nullable CommandClone.a blockEntityInfo) {

    }
}
