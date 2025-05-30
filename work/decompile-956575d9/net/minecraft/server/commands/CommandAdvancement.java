package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;

public class CommandAdvancement {

    private static final DynamicCommandExceptionType ERROR_NO_ACTION_PERFORMED = new DynamicCommandExceptionType((object) -> {
        return (IChatBaseComponent) object;
    });
    private static final Dynamic2CommandExceptionType ERROR_CRITERION_NOT_FOUND = new Dynamic2CommandExceptionType((object, object1) -> {
        return IChatBaseComponent.translatableEscape("commands.advancement.criterionNotFound", object, object1);
    });

    public CommandAdvancement() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("advancement").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(net.minecraft.commands.CommandDispatcher.literal("grant").then(((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.literal("only").then(((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT)).executes((commandcontext) -> {
            return perform((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.GRANT, getAdvancements(commandcontext, ResourceKeyArgument.getAdvancement(commandcontext, "advancement"), CommandAdvancement.Filter.ONLY));
        })).then(net.minecraft.commands.CommandDispatcher.argument("criterion", StringArgumentType.greedyString()).suggests((commandcontext, suggestionsbuilder) -> {
            return ICompletionProvider.suggest(ResourceKeyArgument.getAdvancement(commandcontext, "advancement").value().criteria().keySet(), suggestionsbuilder);
        }).executes((commandcontext) -> {
            return performCriterion((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.GRANT, ResourceKeyArgument.getAdvancement(commandcontext, "advancement"), StringArgumentType.getString(commandcontext, "criterion"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("from").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT)).executes((commandcontext) -> {
            return perform((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.GRANT, getAdvancements(commandcontext, ResourceKeyArgument.getAdvancement(commandcontext, "advancement"), CommandAdvancement.Filter.FROM));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("until").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT)).executes((commandcontext) -> {
            return perform((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.GRANT, getAdvancements(commandcontext, ResourceKeyArgument.getAdvancement(commandcontext, "advancement"), CommandAdvancement.Filter.UNTIL));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("through").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT)).executes((commandcontext) -> {
            return perform((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.GRANT, getAdvancements(commandcontext, ResourceKeyArgument.getAdvancement(commandcontext, "advancement"), CommandAdvancement.Filter.THROUGH));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("everything").executes((commandcontext) -> {
            return perform((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.GRANT, ((CommandListenerWrapper) commandcontext.getSource()).getServer().getAdvancements().getAllAdvancements(), false);
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("revoke").then(((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.literal("only").then(((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT)).executes((commandcontext) -> {
            return perform((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.REVOKE, getAdvancements(commandcontext, ResourceKeyArgument.getAdvancement(commandcontext, "advancement"), CommandAdvancement.Filter.ONLY));
        })).then(net.minecraft.commands.CommandDispatcher.argument("criterion", StringArgumentType.greedyString()).suggests((commandcontext, suggestionsbuilder) -> {
            return ICompletionProvider.suggest(ResourceKeyArgument.getAdvancement(commandcontext, "advancement").value().criteria().keySet(), suggestionsbuilder);
        }).executes((commandcontext) -> {
            return performCriterion((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.REVOKE, ResourceKeyArgument.getAdvancement(commandcontext, "advancement"), StringArgumentType.getString(commandcontext, "criterion"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("from").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT)).executes((commandcontext) -> {
            return perform((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.REVOKE, getAdvancements(commandcontext, ResourceKeyArgument.getAdvancement(commandcontext, "advancement"), CommandAdvancement.Filter.FROM));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("until").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT)).executes((commandcontext) -> {
            return perform((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.REVOKE, getAdvancements(commandcontext, ResourceKeyArgument.getAdvancement(commandcontext, "advancement"), CommandAdvancement.Filter.UNTIL));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("through").then(net.minecraft.commands.CommandDispatcher.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT)).executes((commandcontext) -> {
            return perform((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.REVOKE, getAdvancements(commandcontext, ResourceKeyArgument.getAdvancement(commandcontext, "advancement"), CommandAdvancement.Filter.THROUGH));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("everything").executes((commandcontext) -> {
            return perform((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntity.getPlayers(commandcontext, "targets"), CommandAdvancement.Action.REVOKE, ((CommandListenerWrapper) commandcontext.getSource()).getServer().getAdvancements().getAllAdvancements());
        })))));
    }

    private static int perform(CommandListenerWrapper commandlistenerwrapper, Collection<EntityPlayer> collection, CommandAdvancement.Action commandadvancement_action, Collection<AdvancementHolder> collection1) throws CommandSyntaxException {
        return perform(commandlistenerwrapper, collection, commandadvancement_action, collection1, true);
    }

    private static int perform(CommandListenerWrapper commandlistenerwrapper, Collection<EntityPlayer> collection, CommandAdvancement.Action commandadvancement_action, Collection<AdvancementHolder> collection1, boolean flag) throws CommandSyntaxException {
        int i = 0;

        for (EntityPlayer entityplayer : collection) {
            i += commandadvancement_action.perform(entityplayer, collection1, flag);
        }

        if (i == 0) {
            if (collection1.size() == 1) {
                if (collection.size() == 1) {
                    throw CommandAdvancement.ERROR_NO_ACTION_PERFORMED.create(IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".one.to.one.failure", Advancement.name((AdvancementHolder) collection1.iterator().next()), ((EntityPlayer) collection.iterator().next()).getDisplayName()));
                } else {
                    throw CommandAdvancement.ERROR_NO_ACTION_PERFORMED.create(IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".one.to.many.failure", Advancement.name((AdvancementHolder) collection1.iterator().next()), collection.size()));
                }
            } else if (collection.size() == 1) {
                throw CommandAdvancement.ERROR_NO_ACTION_PERFORMED.create(IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".many.to.one.failure", collection1.size(), ((EntityPlayer) collection.iterator().next()).getDisplayName()));
            } else {
                throw CommandAdvancement.ERROR_NO_ACTION_PERFORMED.create(IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".many.to.many.failure", collection1.size(), collection.size()));
            }
        } else {
            if (collection1.size() == 1) {
                if (collection.size() == 1) {
                    commandlistenerwrapper.sendSuccess(() -> {
                        return IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".one.to.one.success", Advancement.name((AdvancementHolder) collection1.iterator().next()), ((EntityPlayer) collection.iterator().next()).getDisplayName());
                    }, true);
                } else {
                    commandlistenerwrapper.sendSuccess(() -> {
                        return IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".one.to.many.success", Advancement.name((AdvancementHolder) collection1.iterator().next()), collection.size());
                    }, true);
                }
            } else if (collection.size() == 1) {
                commandlistenerwrapper.sendSuccess(() -> {
                    return IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".many.to.one.success", collection1.size(), ((EntityPlayer) collection.iterator().next()).getDisplayName());
                }, true);
            } else {
                commandlistenerwrapper.sendSuccess(() -> {
                    return IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".many.to.many.success", collection1.size(), collection.size());
                }, true);
            }

            return i;
        }
    }

    private static int performCriterion(CommandListenerWrapper commandlistenerwrapper, Collection<EntityPlayer> collection, CommandAdvancement.Action commandadvancement_action, AdvancementHolder advancementholder, String s) throws CommandSyntaxException {
        int i = 0;
        Advancement advancement = advancementholder.value();

        if (!advancement.criteria().containsKey(s)) {
            throw CommandAdvancement.ERROR_CRITERION_NOT_FOUND.create(Advancement.name(advancementholder), s);
        } else {
            for (EntityPlayer entityplayer : collection) {
                if (commandadvancement_action.performCriterion(entityplayer, advancementholder, s)) {
                    ++i;
                }
            }

            if (i == 0) {
                if (collection.size() == 1) {
                    throw CommandAdvancement.ERROR_NO_ACTION_PERFORMED.create(IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".criterion.to.one.failure", s, Advancement.name(advancementholder), ((EntityPlayer) collection.iterator().next()).getDisplayName()));
                } else {
                    throw CommandAdvancement.ERROR_NO_ACTION_PERFORMED.create(IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".criterion.to.many.failure", s, Advancement.name(advancementholder), collection.size()));
                }
            } else {
                if (collection.size() == 1) {
                    commandlistenerwrapper.sendSuccess(() -> {
                        return IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".criterion.to.one.success", s, Advancement.name(advancementholder), ((EntityPlayer) collection.iterator().next()).getDisplayName());
                    }, true);
                } else {
                    commandlistenerwrapper.sendSuccess(() -> {
                        return IChatBaseComponent.translatable(commandadvancement_action.getKey() + ".criterion.to.many.success", s, Advancement.name(advancementholder), collection.size());
                    }, true);
                }

                return i;
            }
        }
    }

    private static List<AdvancementHolder> getAdvancements(CommandContext<CommandListenerWrapper> commandcontext, AdvancementHolder advancementholder, CommandAdvancement.Filter commandadvancement_filter) {
        AdvancementTree advancementtree = ((CommandListenerWrapper) commandcontext.getSource()).getServer().getAdvancements().tree();
        AdvancementNode advancementnode = advancementtree.get(advancementholder);

        if (advancementnode == null) {
            return List.of(advancementholder);
        } else {
            List<AdvancementHolder> list = new ArrayList();

            if (commandadvancement_filter.parents) {
                for (AdvancementNode advancementnode1 = advancementnode.parent(); advancementnode1 != null; advancementnode1 = advancementnode1.parent()) {
                    list.add(advancementnode1.holder());
                }
            }

            list.add(advancementholder);
            if (commandadvancement_filter.children) {
                addChildren(advancementnode, list);
            }

            return list;
        }
    }

    private static void addChildren(AdvancementNode advancementnode, List<AdvancementHolder> list) {
        for (AdvancementNode advancementnode1 : advancementnode.children()) {
            list.add(advancementnode1.holder());
            addChildren(advancementnode1, list);
        }

    }

    private static enum Action {

        GRANT("grant") {
            @Override
            protected boolean perform(EntityPlayer entityplayer, AdvancementHolder advancementholder) {
                AdvancementProgress advancementprogress = entityplayer.getAdvancements().getOrStartProgress(advancementholder);

                if (advancementprogress.isDone()) {
                    return false;
                } else {
                    for (String s : advancementprogress.getRemainingCriteria()) {
                        entityplayer.getAdvancements().award(advancementholder, s);
                    }

                    return true;
                }
            }

            @Override
            protected boolean performCriterion(EntityPlayer entityplayer, AdvancementHolder advancementholder, String s) {
                return entityplayer.getAdvancements().award(advancementholder, s);
            }
        },
        REVOKE("revoke") {
            @Override
            protected boolean perform(EntityPlayer entityplayer, AdvancementHolder advancementholder) {
                AdvancementProgress advancementprogress = entityplayer.getAdvancements().getOrStartProgress(advancementholder);

                if (!advancementprogress.hasProgress()) {
                    return false;
                } else {
                    for (String s : advancementprogress.getCompletedCriteria()) {
                        entityplayer.getAdvancements().revoke(advancementholder, s);
                    }

                    return true;
                }
            }

            @Override
            protected boolean performCriterion(EntityPlayer entityplayer, AdvancementHolder advancementholder, String s) {
                return entityplayer.getAdvancements().revoke(advancementholder, s);
            }
        };

        private final String key;

        Action(final String s) {
            this.key = "commands.advancement." + s;
        }

        public int perform(EntityPlayer entityplayer, Iterable<AdvancementHolder> iterable, boolean flag) {
            int i = 0;

            if (!flag) {
                entityplayer.getAdvancements().flushDirty(entityplayer, true);
            }

            for (AdvancementHolder advancementholder : iterable) {
                if (this.perform(entityplayer, advancementholder)) {
                    ++i;
                }
            }

            if (!flag) {
                entityplayer.getAdvancements().flushDirty(entityplayer, false);
            }

            return i;
        }

        protected abstract boolean perform(EntityPlayer entityplayer, AdvancementHolder advancementholder);

        protected abstract boolean performCriterion(EntityPlayer entityplayer, AdvancementHolder advancementholder, String s);

        protected String getKey() {
            return this.key;
        }
    }

    private static enum Filter {

        ONLY(false, false), THROUGH(true, true), FROM(false, true), UNTIL(true, false), EVERYTHING(true, true);

        final boolean parents;
        final boolean children;

        private Filter(final boolean flag, final boolean flag1) {
            this.parents = flag;
            this.children = flag1;
        }
    }
}
