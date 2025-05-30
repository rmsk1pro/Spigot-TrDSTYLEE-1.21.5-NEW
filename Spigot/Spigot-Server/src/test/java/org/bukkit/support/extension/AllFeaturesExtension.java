package org.bukkit.support.extension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.flag.FeatureFlags;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.plugin.PluginManager;
import org.bukkit.support.DummyServerHelper;
import org.bukkit.support.RegistryHelper;
import org.junit.jupiter.api.extension.ExtensionContext;

public class AllFeaturesExtension extends BaseExtension {

    private static final Map<Class<? extends Keyed>, Registry<?>> realRegistries = new HashMap<>();
    private static final Map<Class<? extends Keyed>, Registry<?>> spyRegistries = new HashMap<>();

    public AllFeaturesExtension() {
        super("AllFeatures");
    }

    public static <T extends Keyed> Registry<T> getRealRegistry(Class<T> clazz) {
        return (Registry<T>) realRegistries.get(clazz);
    }

    public static Map<Class<? extends Keyed>, Registry<?>> getRealRegistries() {
        return realRegistries;
    }

    @Override
    public void init(ExtensionContext extensionContext) {
        RegistryHelper.setup(FeatureFlags.REGISTRY.allFlags());

        Server server = DummyServerHelper.setup();

        Bukkit.setServer(server);

        when(server.getRegistry(any()))
                .then(invocation -> {
                    Class<? extends Keyed> keyed = invocation.getArgument(0);
                    if (spyRegistries.containsKey(keyed)) {
                        return spyRegistries.get(keyed);
                    }

                    Registry<?> registry = CraftRegistry.createRegistry(keyed, RegistryHelper.getRegistry());
                    realRegistries.put(keyed, registry);

                    Registry<?> spy = mock(registry.getClass(), withSettings().stubOnly().spiedInstance(registry).defaultAnswer(CALLS_REAL_METHODS));

                    spyRegistries.put(keyed, spy);

                    return spy;
                });

        PluginManager pluginManager = mock(withSettings().stubOnly());
        when(server.getPluginManager()).thenReturn(pluginManager);

        CraftRegistry.setMinecraftRegistry(RegistryHelper.getRegistry());
    }

    @Override
    void runBeforeEach(ExtensionContext extensionContext) {
    }
}
