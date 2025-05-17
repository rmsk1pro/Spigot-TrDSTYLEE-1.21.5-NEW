package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;

public class CommandGamemode {

    public static final int PERMISSION_LEVEL = 2;

    public CommandGamemode() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("gamemode").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("gamemode", GameModeArgument.gameMode()).executes((commandcontext) -> {
            return setMode(commandcontext, Collections.singleton(((CommandListenerWrapper) commandcontext.getSource()).getPlayerOrException()), GameModeArgument.getGameMode(commandcontext, "gamemode"));
        })).then(net.minecraft.commands.CommandDispatcher.argument("target", ArgumentEntity.players()).executes((commandcontext) -> {
            return setMode(commandcontext, ArgumentEntity.getPlayers(commandcontext, "target"), GameModeArgument.getGameMode(commandcontext, "gamemode"));
        }))));
    }

    private static void logGamemodeChange(CommandListenerWrapper commandlistenerwrapper, EntityPlayer entityplayer, EnumGamemode enumgamemode) {
        IChatBaseComponent ichatbasecomponent = IChatBaseComponent.translatable("gameMode." + enumgamemode.getName());

        if (commandlistenerwrapper.getEntity() == entityplayer) {
            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.gamemode.success.self", ichatbasecomponent);
            }, true);
        } else {
            if (commandlistenerwrapper.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
                entityplayer.sendSystemMessage(IChatBaseComponent.translatable("gameMode.changed", ichatbasecomponent));
            }

            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.gamemode.success.other", entityplayer.getDisplayName(), ichatbasecomponent);
            }, true);
        }

    }

    private static int setMode(CommandContext<CommandListenerWrapper> commandcontext, Collection<EntityPlayer> collection, EnumGamemode enumgamemode) {
        int i = 0;

        for (EntityPlayer entityplayer : collection) {
            if (entityplayer.setGameMode(enumgamemode)) {
                logGamemodeChange((CommandListenerWrapper) commandcontext.getSource(), entityplayer, enumgamemode);
                ++i;
            }
        }

        return i;
    }
}
