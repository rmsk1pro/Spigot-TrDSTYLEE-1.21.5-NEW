package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.blocks.ArgumentTile;
import net.minecraft.commands.arguments.blocks.ArgumentTileLocation;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;

public class CommandSetBlock {

    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(IChatBaseComponent.translatable("commands.setblock.failed"));

    public CommandSetBlock() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher, CommandBuildContext commandbuildcontext) {
        Predicate<ShapeDetectorBlock> predicate = (shapedetectorblock) -> {
            return shapedetectorblock.getLevel().isEmptyBlock(shapedetectorblock.getPos());
        };

        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("setblock").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentPosition.blockPos()).then(((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("block", ArgumentTile.block(commandbuildcontext)).executes((commandcontext) -> {
            return setBlock((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "pos"), ArgumentTile.getBlock(commandcontext, "block"), CommandSetBlock.Mode.REPLACE, (Predicate) null, false);
        })).then(net.minecraft.commands.CommandDispatcher.literal("destroy").executes((commandcontext) -> {
            return setBlock((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "pos"), ArgumentTile.getBlock(commandcontext, "block"), CommandSetBlock.Mode.DESTROY, (Predicate) null, false);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("keep").executes((commandcontext) -> {
            return setBlock((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "pos"), ArgumentTile.getBlock(commandcontext, "block"), CommandSetBlock.Mode.REPLACE, predicate, false);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("replace").executes((commandcontext) -> {
            return setBlock((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "pos"), ArgumentTile.getBlock(commandcontext, "block"), CommandSetBlock.Mode.REPLACE, (Predicate) null, false);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("strict").executes((commandcontext) -> {
            return setBlock((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "pos"), ArgumentTile.getBlock(commandcontext, "block"), CommandSetBlock.Mode.REPLACE, (Predicate) null, true);
        })))));
    }

    private static int setBlock(CommandListenerWrapper commandlistenerwrapper, BlockPosition blockposition, ArgumentTileLocation argumenttilelocation, CommandSetBlock.Mode commandsetblock_mode, @Nullable Predicate<ShapeDetectorBlock> predicate, boolean flag) throws CommandSyntaxException {
        WorldServer worldserver = commandlistenerwrapper.getLevel();

        if (worldserver.isDebug()) {
            throw CommandSetBlock.ERROR_FAILED.create();
        } else if (predicate != null && !predicate.test(new ShapeDetectorBlock(worldserver, blockposition, true))) {
            throw CommandSetBlock.ERROR_FAILED.create();
        } else {
            boolean flag1;

            if (commandsetblock_mode == CommandSetBlock.Mode.DESTROY) {
                worldserver.destroyBlock(blockposition, true);
                flag1 = !argumenttilelocation.getState().isAir() || !worldserver.getBlockState(blockposition).isAir();
            } else {
                flag1 = true;
            }

            if (flag1 && !argumenttilelocation.place(worldserver, blockposition, 2 | (flag ? 816 : 256))) {
                throw CommandSetBlock.ERROR_FAILED.create();
            } else {
                if (!flag) {
                    worldserver.updateNeighborsAt(blockposition, argumenttilelocation.getState().getBlock());
                }

                commandlistenerwrapper.sendSuccess(() -> {
                    return IChatBaseComponent.translatable("commands.setblock.success", blockposition.getX(), blockposition.getY(), blockposition.getZ());
                }, true);
                return 1;
            }
        }
    }

    public static enum Mode {

        REPLACE, DESTROY;

        private Mode() {}
    }
}
