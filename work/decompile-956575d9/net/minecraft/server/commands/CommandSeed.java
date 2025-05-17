package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.IChatBaseComponent;

public class CommandSeed {

    public CommandSeed() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher, boolean flag) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("seed").requires((commandlistenerwrapper) -> {
            return !flag || commandlistenerwrapper.hasPermission(2);
        })).executes((commandcontext) -> {
            long i = ((CommandListenerWrapper) commandcontext.getSource()).getLevel().getSeed();
            IChatBaseComponent ichatbasecomponent = ChatComponentUtils.copyOnClickText(String.valueOf(i));

            ((CommandListenerWrapper) commandcontext.getSource()).sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.seed.success", ichatbasecomponent);
            }, false);
            return (int) i;
        }));
    }
}
