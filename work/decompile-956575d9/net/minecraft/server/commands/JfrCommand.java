package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.EnumChatFormat;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;

public class JfrCommand {

    private static final SimpleCommandExceptionType START_FAILED = new SimpleCommandExceptionType(IChatBaseComponent.translatable("commands.jfr.start.failed"));
    private static final DynamicCommandExceptionType DUMP_FAILED = new DynamicCommandExceptionType((object) -> {
        return IChatBaseComponent.translatableEscape("commands.jfr.dump.failed", object);
    });

    private JfrCommand() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("jfr").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(4);
        })).then(net.minecraft.commands.CommandDispatcher.literal("start").executes((commandcontext) -> {
            return startJfr((CommandListenerWrapper) commandcontext.getSource());
        }))).then(net.minecraft.commands.CommandDispatcher.literal("stop").executes((commandcontext) -> {
            return stopJfr((CommandListenerWrapper) commandcontext.getSource());
        })));
    }

    private static int startJfr(CommandListenerWrapper commandlistenerwrapper) throws CommandSyntaxException {
        Environment environment = Environment.from(commandlistenerwrapper.getServer());

        if (!JvmProfiler.INSTANCE.start(environment)) {
            throw JfrCommand.START_FAILED.create();
        } else {
            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.jfr.started");
            }, false);
            return 1;
        }
    }

    private static int stopJfr(CommandListenerWrapper commandlistenerwrapper) throws CommandSyntaxException {
        try {
            Path path = Paths.get(".").relativize(JvmProfiler.INSTANCE.stop().normalize());
            Path path1 = commandlistenerwrapper.getServer().isPublished() && !SharedConstants.IS_RUNNING_IN_IDE ? path : path.toAbsolutePath();
            IChatBaseComponent ichatbasecomponent = IChatBaseComponent.literal(path.toString()).withStyle(EnumChatFormat.UNDERLINE).withStyle((chatmodifier) -> {
                return chatmodifier.withClickEvent(new ChatClickable.CopyToClipboard(path1.toString())).withHoverEvent(new ChatHoverable.e(IChatBaseComponent.translatable("chat.copy.click")));
            });

            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.jfr.stopped", ichatbasecomponent);
            }, false);
            return 1;
        } catch (Throwable throwable) {
            throw JfrCommand.DUMP_FAILED.create(throwable.getMessage());
        }
    }
}
