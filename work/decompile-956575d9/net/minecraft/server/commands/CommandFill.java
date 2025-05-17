package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.blocks.ArgumentBlockPredicate;
import net.minecraft.commands.arguments.blocks.ArgumentTile;
import net.minecraft.commands.arguments.blocks.ArgumentTileLocation;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;

public class CommandFill {

    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((object, object1) -> {
        return IChatBaseComponent.translatableEscape("commands.fill.toobig", object, object1);
    });
    static final ArgumentTileLocation HOLLOW_CORE = new ArgumentTileLocation(Blocks.AIR.defaultBlockState(), Collections.emptySet(), (NBTTagCompound) null);
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(IChatBaseComponent.translatable("commands.fill.failed"));

    public CommandFill() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher, CommandBuildContext commandbuildcontext) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("fill").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(net.minecraft.commands.CommandDispatcher.argument("from", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("to", ArgumentPosition.blockPos()).then(wrapWithMode(commandbuildcontext, net.minecraft.commands.CommandDispatcher.argument("block", ArgumentTile.block(commandbuildcontext)), (commandcontext) -> {
            return ArgumentPosition.getLoadedBlockPos(commandcontext, "from");
        }, (commandcontext) -> {
            return ArgumentPosition.getLoadedBlockPos(commandcontext, "to");
        }, (commandcontext) -> {
            return ArgumentTile.getBlock(commandcontext, "block");
        }, (commandcontext) -> {
            return null;
        }).then(((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("replace").executes((commandcontext) -> {
            return fillBlocks((CommandListenerWrapper) commandcontext.getSource(), StructureBoundingBox.fromCorners(ArgumentPosition.getLoadedBlockPos(commandcontext, "from"), ArgumentPosition.getLoadedBlockPos(commandcontext, "to")), ArgumentTile.getBlock(commandcontext, "block"), CommandFill.Mode.REPLACE, (Predicate) null, false);
        })).then(wrapWithMode(commandbuildcontext, net.minecraft.commands.CommandDispatcher.argument("filter", ArgumentBlockPredicate.blockPredicate(commandbuildcontext)), (commandcontext) -> {
            return ArgumentPosition.getLoadedBlockPos(commandcontext, "from");
        }, (commandcontext) -> {
            return ArgumentPosition.getLoadedBlockPos(commandcontext, "to");
        }, (commandcontext) -> {
            return ArgumentTile.getBlock(commandcontext, "block");
        }, (commandcontext) -> {
            return ArgumentBlockPredicate.getBlockPredicate(commandcontext, "filter");
        }))).then(net.minecraft.commands.CommandDispatcher.literal("keep").executes((commandcontext) -> {
            return fillBlocks((CommandListenerWrapper) commandcontext.getSource(), StructureBoundingBox.fromCorners(ArgumentPosition.getLoadedBlockPos(commandcontext, "from"), ArgumentPosition.getLoadedBlockPos(commandcontext, "to")), ArgumentTile.getBlock(commandcontext, "block"), CommandFill.Mode.REPLACE, (shapedetectorblock) -> {
                return shapedetectorblock.getLevel().isEmptyBlock(shapedetectorblock.getPos());
            }, false);
        }))))));
    }

    private static ArgumentBuilder<CommandListenerWrapper, ?> wrapWithMode(CommandBuildContext commandbuildcontext, ArgumentBuilder<CommandListenerWrapper, ?> argumentbuilder, InCommandFunction<CommandContext<CommandListenerWrapper>, BlockPosition> incommandfunction, InCommandFunction<CommandContext<CommandListenerWrapper>, BlockPosition> incommandfunction1, InCommandFunction<CommandContext<CommandListenerWrapper>, ArgumentTileLocation> incommandfunction2, CommandFill.d<CommandContext<CommandListenerWrapper>, Predicate<ShapeDetectorBlock>> commandfill_d) {
        return argumentbuilder.executes((commandcontext) -> {
            return fillBlocks((CommandListenerWrapper) commandcontext.getSource(), StructureBoundingBox.fromCorners(incommandfunction.apply(commandcontext), incommandfunction1.apply(commandcontext)), incommandfunction2.apply(commandcontext), CommandFill.Mode.REPLACE, commandfill_d.apply(commandcontext), false);
        }).then(net.minecraft.commands.CommandDispatcher.literal("outline").executes((commandcontext) -> {
            return fillBlocks((CommandListenerWrapper) commandcontext.getSource(), StructureBoundingBox.fromCorners(incommandfunction.apply(commandcontext), incommandfunction1.apply(commandcontext)), incommandfunction2.apply(commandcontext), CommandFill.Mode.OUTLINE, commandfill_d.apply(commandcontext), false);
        })).then(net.minecraft.commands.CommandDispatcher.literal("hollow").executes((commandcontext) -> {
            return fillBlocks((CommandListenerWrapper) commandcontext.getSource(), StructureBoundingBox.fromCorners(incommandfunction.apply(commandcontext), incommandfunction1.apply(commandcontext)), incommandfunction2.apply(commandcontext), CommandFill.Mode.HOLLOW, commandfill_d.apply(commandcontext), false);
        })).then(net.minecraft.commands.CommandDispatcher.literal("destroy").executes((commandcontext) -> {
            return fillBlocks((CommandListenerWrapper) commandcontext.getSource(), StructureBoundingBox.fromCorners(incommandfunction.apply(commandcontext), incommandfunction1.apply(commandcontext)), incommandfunction2.apply(commandcontext), CommandFill.Mode.DESTROY, commandfill_d.apply(commandcontext), false);
        })).then(net.minecraft.commands.CommandDispatcher.literal("strict").executes((commandcontext) -> {
            return fillBlocks((CommandListenerWrapper) commandcontext.getSource(), StructureBoundingBox.fromCorners(incommandfunction.apply(commandcontext), incommandfunction1.apply(commandcontext)), incommandfunction2.apply(commandcontext), CommandFill.Mode.REPLACE, commandfill_d.apply(commandcontext), true);
        }));
    }

    private static int fillBlocks(CommandListenerWrapper commandlistenerwrapper, StructureBoundingBox structureboundingbox, ArgumentTileLocation argumenttilelocation, CommandFill.Mode commandfill_mode, @Nullable Predicate<ShapeDetectorBlock> predicate, boolean flag) throws CommandSyntaxException {
        int i = structureboundingbox.getXSpan() * structureboundingbox.getYSpan() * structureboundingbox.getZSpan();
        int j = commandlistenerwrapper.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);

        if (i > j) {
            throw CommandFill.ERROR_AREA_TOO_LARGE.create(j, i);
        } else {
            List<BlockPosition> list = Lists.newArrayList();
            WorldServer worldserver = commandlistenerwrapper.getLevel();

            if (worldserver.isDebug()) {
                throw CommandFill.ERROR_FAILED.create();
            } else {
                int k = 0;

                for (BlockPosition blockposition : BlockPosition.betweenClosed(structureboundingbox.minX(), structureboundingbox.minY(), structureboundingbox.minZ(), structureboundingbox.maxX(), structureboundingbox.maxY(), structureboundingbox.maxZ())) {
                    if (predicate == null || predicate.test(new ShapeDetectorBlock(worldserver, blockposition, true))) {
                        boolean flag1 = false;

                        if (commandfill_mode.affector.affect(worldserver, blockposition)) {
                            flag1 = true;
                        }

                        ArgumentTileLocation argumenttilelocation1 = commandfill_mode.filter.filter(structureboundingbox, blockposition, argumenttilelocation, worldserver);

                        if (argumenttilelocation1 == null) {
                            if (flag1) {
                                ++k;
                            }
                        } else if (!argumenttilelocation1.place(worldserver, blockposition, 2 | (flag ? 816 : 256))) {
                            if (flag1) {
                                ++k;
                            }
                        } else {
                            if (!flag) {
                                list.add(blockposition.immutable());
                            }

                            ++k;
                        }
                    }
                }

                for (BlockPosition blockposition1 : list) {
                    Block block = worldserver.getBlockState(blockposition1).getBlock();

                    worldserver.updateNeighborsAt(blockposition1, block);
                }

                if (k == 0) {
                    throw CommandFill.ERROR_FAILED.create();
                } else {
                    commandlistenerwrapper.sendSuccess(() -> {
                        return IChatBaseComponent.translatable("commands.fill.success", k);
                    }, true);
                    return k;
                }
            }
        }
    }

    private static enum Mode {

        REPLACE(CommandFill.a.NOOP, CommandFill.b.NOOP), OUTLINE(CommandFill.a.NOOP, (structureboundingbox, blockposition, argumenttilelocation, worldserver) -> {
            return blockposition.getX() != structureboundingbox.minX() && blockposition.getX() != structureboundingbox.maxX() && blockposition.getY() != structureboundingbox.minY() && blockposition.getY() != structureboundingbox.maxY() && blockposition.getZ() != structureboundingbox.minZ() && blockposition.getZ() != structureboundingbox.maxZ() ? null : argumenttilelocation;
        }), HOLLOW(CommandFill.a.NOOP, (structureboundingbox, blockposition, argumenttilelocation, worldserver) -> {
            return blockposition.getX() != structureboundingbox.minX() && blockposition.getX() != structureboundingbox.maxX() && blockposition.getY() != structureboundingbox.minY() && blockposition.getY() != structureboundingbox.maxY() && blockposition.getZ() != structureboundingbox.minZ() && blockposition.getZ() != structureboundingbox.maxZ() ? CommandFill.HOLLOW_CORE : argumenttilelocation;
        }), DESTROY((worldserver, blockposition) -> {
            return worldserver.destroyBlock(blockposition, true);
        }, CommandFill.b.NOOP);

        public final CommandFill.b filter;
        public final CommandFill.a affector;

        private Mode(final CommandFill.a commandfill_a, final CommandFill.b commandfill_b) {
            this.affector = commandfill_a;
            this.filter = commandfill_b;
        }
    }

    @FunctionalInterface
    public interface b {

        CommandFill.b NOOP = (structureboundingbox, blockposition, argumenttilelocation, worldserver) -> {
            return argumenttilelocation;
        };

        @Nullable
        ArgumentTileLocation filter(StructureBoundingBox structureboundingbox, BlockPosition blockposition, ArgumentTileLocation argumenttilelocation, WorldServer worldserver);
    }

    @FunctionalInterface
    public interface a {

        CommandFill.a NOOP = (worldserver, blockposition) -> {
            return false;
        };

        boolean affect(WorldServer worldserver, BlockPosition blockposition);
    }

    @FunctionalInterface
    private interface d<T, R> {

        @Nullable
        R apply(T t0) throws CommandSyntaxException;
    }
}
