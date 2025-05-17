package net.minecraft.server.packs.linkfs;

import com.google.common.base.Splitter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class LinkFileSystem extends FileSystem {

    private static final Set<String> VIEWS = Set.of("basic");
    public static final String PATH_SEPARATOR = "/";
    private static final Splitter PATH_SPLITTER = Splitter.on('/');
    private final FileStore store;
    private final FileSystemProvider provider = new LinkFSProvider();
    private final LinkFSPath root;

    LinkFileSystem(String s, LinkFileSystem.b linkfilesystem_b) {
        this.store = new LinkFSFileStore(s);
        this.root = buildPath(linkfilesystem_b, this, "", (LinkFSPath) null);
    }

    private static LinkFSPath buildPath(LinkFileSystem.b linkfilesystem_b, LinkFileSystem linkfilesystem, String s, @Nullable LinkFSPath linkfspath) {
        Object2ObjectOpenHashMap<String, LinkFSPath> object2objectopenhashmap = new Object2ObjectOpenHashMap();
        LinkFSPath linkfspath1 = new LinkFSPath(linkfilesystem, s, linkfspath, new PathContents.a(object2objectopenhashmap));

        linkfilesystem_b.files.forEach((s1, path) -> {
            object2objectopenhashmap.put(s1, new LinkFSPath(linkfilesystem, s1, linkfspath1, new PathContents.b(path)));
        });
        linkfilesystem_b.children.forEach((s1, linkfilesystem_b1) -> {
            object2objectopenhashmap.put(s1, buildPath(linkfilesystem_b1, linkfilesystem, s1, linkfspath1));
        });
        object2objectopenhashmap.trim();
        return linkfspath1;
    }

    public FileSystemProvider provider() {
        return this.provider;
    }

    public void close() {}

    public boolean isOpen() {
        return true;
    }

    public boolean isReadOnly() {
        return true;
    }

    public String getSeparator() {
        return "/";
    }

    public Iterable<Path> getRootDirectories() {
        return List.of(this.root);
    }

    public Iterable<FileStore> getFileStores() {
        return List.of(this.store);
    }

    public Set<String> supportedFileAttributeViews() {
        return LinkFileSystem.VIEWS;
    }

    public Path getPath(String s, String... astring) {
        Stream<String> stream = Stream.of(s);

        if (astring.length > 0) {
            stream = Stream.concat(stream, Stream.of(astring));
        }

        String s1 = (String) stream.collect(Collectors.joining("/"));

        if (s1.equals("/")) {
            return this.root;
        } else if (s1.startsWith("/")) {
            LinkFSPath linkfspath = this.root;

            for (String s2 : LinkFileSystem.PATH_SPLITTER.split(s1.substring(1))) {
                if (s2.isEmpty()) {
                    throw new IllegalArgumentException("Empty paths not allowed");
                }

                linkfspath = linkfspath.resolveName(s2);
            }

            return linkfspath;
        } else {
            LinkFSPath linkfspath1 = null;

            for (String s3 : LinkFileSystem.PATH_SPLITTER.split(s1)) {
                if (s3.isEmpty()) {
                    throw new IllegalArgumentException("Empty paths not allowed");
                }

                linkfspath1 = new LinkFSPath(this, s3, linkfspath1, PathContents.RELATIVE);
            }

            if (linkfspath1 == null) {
                throw new IllegalArgumentException("Empty paths not allowed");
            } else {
                return linkfspath1;
            }
        }
    }

    public PathMatcher getPathMatcher(String s) {
        throw new UnsupportedOperationException();
    }

    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    public WatchService newWatchService() {
        throw new UnsupportedOperationException();
    }

    public FileStore store() {
        return this.store;
    }

    public LinkFSPath rootPath() {
        return this.root;
    }

    public static LinkFileSystem.a builder() {
        return new LinkFileSystem.a();
    }

    private static record b(Map<String, LinkFileSystem.b> children, Map<String, Path> files) {

        public b() {
            this(new HashMap(), new HashMap());
        }
    }

    public static class a {

        private final LinkFileSystem.b root = new LinkFileSystem.b();

        public a() {}

        public LinkFileSystem.a put(List<String> list, String s, Path path) {
            LinkFileSystem.b linkfilesystem_b = this.root;

            for (String s1 : list) {
                linkfilesystem_b = (LinkFileSystem.b) linkfilesystem_b.children.computeIfAbsent(s1, (s2) -> {
                    return new LinkFileSystem.b();
                });
            }

            linkfilesystem_b.files.put(s, path);
            return this;
        }

        public LinkFileSystem.a put(List<String> list, Path path) {
            if (list.isEmpty()) {
                throw new IllegalArgumentException("Path can't be empty");
            } else {
                int i = list.size() - 1;

                return this.put(list.subList(0, i), (String) list.get(i), path);
            }
        }

        public FileSystem build(String s) {
            return new LinkFileSystem(s, this.root);
        }
    }
}
