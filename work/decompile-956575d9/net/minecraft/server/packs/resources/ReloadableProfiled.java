package net.minecraft.server.packs.resources;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.SystemUtils;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.util.profiling.Profiler;
import org.slf4j.Logger;

public class ReloadableProfiled extends Reloadable<ReloadableProfiled.a> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final Stopwatch total = Stopwatch.createUnstarted();

    public static IReloadable of(IResourceManager iresourcemanager, List<IReloadListener> list, Executor executor, Executor executor1, CompletableFuture<Unit> completablefuture) {
        ReloadableProfiled reloadableprofiled = new ReloadableProfiled(list);

        reloadableprofiled.startTasks(executor, executor1, iresourcemanager, list, (ireloadlistener_a, iresourcemanager1, ireloadlistener, executor2, executor3) -> {
            AtomicLong atomiclong = new AtomicLong();
            AtomicLong atomiclong1 = new AtomicLong();
            AtomicLong atomiclong2 = new AtomicLong();
            AtomicLong atomiclong3 = new AtomicLong();
            CompletableFuture<Void> completablefuture1 = ireloadlistener.reload(ireloadlistener_a, iresourcemanager1, profiledExecutor(executor2, atomiclong, atomiclong1, ireloadlistener.getName()), profiledExecutor(executor3, atomiclong2, atomiclong3, ireloadlistener.getName()));

            return completablefuture1.thenApplyAsync((ovoid) -> {
                ReloadableProfiled.LOGGER.debug("Finished reloading {}", ireloadlistener.getName());
                return new ReloadableProfiled.a(ireloadlistener.getName(), atomiclong, atomiclong1, atomiclong2, atomiclong3);
            }, executor1);
        }, completablefuture);
        return reloadableprofiled;
    }

    private ReloadableProfiled(List<IReloadListener> list) {
        super(list);
        this.total.start();
    }

    @Override
    protected CompletableFuture<List<ReloadableProfiled.a>> prepareTasks(Executor executor, Executor executor1, IResourceManager iresourcemanager, List<IReloadListener> list, Reloadable.a<ReloadableProfiled.a> reloadable_a, CompletableFuture<?> completablefuture) {
        return super.prepareTasks(executor, executor1, iresourcemanager, list, reloadable_a, completablefuture).thenApplyAsync(this::finish, executor1);
    }

    private static Executor profiledExecutor(Executor executor, AtomicLong atomiclong, AtomicLong atomiclong1, String s) {
        return (runnable) -> {
            executor.execute(() -> {
                GameProfilerFiller gameprofilerfiller = Profiler.get();

                gameprofilerfiller.push(s);
                long i = SystemUtils.getNanos();

                runnable.run();
                atomiclong.addAndGet(SystemUtils.getNanos() - i);
                atomiclong1.incrementAndGet();
                gameprofilerfiller.pop();
            });
        };
    }

    private List<ReloadableProfiled.a> finish(List<ReloadableProfiled.a> list) {
        this.total.stop();
        long i = 0L;

        ReloadableProfiled.LOGGER.info("Resource reload finished after {} ms", this.total.elapsed(TimeUnit.MILLISECONDS));

        for (ReloadableProfiled.a reloadableprofiled_a : list) {
            long j = TimeUnit.NANOSECONDS.toMillis(reloadableprofiled_a.preparationNanos.get());
            long k = reloadableprofiled_a.preparationCount.get();
            long l = TimeUnit.NANOSECONDS.toMillis(reloadableprofiled_a.reloadNanos.get());
            long i1 = reloadableprofiled_a.reloadCount.get();
            long j1 = j + l;
            long k1 = k + i1;
            String s = reloadableprofiled_a.name;

            ReloadableProfiled.LOGGER.info("{} took approximately {} tasks/{} ms ({} tasks/{} ms preparing, {} tasks/{} ms applying)", new Object[]{s, k1, j1, k, j, i1, l});
            i += l;
        }

        ReloadableProfiled.LOGGER.info("Total blocking time: {} ms", i);
        return list;
    }

    public static record a(String name, AtomicLong preparationNanos, AtomicLong preparationCount, AtomicLong reloadNanos, AtomicLong reloadCount) {

    }
}
