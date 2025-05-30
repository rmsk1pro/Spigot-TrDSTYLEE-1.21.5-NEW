package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.selector.ArgumentParserSelector;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;

public class ArgumentProfile implements ArgumentType<ArgumentProfile.a> {

    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "dd12be42-52a9-4a91-a8a1-11c01849e498", "@e");
    public static final SimpleCommandExceptionType ERROR_UNKNOWN_PLAYER = new SimpleCommandExceptionType(IChatBaseComponent.translatable("argument.player.unknown"));

    public ArgumentProfile() {}

    public static Collection<GameProfile> getGameProfiles(CommandContext<CommandListenerWrapper> commandcontext, String s) throws CommandSyntaxException {
        return ((ArgumentProfile.a) commandcontext.getArgument(s, ArgumentProfile.a.class)).getNames((CommandListenerWrapper) commandcontext.getSource());
    }

    public static ArgumentProfile gameProfile() {
        return new ArgumentProfile();
    }

    public <S> ArgumentProfile.a parse(StringReader stringreader, S s0) throws CommandSyntaxException {
        return parse(stringreader, ArgumentParserSelector.allowSelectors(s0));
    }

    public ArgumentProfile.a parse(StringReader stringreader) throws CommandSyntaxException {
        return parse(stringreader, true);
    }

    private static ArgumentProfile.a parse(StringReader stringreader, boolean flag) throws CommandSyntaxException {
        if (stringreader.canRead() && stringreader.peek() == '@') {
            ArgumentParserSelector argumentparserselector = new ArgumentParserSelector(stringreader, flag);
            EntitySelector entityselector = argumentparserselector.parse();

            if (entityselector.includesEntities()) {
                throw ArgumentEntity.ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(stringreader);
            } else {
                return new ArgumentProfile.b(entityselector);
            }
        } else {
            int i = stringreader.getCursor();

            while (stringreader.canRead() && stringreader.peek() != ' ') {
                stringreader.skip();
            }

            String s = stringreader.getString().substring(i, stringreader.getCursor());

            return (commandlistenerwrapper) -> {
                Optional<GameProfile> optional = commandlistenerwrapper.getServer().getProfileCache().get(s);
                SimpleCommandExceptionType simplecommandexceptiontype = ArgumentProfile.ERROR_UNKNOWN_PLAYER;

                Objects.requireNonNull(simplecommandexceptiontype);
                return Collections.singleton((GameProfile) optional.orElseThrow(simplecommandexceptiontype::create));
            };
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandcontext, SuggestionsBuilder suggestionsbuilder) {
        Object object = commandcontext.getSource();

        if (object instanceof ICompletionProvider icompletionprovider) {
            StringReader stringreader = new StringReader(suggestionsbuilder.getInput());

            stringreader.setCursor(suggestionsbuilder.getStart());
            ArgumentParserSelector argumentparserselector = new ArgumentParserSelector(stringreader, ArgumentParserSelector.allowSelectors(icompletionprovider));

            try {
                argumentparserselector.parse();
            } catch (CommandSyntaxException commandsyntaxexception) {
                ;
            }

            return argumentparserselector.fillSuggestions(suggestionsbuilder, (suggestionsbuilder1) -> {
                ICompletionProvider.suggest(icompletionprovider.getOnlinePlayerNames(), suggestionsbuilder1);
            });
        } else {
            return Suggestions.empty();
        }
    }

    public Collection<String> getExamples() {
        return ArgumentProfile.EXAMPLES;
    }

    public static class b implements ArgumentProfile.a {

        private final EntitySelector selector;

        public b(EntitySelector entityselector) {
            this.selector = entityselector;
        }

        @Override
        public Collection<GameProfile> getNames(CommandListenerWrapper commandlistenerwrapper) throws CommandSyntaxException {
            List<EntityPlayer> list = this.selector.findPlayers(commandlistenerwrapper);

            if (list.isEmpty()) {
                throw ArgumentEntity.NO_PLAYERS_FOUND.create();
            } else {
                List<GameProfile> list1 = Lists.newArrayList();

                for (EntityPlayer entityplayer : list) {
                    list1.add(entityplayer.getGameProfile());
                }

                return list1;
            }
        }
    }

    @FunctionalInterface
    public interface a {

        Collection<GameProfile> getNames(CommandListenerWrapper commandlistenerwrapper) throws CommandSyntaxException;
    }
}
