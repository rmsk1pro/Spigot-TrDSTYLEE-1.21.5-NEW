package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;

public class SpawnArmorTrimsCommand {

    private static final List<ResourceKey<TrimPattern>> VANILLA_TRIM_PATTERNS = List.of(TrimPatterns.SENTRY, TrimPatterns.DUNE, TrimPatterns.COAST, TrimPatterns.WILD, TrimPatterns.WARD, TrimPatterns.EYE, TrimPatterns.VEX, TrimPatterns.TIDE, TrimPatterns.SNOUT, TrimPatterns.RIB, TrimPatterns.SPIRE, TrimPatterns.WAYFINDER, TrimPatterns.SHAPER, TrimPatterns.SILENCE, TrimPatterns.RAISER, TrimPatterns.HOST, TrimPatterns.FLOW, TrimPatterns.BOLT);
    private static final List<ResourceKey<TrimMaterial>> VANILLA_TRIM_MATERIALS = List.of(TrimMaterials.QUARTZ, TrimMaterials.IRON, TrimMaterials.NETHERITE, TrimMaterials.REDSTONE, TrimMaterials.COPPER, TrimMaterials.GOLD, TrimMaterials.EMERALD, TrimMaterials.DIAMOND, TrimMaterials.LAPIS, TrimMaterials.AMETHYST, TrimMaterials.RESIN);
    private static final ToIntFunction<ResourceKey<TrimPattern>> TRIM_PATTERN_ORDER = SystemUtils.<ResourceKey<TrimPattern>>createIndexLookup(SpawnArmorTrimsCommand.VANILLA_TRIM_PATTERNS);
    private static final ToIntFunction<ResourceKey<TrimMaterial>> TRIM_MATERIAL_ORDER = SystemUtils.<ResourceKey<TrimMaterial>>createIndexLookup(SpawnArmorTrimsCommand.VANILLA_TRIM_MATERIALS);
    private static final DynamicCommandExceptionType ERROR_INVALID_PATTERN = new DynamicCommandExceptionType((object) -> {
        return IChatBaseComponent.translatableEscape("Invalid pattern", object);
    });

    public SpawnArmorTrimsCommand() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("spawn_armor_trims").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(net.minecraft.commands.CommandDispatcher.literal("*_lag_my_game").executes((commandcontext) -> {
            return spawnAllArmorTrims((CommandListenerWrapper) commandcontext.getSource(), ((CommandListenerWrapper) commandcontext.getSource()).getPlayerOrException());
        }))).then(net.minecraft.commands.CommandDispatcher.argument("pattern", ResourceKeyArgument.key(Registries.TRIM_PATTERN)).executes((commandcontext) -> {
            return spawnArmorTrim((CommandListenerWrapper) commandcontext.getSource(), ((CommandListenerWrapper) commandcontext.getSource()).getPlayerOrException(), ResourceKeyArgument.getRegistryKey(commandcontext, "pattern", Registries.TRIM_PATTERN, SpawnArmorTrimsCommand.ERROR_INVALID_PATTERN));
        })));
    }

    private static int spawnAllArmorTrims(CommandListenerWrapper commandlistenerwrapper, EntityHuman entityhuman) {
        return spawnArmorTrims(commandlistenerwrapper, entityhuman, commandlistenerwrapper.getServer().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).listElements());
    }

    private static int spawnArmorTrim(CommandListenerWrapper commandlistenerwrapper, EntityHuman entityhuman, ResourceKey<TrimPattern> resourcekey) {
        return spawnArmorTrims(commandlistenerwrapper, entityhuman, Stream.of((Holder.c) commandlistenerwrapper.getServer().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).get(resourcekey).orElseThrow()));
    }

    private static int spawnArmorTrims(CommandListenerWrapper commandlistenerwrapper, EntityHuman entityhuman, Stream<Holder.c<TrimPattern>> stream) {
        WorldServer worldserver = commandlistenerwrapper.getLevel();
        List<Holder.c<TrimPattern>> list = stream.sorted(Comparator.comparing((holder_c) -> {
            return SpawnArmorTrimsCommand.TRIM_PATTERN_ORDER.applyAsInt(holder_c.key());
        })).toList();
        List<Holder.c<TrimMaterial>> list1 = worldserver.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).listElements().sorted(Comparator.comparing((holder_c) -> {
            return SpawnArmorTrimsCommand.TRIM_MATERIAL_ORDER.applyAsInt(holder_c.key());
        })).toList();
        List<Holder.c<Item>> list2 = findEquippableItemsWithAssets(worldserver.registryAccess().lookupOrThrow(Registries.ITEM));
        BlockPosition blockposition = entityhuman.blockPosition().relative(entityhuman.getDirection(), 5);
        double d0 = 3.0D;

        for (int i = 0; i < list1.size(); ++i) {
            Holder.c<TrimMaterial> holder_c = (Holder.c) list1.get(i);

            for (int j = 0; j < list.size(); ++j) {
                Holder.c<TrimPattern> holder_c1 = (Holder.c) list.get(j);
                ArmorTrim armortrim = new ArmorTrim(holder_c, holder_c1);

                for (int k = 0; k < list2.size(); ++k) {
                    Holder.c<Item> holder_c2 = (Holder.c) list2.get(k);
                    double d1 = (double) blockposition.getX() + 0.5D - (double) k * 3.0D;
                    double d2 = (double) blockposition.getY() + 0.5D + (double) i * 3.0D;
                    double d3 = (double) blockposition.getZ() + 0.5D + (double) (j * 10);
                    EntityArmorStand entityarmorstand = new EntityArmorStand(worldserver, d1, d2, d3);

                    entityarmorstand.setYRot(180.0F);
                    entityarmorstand.setNoGravity(true);
                    ItemStack itemstack = new ItemStack(holder_c2);
                    Equippable equippable = (Equippable) Objects.requireNonNull((Equippable) itemstack.get(DataComponents.EQUIPPABLE));

                    itemstack.set(DataComponents.TRIM, armortrim);
                    entityarmorstand.setItemSlot(equippable.slot(), itemstack);
                    if (k == 0) {
                        entityarmorstand.setCustomName(((TrimPattern) armortrim.pattern().value()).copyWithStyle(armortrim.material()).copy().append(" & ").append(((TrimMaterial) armortrim.material().value()).description()));
                        entityarmorstand.setCustomNameVisible(true);
                    } else {
                        entityarmorstand.setInvisible(true);
                    }

                    worldserver.addFreshEntity(entityarmorstand);
                }
            }
        }

        commandlistenerwrapper.sendSuccess(() -> {
            return IChatBaseComponent.literal("Armorstands with trimmed armor spawned around you");
        }, true);
        return 1;
    }

    private static List<Holder.c<Item>> findEquippableItemsWithAssets(HolderLookup<Item> holderlookup) {
        List<Holder.c<Item>> list = new ArrayList();

        holderlookup.listElements().forEach((holder_c) -> {
            Equippable equippable = (Equippable) ((Item) holder_c.value()).components().get(DataComponents.EQUIPPABLE);

            if (equippable != null && equippable.slot().getType() == EnumItemSlot.Function.HUMANOID_ARMOR && equippable.assetId().isPresent()) {
                list.add(holder_c);
            }

        });
        return list;
    }
}
