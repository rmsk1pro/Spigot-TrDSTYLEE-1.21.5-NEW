package net.minecraft.server.packs.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.util.Unit;

public class Reloadable<S> implements IReloadable {

    private static final int PREPARATION_PROGRESS_WEIGHT = 2;
    private static final int EXTRA_RELOAD_PROGRESS_WEIGHT = 2;
    private static final int LISTENER_PROGRESS_WEIGHT = 1;
    final CompletableFuture<Unit> allPreparations = new CompletableFuture();
    @Nullable
    private CompletableFuture<List<S>> allDone;
    final Set<IReloadListener> preparingListeners;
    private final int listenerCount;
    private final AtomicInteger startedTasks = new AtomicInteger();
    private final AtomicInteger finishedTasks = new AtomicInteger();
    private final AtomicInteger startedReloads = new AtomicInteger();
    private final AtomicInteger finishedReloads = new AtomicInteger();

    public static IReloadable of(IResourceManager iresourcemanager, List<IReloadListener> list, Executor executor, Executor executor1, CompletableFuture<Unit> completablefuture) {
        Reloadable<Void> reloadable = new Reloadable<Void>(list);

        reloadable.startTasks(executor, executor1, iresourcemanager, list, Reloadable.a.SIMPLE, completablefuture);
        return reloadable;
    }

    protected Reloadable(List<IReloadListener> list) {
        this.listenerCount = list.size();
        this.preparingListeners = new HashSet(list);
    }

    protected void startTasks(Executor executor, Executor executor1, IResourceManager iresourcemanager, List<IReloadListener> list, Reloadable.a<S> reloadable_a, CompletableFuture<?> completablefuture) {
        this.allDone = this.prepareTasks(executor, executor1, iresourcemanager, list, reloadable_a, completablefuture);
    }

    protected CompletableFuture<List<S>> prepareTasks(Executor executor, Executor executor1, IResourceManager iresourcemanager, List<IReloadListener> list, Reloadable.a<S> reloadable_a, CompletableFuture<?> completablefuture) {
        Executor executor2 = (runnable) -> {
            this.startedTasks.incrementAndGet();
            executor.execute(() -> {
                runnable.run();
                this.finishedTasks.incrementAndGet();
            });
        };
        Executor executor3 = (runnable) -> {
            this.startedReloads.incrementAndGet();
            executor1.execute(() -> {
                runnable.run();
                this.finishedReloads.incrementAndGet();
            });
        };

        this.startedTasks.incrementAndGet();
        AtomicInteger atomicinteger = this.finishedTasks;

        Objects.requireNonNull(this.finishedTasks);
        completablefuture.thenRun(atomicinteger::incrementAndGet);
        CompletableFuture<?> completablefuture1 = completablefuture;
        List<CompletableFuture<S>> list1 = new ArrayList();

        for (IReloadListener ireloadlistener : list) {
            IReloadListener.a ireloadlistener_a = this.createBarrierForListener(ireloadlistener, completablefuture1, executor1);
            CompletableFuture<S> completablefuture2 = reloadable_a.create(ireloadlistener_a, iresourcemanager, ireloadlistener, executor2, executor3);

            list1.add(completablefuture2);
            completablefuture1 = completablefuture2;
        }

        return SystemUtils.sequenceFailFast(list1);
    }

    private IReloadListener.a createBarrierForListener(final IReloadListener ireloadlistener, final CompletableFuture<?> completablefuture, final Executor executor) {
        return new IReloadListener.a() {
            @Override
            public <T> CompletableFuture<T> wait(T t0) {
                executor.execute(() -> {
                    Reloadable.this.preparingListeners.remove(ireloadlistener);
                    if (Reloadable.this.preparingListeners.isEmpty()) {
                        Reloadable.this.allPreparations.complete(Unit.INSTANCE);
                    }

                });
                return Reloadable.this.allPreparations.thenCombine(completablefuture, (unit, object) -> {
                    return t0;
                });
            }
        };
    }

    @Override
    public CompletableFuture<?> done() {
        return (CompletableFuture) Objects.requireNonNull(this.allDone, "not started");
    }

    @Override
    public float getActualProgress() {
        int i = this.listenerCount - this.preparingListeners.size();
        float f = (float) weightProgress(this.finishedTasks.get(), this.finishedReloads.get(), i);
        float f1 = (float) weightProgress(this.startedTasks.get(), this.startedReloads.get(), this.listenerCount);

        return f / f1;
    }

    private static int weightProgress(int i, int j, int k) {
        return i * 2 + j * 2 + k * 1;
    }

    public static IReloadable create(IResourceManager iresourcemanager, List<IReloadListener> list, Executor executor, Executor executor1, CompletableFuture<Unit> completablefuture, boolean flag) {
        return flag ? ReloadableProfiled.of(iresourcemanager, list, executor, executor1, completablefuture) : of(iresourcemanager, list, executor, executor1, completablefuture);
    }

    @FunctionalInterface
    protected interface a<S> {

        Reloadable.a<Void> SIMPLE = (ireloadlistener_a, iresourcemanager, ireloadlistener, executor, executor1) -> {
            return ireloadlistener.reload(ireloadlistener_a, iresourcemanager, executor, executor1);
        };

        CompletableFuture<S> create(IReloadListener.a ireloadlistener_a, IResourceManager iresourcemanager, IReloadListener ireloadlistener, Executor executor, Executor executor1);
    }
}
