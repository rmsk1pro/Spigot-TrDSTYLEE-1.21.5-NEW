package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.function.Function;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChatComponent;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentTime;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.EntityPlayer;

public class CommandTitle {

    public CommandTitle() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher, CommandBuildContext commandbuildcontext) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("title").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.literal("clear").executes((commandcontext) -> {
            return clearTitle((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("reset").executes((commandcontext) -> {
            return resetTitle((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("title").then(net.minecraft.commands.CommandDispatcher.argument("title", ArgumentChatComponent.textComponent(commandbuildcontext)).executes((commandcontext) -> {
            return showTitle((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), ArgumentChatComponent.getRawComponent(commandcontext, "title"), "title", ClientboundSetTitleTextPacket::new);
        })))).then(net.minecraft.commands.CommandDispatcher.literal("subtitle").then(net.minecraft.commands.CommandDispatcher.argument("title", ArgumentChatComponent.textComponent(commandbuildcontext)).executes((commandcontext) -> {
            return showTitle((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), ArgumentChatComponent.getRawComponent(commandcontext, "title"), "subtitle", ClientboundSetSubtitleTextPacket::new);
        })))).then(net.minecraft.commands.CommandDispatcher.literal("actionbar").then(net.minecraft.commands.CommandDispatcher.argument("title", ArgumentChatComponent.textComponent(commandbuildcontext)).executes((commandcontext) -> {
            return showTitle((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), ArgumentChatComponent.getRawComponent(commandcontext, "title"), "actionbar", ClientboundSetActionBarTextPacket::new);
        })))).then(net.minecraft.commands.CommandDispatcher.literal("times").then(net.minecraft.commands.CommandDispatcher.argument("fadeIn", ArgumentTime.time()).then(net.minecraft.commands.CommandDispatcher.argument("stay", ArgumentTime.time()).then(net.minecraft.commands.CommandDispatcher.argument("fadeOut", ArgumentTime.time()).executes((commandcontext) -> {
            return setTimes((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), IntegerArgumentType.getInteger(commandcontext, "fadeIn"), IntegerArgumentType.getInteger(commandcontext, "stay"), IntegerArgumentType.getInteger(commandcontext, "fadeOut"));
        })))))));
    }

    private static int clearTitle(CommandListenerWrapper commandlistenerwrapper, Collection<EntityPlayer> collection) {
        ClientboundClearTitlesPacket clientboundcleartitlespacket = new ClientboundClearTitlesPacket(false);

        for (EntityPlayer entityplayer : collection) {
            entityplayer.connection.send(clientboundcleartitlespacket);
        }

        if (collection.size() == 1) {
            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.title.cleared.single", ((EntityPlayer) collection.iterator().next()).getDisplayName());
            }, true);
        } else {
            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.title.cleared.multiple", collection.size());
            }, true);
        }

        return collection.size();
    }

    private static int resetTitle(CommandListenerWrapper commandlistenerwrapper, Collection<EntityPlayer> collection) {
        ClientboundClearTitlesPacket clientboundcleartitlespacket = new ClientboundClearTitlesPacket(true);

        for (EntityPlayer entityplayer : collection) {
            entityplayer.connection.send(clientboundcleartitlespacket);
        }

        if (collection.size() == 1) {
            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.title.reset.single", ((EntityPlayer) collection.iterator().next()).getDisplayName());
            }, true);
        } else {
            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.title.reset.multiple", collection.size());
            }, true);
        }

        return collection.size();
    }

    private static int showTitle(CommandListenerWrapper commandlistenerwrapper, Collection<EntityPlayer> collection, IChatBaseComponent ichatbasecomponent, String s, Function<IChatBaseComponent, Packet<?>> function) throws CommandSyntaxException {
        for (EntityPlayer entityplayer : collection) {
            entityplayer.connection.send((Packet) function.apply(ChatComponentUtils.updateForEntity(commandlistenerwrapper, ichatbasecomponent, entityplayer, 0)));
        }

        if (collection.size() == 1) {
            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.title.show." + s + ".single", ((EntityPlayer) collection.iterator().next()).getDisplayName());
            }, true);
        } else {
            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.title.show." + s + ".multiple", collection.size());
            }, true);
        }

        return collection.size();
    }

    private static int setTimes(CommandListenerWrapper commandlistenerwrapper, Collection<EntityPlayer> collection, int i, int j, int k) {
        ClientboundSetTitlesAnimationPacket clientboundsettitlesanimationpacket = new ClientboundSetTitlesAnimationPacket(i, j, k);

        for (EntityPlayer entityplayer : collection) {
            entityplayer.connection.send(clientboundsettitlesanimationpacket);
        }

        if (collection.size() == 1) {
            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.title.times.single", ((EntityPlayer) collection.iterator().next()).getDisplayName());
            }, true);
        } else {
            commandlistenerwrapper.sendSuccess(() -> {
                return IChatBaseComponent.translatable("commands.title.times.multiple", collection.size());
            }, true);
        }

        return collection.size();
    }
}
