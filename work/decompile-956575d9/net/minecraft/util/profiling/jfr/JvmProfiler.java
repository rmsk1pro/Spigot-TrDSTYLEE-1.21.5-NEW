package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.net.SocketAddress;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.network.EnumProtocol;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.chunk.storage.RegionFileCompression;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;

public interface JvmProfiler {

    JvmProfiler INSTANCE = (JvmProfiler) (Runtime.class.getModule().getLayer().findModule("jdk.jfr").isPresent() ? JfrProfiler.getInstance() : new JvmProfiler.a());

    boolean start(Environment environment);

    Path stop();

    boolean isRunning();

    boolean isAvailable();

    void onServerTick(float f);

    void onPacketReceived(EnumProtocol enumprotocol, PacketType<?> packettype, SocketAddress socketaddress, int i);

    void onPacketSent(EnumProtocol enumprotocol, PacketType<?> packettype, SocketAddress socketaddress, int i);

    void onRegionFileRead(RegionStorageInfo regionstorageinfo, ChunkCoordIntPair chunkcoordintpair, RegionFileCompression regionfilecompression, int i);

    void onRegionFileWrite(RegionStorageInfo regionstorageinfo, ChunkCoordIntPair chunkcoordintpair, RegionFileCompression regionfilecompression, int i);

    @Nullable
    ProfiledDuration onWorldLoadedStarted();

    @Nullable
    ProfiledDuration onChunkGenerate(ChunkCoordIntPair chunkcoordintpair, ResourceKey<World> resourcekey, String s);

    @Nullable
    ProfiledDuration onStructureGenerate(ChunkCoordIntPair chunkcoordintpair, ResourceKey<World> resourcekey, Holder<Structure> holder);

    public static class a implements JvmProfiler {

        private static final Logger LOGGER = LogUtils.getLogger();
        static final ProfiledDuration noOpCommit = (flag) -> {
        };

        public a() {}

        @Override
        public boolean start(Environment environment) {
            JvmProfiler.a.LOGGER.warn("Attempted to start Flight Recorder, but it's not supported on this JVM");
            return false;
        }

        @Override
        public Path stop() {
            throw new IllegalStateException("Attempted to stop Flight Recorder, but it's not supported on this JVM");
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void onPacketReceived(EnumProtocol enumprotocol, PacketType<?> packettype, SocketAddress socketaddress, int i) {}

        @Override
        public void onPacketSent(EnumProtocol enumprotocol, PacketType<?> packettype, SocketAddress socketaddress, int i) {}

        @Override
        public void onRegionFileRead(RegionStorageInfo regionstorageinfo, ChunkCoordIntPair chunkcoordintpair, RegionFileCompression regionfilecompression, int i) {}

        @Override
        public void onRegionFileWrite(RegionStorageInfo regionstorageinfo, ChunkCoordIntPair chunkcoordintpair, RegionFileCompression regionfilecompression, int i) {}

        @Override
        public void onServerTick(float f) {}

        @Override
        public ProfiledDuration onWorldLoadedStarted() {
            return JvmProfiler.a.noOpCommit;
        }

        @Nullable
        @Override
        public ProfiledDuration onChunkGenerate(ChunkCoordIntPair chunkcoordintpair, ResourceKey<World> resourcekey, String s) {
            return null;
        }

        @Override
        public ProfiledDuration onStructureGenerate(ChunkCoordIntPair chunkcoordintpair, ResourceKey<World> resourcekey, Holder<Structure> holder) {
            return JvmProfiler.a.noOpCommit;
        }
    }
}
