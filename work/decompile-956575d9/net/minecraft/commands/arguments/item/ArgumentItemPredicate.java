package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.critereon.CriterionConditionValue;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ArgumentItemPredicate extends ParserBasedArgument<ArgumentItemPredicate.d> {

    private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo:'bar'}");
    static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType((object) -> {
        return IChatBaseComponent.translatableEscape("argument.item.id.invalid", object);
    });
    static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType((object) -> {
        return IChatBaseComponent.translatableEscape("arguments.item.tag.unknown", object);
    });
    static final DynamicCommandExceptionType ERROR_UNKNOWN_COMPONENT = new DynamicCommandExceptionType((object) -> {
        return IChatBaseComponent.translatableEscape("arguments.item.component.unknown", object);
    });
    static final Dynamic2CommandExceptionType ERROR_MALFORMED_COMPONENT = new Dynamic2CommandExceptionType((object, object1) -> {
        return IChatBaseComponent.translatableEscape("arguments.item.component.malformed", object, object1);
    });
    static final DynamicCommandExceptionType ERROR_UNKNOWN_PREDICATE = new DynamicCommandExceptionType((object) -> {
        return IChatBaseComponent.translatableEscape("arguments.item.predicate.unknown", object);
    });
    static final Dynamic2CommandExceptionType ERROR_MALFORMED_PREDICATE = new Dynamic2CommandExceptionType((object, object1) -> {
        return IChatBaseComponent.translatableEscape("arguments.item.predicate.malformed", object, object1);
    });
    private static final MinecraftKey COUNT_ID = MinecraftKey.withDefaultNamespace("count");
    static final Map<MinecraftKey, ArgumentItemPredicate.a> PSEUDO_COMPONENTS = (Map) Stream.of(new ArgumentItemPredicate.a(ArgumentItemPredicate.COUNT_ID, (itemstack) -> {
        return true;
    }, CriterionConditionValue.IntegerRange.CODEC.map((criterionconditionvalue_integerrange) -> {
        return (itemstack) -> {
            return criterionconditionvalue_integerrange.matches(itemstack.getCount());
        };
    }))).collect(Collectors.toUnmodifiableMap(ArgumentItemPredicate.a::id, (argumentitempredicate_a) -> {
        return argumentitempredicate_a;
    }));
    static final Map<MinecraftKey, ArgumentItemPredicate.c> PSEUDO_PREDICATES = (Map) Stream.of(new ArgumentItemPredicate.c(ArgumentItemPredicate.COUNT_ID, CriterionConditionValue.IntegerRange.CODEC.map((criterionconditionvalue_integerrange) -> {
        return (itemstack) -> {
            return criterionconditionvalue_integerrange.matches(itemstack.getCount());
        };
    }))).collect(Collectors.toUnmodifiableMap(ArgumentItemPredicate.c::id, (argumentitempredicate_c) -> {
        return argumentitempredicate_c;
    }));

    public ArgumentItemPredicate(CommandBuildContext commandbuildcontext) {
        super(ComponentPredicateParser.createGrammar(new ArgumentItemPredicate.b(commandbuildcontext)).mapResult((list) -> {
            Predicate predicate = SystemUtils.allOf(list);

            Objects.requireNonNull(predicate);
            return predicate::test;
        }));
    }

    public static ArgumentItemPredicate itemPredicate(CommandBuildContext commandbuildcontext) {
        return new ArgumentItemPredicate(commandbuildcontext);
    }

    public static ArgumentItemPredicate.d getItemPredicate(CommandContext<CommandListenerWrapper> commandcontext, String s) {
        return (ArgumentItemPredicate.d) commandcontext.getArgument(s, ArgumentItemPredicate.d.class);
    }

    public Collection<String> getExamples() {
        return ArgumentItemPredicate.EXAMPLES;
    }

    private static record a(MinecraftKey id, Predicate<ItemStack> presenceChecker, Decoder<? extends Predicate<ItemStack>> valueChecker) {

        public static <T> ArgumentItemPredicate.a create(ImmutableStringReader immutablestringreader, MinecraftKey minecraftkey, DataComponentType<T> datacomponenttype) throws CommandSyntaxException {
            Codec<T> codec = datacomponenttype.codec();

            if (codec == null) {
                throw ArgumentItemPredicate.ERROR_UNKNOWN_COMPONENT.createWithContext(immutablestringreader, minecraftkey);
            } else {
                return new ArgumentItemPredicate.a(minecraftkey, (itemstack) -> {
                    return itemstack.has(datacomponenttype);
                }, codec.map((object) -> {
                    return (itemstack) -> {
                        T t0 = (T) itemstack.get(datacomponenttype);

                        return Objects.equals(object, t0);
                    };
                }));
            }
        }

        public Predicate<ItemStack> decode(ImmutableStringReader immutablestringreader, Dynamic<?> dynamic) throws CommandSyntaxException {
            DataResult<? extends Predicate<ItemStack>> dataresult = this.valueChecker.parse(dynamic);

            return (Predicate) dataresult.getOrThrow((s) -> {
                return ArgumentItemPredicate.ERROR_MALFORMED_COMPONENT.createWithContext(immutablestringreader, this.id.toString(), s);
            });
        }
    }

    private static record c(MinecraftKey id, Decoder<? extends Predicate<ItemStack>> type) {

        public c(Holder.c<DataComponentPredicate.b<?>> holder_c) {
            this(holder_c.key().location(), ((DataComponentPredicate.b) holder_c.value()).codec().map((datacomponentpredicate) -> {
                Objects.requireNonNull(datacomponentpredicate);
                return datacomponentpredicate::matches;
            }));
        }

        public Predicate<ItemStack> decode(ImmutableStringReader immutablestringreader, Dynamic<?> dynamic) throws CommandSyntaxException {
            DataResult<? extends Predicate<ItemStack>> dataresult = this.type.parse(dynamic);

            return (Predicate) dataresult.getOrThrow((s) -> {
                return ArgumentItemPredicate.ERROR_MALFORMED_PREDICATE.createWithContext(immutablestringreader, this.id.toString(), s);
            });
        }
    }

    private static class b implements ComponentPredicateParser.b<Predicate<ItemStack>, ArgumentItemPredicate.a, ArgumentItemPredicate.c> {

        private final HolderLookup.a registries;
        private final HolderLookup.b<Item> items;
        private final HolderLookup.b<DataComponentType<?>> components;
        private final HolderLookup.b<DataComponentPredicate.b<?>> predicates;

        b(HolderLookup.a holderlookup_a) {
            this.registries = holderlookup_a;
            this.items = holderlookup_a.lookupOrThrow(Registries.ITEM);
            this.components = holderlookup_a.lookupOrThrow(Registries.DATA_COMPONENT_TYPE);
            this.predicates = holderlookup_a.lookupOrThrow(Registries.DATA_COMPONENT_PREDICATE_TYPE);
        }

        @Override
        public Predicate<ItemStack> forElementType(ImmutableStringReader immutablestringreader, MinecraftKey minecraftkey) throws CommandSyntaxException {
            Holder.c<Item> holder_c = (Holder.c) this.items.get(ResourceKey.create(Registries.ITEM, minecraftkey)).orElseThrow(() -> {
                return ArgumentItemPredicate.ERROR_UNKNOWN_ITEM.createWithContext(immutablestringreader, minecraftkey);
            });

            return (itemstack) -> {
                return itemstack.is(holder_c);
            };
        }

        @Override
        public Predicate<ItemStack> forTagType(ImmutableStringReader immutablestringreader, MinecraftKey minecraftkey) throws CommandSyntaxException {
            HolderSet<Item> holderset = (HolderSet) this.items.get(TagKey.create(Registries.ITEM, minecraftkey)).orElseThrow(() -> {
                return ArgumentItemPredicate.ERROR_UNKNOWN_TAG.createWithContext(immutablestringreader, minecraftkey);
            });

            return (itemstack) -> {
                return itemstack.is(holderset);
            };
        }

        @Override
        public ArgumentItemPredicate.a lookupComponentType(ImmutableStringReader immutablestringreader, MinecraftKey minecraftkey) throws CommandSyntaxException {
            ArgumentItemPredicate.a argumentitempredicate_a = (ArgumentItemPredicate.a) ArgumentItemPredicate.PSEUDO_COMPONENTS.get(minecraftkey);

            if (argumentitempredicate_a != null) {
                return argumentitempredicate_a;
            } else {
                DataComponentType<?> datacomponenttype = (DataComponentType) this.components.get(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, minecraftkey)).map(Holder::value).orElseThrow(() -> {
                    return ArgumentItemPredicate.ERROR_UNKNOWN_COMPONENT.createWithContext(immutablestringreader, minecraftkey);
                });

                return ArgumentItemPredicate.a.create(immutablestringreader, minecraftkey, datacomponenttype);
            }
        }

        public Predicate<ItemStack> createComponentTest(ImmutableStringReader immutablestringreader, ArgumentItemPredicate.a argumentitempredicate_a, Dynamic<?> dynamic) throws CommandSyntaxException {
            return argumentitempredicate_a.decode(immutablestringreader, RegistryOps.injectRegistryContext(dynamic, this.registries));
        }

        public Predicate<ItemStack> createComponentTest(ImmutableStringReader immutablestringreader, ArgumentItemPredicate.a argumentitempredicate_a) {
            return argumentitempredicate_a.presenceChecker;
        }

        @Override
        public ArgumentItemPredicate.c lookupPredicateType(ImmutableStringReader immutablestringreader, MinecraftKey minecraftkey) throws CommandSyntaxException {
            ArgumentItemPredicate.c argumentitempredicate_c = (ArgumentItemPredicate.c) ArgumentItemPredicate.PSEUDO_PREDICATES.get(minecraftkey);

            return argumentitempredicate_c != null ? argumentitempredicate_c : (ArgumentItemPredicate.c) this.predicates.get(ResourceKey.create(Registries.DATA_COMPONENT_PREDICATE_TYPE, minecraftkey)).map(ArgumentItemPredicate.c::new).orElseThrow(() -> {
                return ArgumentItemPredicate.ERROR_UNKNOWN_PREDICATE.createWithContext(immutablestringreader, minecraftkey);
            });
        }

        public Predicate<ItemStack> createPredicateTest(ImmutableStringReader immutablestringreader, ArgumentItemPredicate.c argumentitempredicate_c, Dynamic<?> dynamic) throws CommandSyntaxException {
            return argumentitempredicate_c.decode(immutablestringreader, RegistryOps.injectRegistryContext(dynamic, this.registries));
        }

        @Override
        public Stream<MinecraftKey> listElementTypes() {
            return this.items.listElementIds().map(ResourceKey::location);
        }

        @Override
        public Stream<MinecraftKey> listTagTypes() {
            return this.items.listTagIds().map(TagKey::location);
        }

        @Override
        public Stream<MinecraftKey> listComponentTypes() {
            return Stream.concat(ArgumentItemPredicate.PSEUDO_COMPONENTS.keySet().stream(), this.components.listElements().filter((holder_c) -> {
                return !((DataComponentType) holder_c.value()).isTransient();
            }).map((holder_c) -> {
                return holder_c.key().location();
            }));
        }

        @Override
        public Stream<MinecraftKey> listPredicateTypes() {
            return Stream.concat(ArgumentItemPredicate.PSEUDO_PREDICATES.keySet().stream(), this.predicates.listElementIds().map(ResourceKey::location));
        }

        public Predicate<ItemStack> negate(Predicate<ItemStack> predicate) {
            return predicate.negate();
        }

        @Override
        public Predicate<ItemStack> anyOf(List<Predicate<ItemStack>> list) {
            return SystemUtils.anyOf(list);
        }
    }

    public interface d extends Predicate<ItemStack> {}
}
