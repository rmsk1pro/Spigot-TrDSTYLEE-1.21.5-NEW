package net.minecraft.server.packs;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.SystemUtils;
import net.minecraft.util.FileSystemUtil;
import org.slf4j.Logger;

public class VanillaPackResourcesBuilder {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static Consumer<VanillaPackResourcesBuilder> developmentConfig = (vanillapackresourcesbuilder) -> {
    };
    private static final Map<EnumResourcePackType, Path> ROOT_DIR_BY_TYPE = (Map) SystemUtils.make(() -> {
        synchronized (ResourcePackVanilla.class) {
            ImmutableMap.Builder<EnumResourcePackType, Path> immutablemap_builder = ImmutableMap.builder();

            for (EnumResourcePackType enumresourcepacktype : EnumResourcePackType.values()) {
                String s = "/" + enumresourcepacktype.getDirectory() + "/.mcassetsroot";
                URL url = ResourcePackVanilla.class.getResource(s);

                if (url == null) {
                    VanillaPackResourcesBuilder.LOGGER.error("File {} does not exist in classpath", s);
                } else {
                    try {
                        URI uri = url.toURI();
                        String s1 = uri.getScheme();

                        if (!"jar".equals(s1) && !"file".equals(s1)) {
                            VanillaPackResourcesBuilder.LOGGER.warn("Assets URL '{}' uses unexpected schema", uri);
                        }

                        Path path = FileSystemUtil.safeGetPath(uri);

                        immutablemap_builder.put(enumresourcepacktype, path.getParent());
                    } catch (Exception exception) {
                        VanillaPackResourcesBuilder.LOGGER.error("Couldn't resolve path to vanilla assets", exception);
                    }
                }
            }

            return immutablemap_builder.build();
        }
    });
    private final Set<Path> rootPaths = new LinkedHashSet();
    private final Map<EnumResourcePackType, Set<Path>> pathsForType = new EnumMap(EnumResourcePackType.class);
    private BuiltInMetadata metadata = BuiltInMetadata.of();
    private final Set<String> namespaces = new HashSet();

    public VanillaPackResourcesBuilder() {}

    private boolean validateDirPath(Path path) {
        if (!Files.exists(path, new LinkOption[0])) {
            return false;
        } else if (!Files.isDirectory(path, new LinkOption[0])) {
            throw new IllegalArgumentException("Path " + String.valueOf(path.toAbsolutePath()) + " is not directory");
        } else {
            return true;
        }
    }

    private void pushRootPath(Path path) {
        if (this.validateDirPath(path)) {
            this.rootPaths.add(path);
        }

    }

    private void pushPathForType(EnumResourcePackType enumresourcepacktype, Path path) {
        if (this.validateDirPath(path)) {
            ((Set) this.pathsForType.computeIfAbsent(enumresourcepacktype, (enumresourcepacktype1) -> {
                return new LinkedHashSet();
            })).add(path);
        }

    }

    public VanillaPackResourcesBuilder pushJarResources() {
        VanillaPackResourcesBuilder.ROOT_DIR_BY_TYPE.forEach((enumresourcepacktype, path) -> {
            this.pushRootPath(path.getParent());
            this.pushPathForType(enumresourcepacktype, path);
        });
        return this;
    }

    public VanillaPackResourcesBuilder pushClasspathResources(EnumResourcePackType enumresourcepacktype, Class<?> oclass) {
        Enumeration<URL> enumeration = null;

        try {
            enumeration = oclass.getClassLoader().getResources(enumresourcepacktype.getDirectory() + "/");
        } catch (IOException ioexception) {
            ;
        }

        while (enumeration != null && enumeration.hasMoreElements()) {
            URL url = (URL) enumeration.nextElement();

            try {
                URI uri = url.toURI();

                if ("file".equals(uri.getScheme())) {
                    Path path = Paths.get(uri);

                    this.pushRootPath(path.getParent());
                    this.pushPathForType(enumresourcepacktype, path);
                }
            } catch (Exception exception) {
                VanillaPackResourcesBuilder.LOGGER.error("Failed to extract path from {}", url, exception);
            }
        }

        return this;
    }

    public VanillaPackResourcesBuilder applyDevelopmentConfig() {
        VanillaPackResourcesBuilder.developmentConfig.accept(this);
        return this;
    }

    public VanillaPackResourcesBuilder pushUniversalPath(Path path) {
        this.pushRootPath(path);

        for (EnumResourcePackType enumresourcepacktype : EnumResourcePackType.values()) {
            this.pushPathForType(enumresourcepacktype, path.resolve(enumresourcepacktype.getDirectory()));
        }

        return this;
    }

    public VanillaPackResourcesBuilder pushAssetPath(EnumResourcePackType enumresourcepacktype, Path path) {
        this.pushRootPath(path);
        this.pushPathForType(enumresourcepacktype, path);
        return this;
    }

    public VanillaPackResourcesBuilder setMetadata(BuiltInMetadata builtinmetadata) {
        this.metadata = builtinmetadata;
        return this;
    }

    public VanillaPackResourcesBuilder exposeNamespace(String... astring) {
        this.namespaces.addAll(Arrays.asList(astring));
        return this;
    }

    public ResourcePackVanilla build(PackLocationInfo packlocationinfo) {
        return new ResourcePackVanilla(packlocationinfo, this.metadata, Set.copyOf(this.namespaces), copyAndReverse(this.rootPaths), SystemUtils.makeEnumMap(EnumResourcePackType.class, (enumresourcepacktype) -> {
            return copyAndReverse((Collection) this.pathsForType.getOrDefault(enumresourcepacktype, Set.of()));
        }));
    }

    private static List<Path> copyAndReverse(Collection<Path> collection) {
        List<Path> list = new ArrayList(collection);

        Collections.reverse(list);
        return List.copyOf(list);
    }
}
