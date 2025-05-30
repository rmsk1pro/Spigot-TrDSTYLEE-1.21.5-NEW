package net.minecraft.gametest.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import org.slf4j.Logger;

public class GameTestHarnessRunner {

    public static final int DEFAULT_TESTS_PER_ROW = 8;
    private static final Logger LOGGER = LogUtils.getLogger();
    final WorldServer level;
    private final GameTestHarnessTicker testTicker;
    private final List<GameTestHarnessInfo> allTestInfos;
    private ImmutableList<GameTestHarnessBatch> batches;
    final List<GameTestBatchListener> batchListeners = Lists.newArrayList();
    private final List<GameTestHarnessInfo> scheduledForRerun = Lists.newArrayList();
    private final GameTestHarnessRunner.b testBatcher;
    private boolean stopped = true;
    @Nullable
    private Holder<TestEnvironmentDefinition> currentEnvironment;
    private final GameTestHarnessRunner.c existingStructureSpawner;
    private final GameTestHarnessRunner.c newStructureSpawner;
    final boolean haltOnError;

    protected GameTestHarnessRunner(GameTestHarnessRunner.b gametestharnessrunner_b, Collection<GameTestHarnessBatch> collection, WorldServer worldserver, GameTestHarnessTicker gametestharnessticker, GameTestHarnessRunner.c gametestharnessrunner_c, GameTestHarnessRunner.c gametestharnessrunner_c1, boolean flag) {
        this.level = worldserver;
        this.testTicker = gametestharnessticker;
        this.testBatcher = gametestharnessrunner_b;
        this.existingStructureSpawner = gametestharnessrunner_c;
        this.newStructureSpawner = gametestharnessrunner_c1;
        this.batches = ImmutableList.copyOf(collection);
        this.haltOnError = flag;
        this.allTestInfos = (List) this.batches.stream().flatMap((gametestharnessbatch) -> {
            return gametestharnessbatch.gameTestInfos().stream();
        }).collect(SystemUtils.toMutableList());
        gametestharnessticker.setRunner(this);
        this.allTestInfos.forEach((gametestharnessinfo) -> {
            gametestharnessinfo.addListener(new ReportGameListener());
        });
    }

    public List<GameTestHarnessInfo> getTestInfos() {
        return this.allTestInfos;
    }

    public void start() {
        this.stopped = false;
        this.runBatch(0);
    }

    public void stop() {
        this.stopped = true;
        if (this.currentEnvironment != null) {
            this.endCurrentEnvironment();
        }

    }

    public void rerunTest(GameTestHarnessInfo gametestharnessinfo) {
        GameTestHarnessInfo gametestharnessinfo1 = gametestharnessinfo.copyReset();

        gametestharnessinfo.getListeners().forEach((gametestharnesslistener) -> {
            gametestharnesslistener.testAddedForRerun(gametestharnessinfo, gametestharnessinfo1, this);
        });
        this.allTestInfos.add(gametestharnessinfo1);
        this.scheduledForRerun.add(gametestharnessinfo1);
        if (this.stopped) {
            this.runScheduledRerunTests();
        }

    }

    void runBatch(final int i) {
        if (i >= this.batches.size()) {
            this.endCurrentEnvironment();
            this.runScheduledRerunTests();
        } else {
            final GameTestHarnessBatch gametestharnessbatch = (GameTestHarnessBatch) this.batches.get(i);

            this.existingStructureSpawner.onBatchStart(this.level);
            this.newStructureSpawner.onBatchStart(this.level);
            Collection<GameTestHarnessInfo> collection = this.createStructuresForBatch(gametestharnessbatch.gameTestInfos());

            GameTestHarnessRunner.LOGGER.info("Running test environment '{}' batch {} ({} tests)...", new Object[]{gametestharnessbatch.environment().getRegisteredName(), gametestharnessbatch.index(), collection.size()});
            if (this.currentEnvironment != gametestharnessbatch.environment()) {
                this.endCurrentEnvironment();
                this.currentEnvironment = gametestharnessbatch.environment();
                ((TestEnvironmentDefinition) this.currentEnvironment.value()).setup(this.level);
            }

            this.batchListeners.forEach((gametestbatchlistener) -> {
                gametestbatchlistener.testBatchStarting(gametestharnessbatch);
            });
            final GameTestHarnessCollector gametestharnesscollector = new GameTestHarnessCollector();

            Objects.requireNonNull(gametestharnesscollector);
            collection.forEach(gametestharnesscollector::addTestToTrack);
            gametestharnesscollector.addListener(new GameTestHarnessListener() {
                private void testCompleted() {
                    if (gametestharnesscollector.isDone()) {
                        GameTestHarnessRunner.this.batchListeners.forEach((gametestbatchlistener) -> {
                            gametestbatchlistener.testBatchFinished(gametestharnessbatch);
                        });
                        LongSet longset = new LongArraySet(GameTestHarnessRunner.this.level.getForceLoadedChunks());

                        longset.forEach((j) -> {
                            GameTestHarnessRunner.this.level.setChunkForced(ChunkCoordIntPair.getX(j), ChunkCoordIntPair.getZ(j), false);
                        });
                        GameTestHarnessRunner.this.runBatch(i + 1);
                    }

                }

                @Override
                public void testStructureLoaded(GameTestHarnessInfo gametestharnessinfo) {}

                @Override
                public void testPassed(GameTestHarnessInfo gametestharnessinfo, GameTestHarnessRunner gametestharnessrunner) {
                    this.testCompleted();
                }

                @Override
                public void testFailed(GameTestHarnessInfo gametestharnessinfo, GameTestHarnessRunner gametestharnessrunner) {
                    if (GameTestHarnessRunner.this.haltOnError) {
                        GameTestHarnessRunner.this.endCurrentEnvironment();
                        LongSet longset = new LongArraySet(GameTestHarnessRunner.this.level.getForceLoadedChunks());

                        longset.forEach((j) -> {
                            GameTestHarnessRunner.this.level.setChunkForced(ChunkCoordIntPair.getX(j), ChunkCoordIntPair.getZ(j), false);
                        });
                        GameTestHarnessTicker.SINGLETON.clear();
                    } else {
                        this.testCompleted();
                    }

                }

                @Override
                public void testAddedForRerun(GameTestHarnessInfo gametestharnessinfo, GameTestHarnessInfo gametestharnessinfo1, GameTestHarnessRunner gametestharnessrunner) {}
            });
            GameTestHarnessTicker gametestharnessticker = this.testTicker;

            Objects.requireNonNull(this.testTicker);
            collection.forEach(gametestharnessticker::add);
        }
    }

    void endCurrentEnvironment() {
        if (this.currentEnvironment != null) {
            ((TestEnvironmentDefinition) this.currentEnvironment.value()).teardown(this.level);
            this.currentEnvironment = null;
        }

    }

    private void runScheduledRerunTests() {
        if (!this.scheduledForRerun.isEmpty()) {
            GameTestHarnessRunner.LOGGER.info("Starting re-run of tests: {}", this.scheduledForRerun.stream().map((gametestharnessinfo) -> {
                return gametestharnessinfo.id().toString();
            }).collect(Collectors.joining(", ")));
            this.batches = ImmutableList.copyOf(this.testBatcher.batch(this.scheduledForRerun));
            this.scheduledForRerun.clear();
            this.stopped = false;
            this.runBatch(0);
        } else {
            this.batches = ImmutableList.of();
            this.stopped = true;
        }

    }

    public void addListener(GameTestBatchListener gametestbatchlistener) {
        this.batchListeners.add(gametestbatchlistener);
    }

    private Collection<GameTestHarnessInfo> createStructuresForBatch(Collection<GameTestHarnessInfo> collection) {
        return collection.stream().map(this::spawn).flatMap(Optional::stream).toList();
    }

    private Optional<GameTestHarnessInfo> spawn(GameTestHarnessInfo gametestharnessinfo) {
        return gametestharnessinfo.getTestBlockPos() == null ? this.newStructureSpawner.spawnStructure(gametestharnessinfo) : this.existingStructureSpawner.spawnStructure(gametestharnessinfo);
    }

    public static void clearMarkers(WorldServer worldserver) {
        PacketDebug.sendGameTestClearPacket(worldserver);
    }

    public interface c {

        GameTestHarnessRunner.c IN_PLACE = (gametestharnessinfo) -> {
            return Optional.of(gametestharnessinfo.prepareTestStructure().startExecution(1));
        };
        GameTestHarnessRunner.c NOT_SET = (gametestharnessinfo) -> {
            return Optional.empty();
        };

        Optional<GameTestHarnessInfo> spawnStructure(GameTestHarnessInfo gametestharnessinfo);

        default void onBatchStart(WorldServer worldserver) {}
    }

    public static class a {

        private final WorldServer level;
        private final GameTestHarnessTicker testTicker;
        private GameTestHarnessRunner.b batcher;
        private GameTestHarnessRunner.c existingStructureSpawner;
        private GameTestHarnessRunner.c newStructureSpawner;
        private final Collection<GameTestHarnessBatch> batches;
        private boolean haltOnError;

        private a(Collection<GameTestHarnessBatch> collection, WorldServer worldserver) {
            this.testTicker = GameTestHarnessTicker.SINGLETON;
            this.batcher = GameTestBatchFactory.fromGameTestInfo();
            this.existingStructureSpawner = GameTestHarnessRunner.c.IN_PLACE;
            this.newStructureSpawner = GameTestHarnessRunner.c.NOT_SET;
            this.haltOnError = false;
            this.batches = collection;
            this.level = worldserver;
        }

        public static GameTestHarnessRunner.a fromBatches(Collection<GameTestHarnessBatch> collection, WorldServer worldserver) {
            return new GameTestHarnessRunner.a(collection, worldserver);
        }

        public static GameTestHarnessRunner.a fromInfo(Collection<GameTestHarnessInfo> collection, WorldServer worldserver) {
            return fromBatches(GameTestBatchFactory.fromGameTestInfo().batch(collection), worldserver);
        }

        public GameTestHarnessRunner.a haltOnError(boolean flag) {
            this.haltOnError = flag;
            return this;
        }

        public GameTestHarnessRunner.a newStructureSpawner(GameTestHarnessRunner.c gametestharnessrunner_c) {
            this.newStructureSpawner = gametestharnessrunner_c;
            return this;
        }

        public GameTestHarnessRunner.a existingStructureSpawner(StructureGridSpawner structuregridspawner) {
            this.existingStructureSpawner = structuregridspawner;
            return this;
        }

        public GameTestHarnessRunner.a batcher(GameTestHarnessRunner.b gametestharnessrunner_b) {
            this.batcher = gametestharnessrunner_b;
            return this;
        }

        public GameTestHarnessRunner build() {
            return new GameTestHarnessRunner(this.batcher, this.batches, this.level, this.testTicker, this.existingStructureSpawner, this.newStructureSpawner, this.haltOnError);
        }
    }

    public interface b {

        Collection<GameTestHarnessBatch> batch(Collection<GameTestHarnessInfo> collection);
    }
}
