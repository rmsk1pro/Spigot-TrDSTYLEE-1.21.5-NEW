package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChatComponent;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.server.level.EntityPlayer;

public class CommandTellRaw {

    public CommandTellRaw() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher, CommandBuildContext commandbuildcontext) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("tellraw").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.argument("message", ArgumentChatComponent.textComponent(commandbuildcontext)).executes((commandcontext) -> {
            int i = 0;

            for (EntityPlayer entityplayer : ArgumentEntity.getPlayers(commandcontext, "targets")) {
                entityplayer.sendSystemMessage(ArgumentChatComponent.getResolvedComponent(commandcontext, "message", entityplayer), false);
                ++i;
            }

            return i;
        }))));
    }
}
