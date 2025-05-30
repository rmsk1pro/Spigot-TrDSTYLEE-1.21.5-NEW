package net.minecraft.world.entity.animal.wolf;

import java.util.Optional;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;

public class WolfVariants {

    public static final ResourceKey<WolfVariant> PALE = createKey("pale");
    public static final ResourceKey<WolfVariant> SPOTTED = createKey("spotted");
    public static final ResourceKey<WolfVariant> SNOWY = createKey("snowy");
    public static final ResourceKey<WolfVariant> BLACK = createKey("black");
    public static final ResourceKey<WolfVariant> ASHEN = createKey("ashen");
    public static final ResourceKey<WolfVariant> RUSTY = createKey("rusty");
    public static final ResourceKey<WolfVariant> WOODS = createKey("woods");
    public static final ResourceKey<WolfVariant> CHESTNUT = createKey("chestnut");
    public static final ResourceKey<WolfVariant> STRIPED = createKey("striped");
    public static final ResourceKey<WolfVariant> DEFAULT = WolfVariants.PALE;

    public WolfVariants() {}

    private static ResourceKey<WolfVariant> createKey(String s) {
        return ResourceKey.create(Registries.WOLF_VARIANT, MinecraftKey.withDefaultNamespace(s));
    }

    private static void register(BootstrapContext<WolfVariant> bootstrapcontext, ResourceKey<WolfVariant> resourcekey, String s, ResourceKey<BiomeBase> resourcekey1) {
        register(bootstrapcontext, resourcekey, s, highPrioBiome(HolderSet.direct(bootstrapcontext.lookup(Registries.BIOME).getOrThrow(resourcekey1))));
    }

    private static void register(BootstrapContext<WolfVariant> bootstrapcontext, ResourceKey<WolfVariant> resourcekey, String s, TagKey<BiomeBase> tagkey) {
        register(bootstrapcontext, resourcekey, s, highPrioBiome(bootstrapcontext.lookup(Registries.BIOME).getOrThrow(tagkey)));
    }

    private static SpawnPrioritySelectors highPrioBiome(HolderSet<BiomeBase> holderset) {
        return SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1);
    }

    private static void register(BootstrapContext<WolfVariant> bootstrapcontext, ResourceKey<WolfVariant> resourcekey, String s, SpawnPrioritySelectors spawnpriorityselectors) {
        MinecraftKey minecraftkey = MinecraftKey.withDefaultNamespace("entity/wolf/" + s);
        MinecraftKey minecraftkey1 = MinecraftKey.withDefaultNamespace("entity/wolf/" + s + "_tame");
        MinecraftKey minecraftkey2 = MinecraftKey.withDefaultNamespace("entity/wolf/" + s + "_angry");

        bootstrapcontext.register(resourcekey, new WolfVariant(new WolfVariant.a(new ClientAsset(minecraftkey), new ClientAsset(minecraftkey1), new ClientAsset(minecraftkey2)), spawnpriorityselectors));
    }

    public static Optional<? extends Holder<WolfVariant>> selectVariantToSpawn(RandomSource randomsource, IRegistryCustom iregistrycustom, SpawnContext spawncontext) {
        return PriorityProvider.pick(iregistrycustom.lookupOrThrow(Registries.WOLF_VARIANT).listElements(), Holder::value, randomsource, spawncontext);
    }

    public static void bootstrap(BootstrapContext<WolfVariant> bootstrapcontext) {
        register(bootstrapcontext, WolfVariants.PALE, "wolf", SpawnPrioritySelectors.fallback(0));
        register(bootstrapcontext, WolfVariants.SPOTTED, "wolf_spotted", BiomeTags.IS_SAVANNA);
        register(bootstrapcontext, WolfVariants.SNOWY, "wolf_snowy", Biomes.GROVE);
        register(bootstrapcontext, WolfVariants.BLACK, "wolf_black", Biomes.OLD_GROWTH_PINE_TAIGA);
        register(bootstrapcontext, WolfVariants.ASHEN, "wolf_ashen", Biomes.SNOWY_TAIGA);
        register(bootstrapcontext, WolfVariants.RUSTY, "wolf_rusty", BiomeTags.IS_JUNGLE);
        register(bootstrapcontext, WolfVariants.WOODS, "wolf_woods", Biomes.FOREST);
        register(bootstrapcontext, WolfVariants.CHESTNUT, "wolf_chestnut", Biomes.OLD_GROWTH_SPRUCE_TAIGA);
        register(bootstrapcontext, WolfVariants.STRIPED, "wolf_striped", BiomeTags.IS_BADLANDS);
    }
}
