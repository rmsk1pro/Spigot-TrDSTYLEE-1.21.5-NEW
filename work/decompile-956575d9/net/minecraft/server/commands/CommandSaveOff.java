package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;

public class CommandSaveOff {

    private static final SimpleCommandExceptionType ERROR_ALREADY_OFF = new SimpleCommandExceptionType(IChatBaseComponent.translatable("commands.save.alreadyOff"));

    public CommandSaveOff() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("save-off").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(4);
        })).executes((commandcontext) -> {
            CommandListenerWrapper commandlistenerwrapper = (CommandListenerWrapper) commandcontext.getSource();
            boolean flag = false;

            for (WorldServer worldserver : commandlistenerwrapper.getServer().getAllLevels()) {
                if (worldserver != null && !worldserver.noSave) {
                    worldserver.noSave = true;
                    flag = true;
                }
            }

            if (!flag) {
                throw CommandSaveOff.ERROR_ALREADY_OFF.create();
            } else {
                commandlistenerwrapper.sendSuccess(() -> {
                    return IChatBaseComponent.translatable("commands.save.disabled");
                }, true);
                return 1;
            }
        }));
    }
}
