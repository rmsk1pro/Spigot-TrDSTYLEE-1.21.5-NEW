package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;

public class CommandSaveOn {

    private static final SimpleCommandExceptionType ERROR_ALREADY_ON = new SimpleCommandExceptionType(IChatBaseComponent.translatable("commands.save.alreadyOn"));

    public CommandSaveOn() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("save-on").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(4);
        })).executes((commandcontext) -> {
            CommandListenerWrapper commandlistenerwrapper = (CommandListenerWrapper) commandcontext.getSource();
            boolean flag = false;

            for (WorldServer worldserver : commandlistenerwrapper.getServer().getAllLevels()) {
                if (worldserver != null && worldserver.noSave) {
                    worldserver.noSave = false;
                    flag = true;
                }
            }

            if (!flag) {
                throw CommandSaveOn.ERROR_ALREADY_ON.create();
            } else {
                commandlistenerwrapper.sendSuccess(() -> {
                    return IChatBaseComponent.translatable("commands.save.enabled");
                }, true);
                return 1;
            }
        }));
    }
}
