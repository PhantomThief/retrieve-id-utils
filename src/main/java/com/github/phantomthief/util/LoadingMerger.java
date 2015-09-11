/**
 * 
 */
package com.github.phantomthief.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.github.phantomthief.stats.SingleStatsHelper;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

/**
 * 
 * @author w.vela
 */
public class LoadingMerger<K, V> implements IMultiDataAccess<K, V> {

    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LoadingMerger.class);

    private final ConcurrentMap<K, LoadingHolder<K, V>> currentLoading = new ConcurrentHashMap<>();
    private final long waitOtherLoadingTimeout;
    private final Function<Collection<K>, Map<K, V>> loader;
    private final SingleStatsHelper<LoadingMergeStats> stats;

    /**
     * @param waitOtherLoadingTimeout
     * @param loader
     */
    private LoadingMerger(long waitOtherLoadingTimeout, Function<Collection<K>, Map<K, V>> loader) {
        this.waitOtherLoadingTimeout = waitOtherLoadingTimeout;
        this.stats = SingleStatsHelper.<LoadingMergeStats> newBuilder() //
                .setCounterReset(LoadingMergeStats.resetter()) //
                .build();
        this.loader = wrapStats(stats, loader);
    }

    private Function<Collection<K>, Map<K, V>> wrapStats(SingleStatsHelper<LoadingMergeStats> stats,
            Function<Collection<K>, Map<K, V>> inner) {
        return keys -> {
            Stopwatch stopwatch = Stopwatch.createStarted();
            try {
                return inner.apply(keys);
            } finally {
                stats.stats(
                        LoadingMergeStats.load(stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)));
            }
        };
    }

    @Override
    public Map<K, V> get(Collection<K> keys) {
        CountDownLatch latch = new CountDownLatch(1);
        Map<K, V> result = new HashMap<>();
        Set<K> needLoadKeys = new HashSet<>();
        Map<K, LoadingHolder<K, V>> otherLoading = new HashMap<>();
        keys.forEach(key -> {
            LoadingHolder<K, V> holder = currentLoading.computeIfAbsent(key,
                    k -> new LoadingHolder<>(latch, k, result));
            if (holder.isCurrent()) {
                needLoadKeys.add(key);
            } else {
                otherLoading.put(key, holder);
            }
        });
        try {
            if (!needLoadKeys.isEmpty()) {
                result.putAll(loader.apply(needLoadKeys));
            }
        } finally {
            needLoadKeys.forEach(currentLoading::remove);
            latch.countDown();
        }
        Map<K, V> finalResult = new HashMap<>(result);
        if (waitOtherLoadingTimeout > 0) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            long remained = waitOtherLoadingTimeout;
            Set<K> remainedKeys = new HashSet<>();
            for (Entry<K, LoadingHolder<K, V>> entry : otherLoading.entrySet()) {
                K key = entry.getKey();
                LoadingHolder<K, V> holder = entry.getValue();
                long s = System.currentTimeMillis();
                try {
                    V v = holder.get(remained, TimeUnit.MILLISECONDS);
                    if (v != null) {
                        finalResult.put(key, v);
                    }
                } catch (Exception e) {
                    logger.error("Ops.", e);
                }
                s = System.currentTimeMillis() - s;
                remained -= s;
                if (remained <= 0) {
                    remainedKeys.add(key);
                }
            }
            stats.stats(LoadingMergeStats.merge(otherLoading.size(),
                    stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)));
            if (!remainedKeys.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("found timeout retrieve ids:{}", remainedKeys);
                }
                stats.stats(LoadingMergeStats.timeout());
                finalResult.putAll(loader.apply(remainedKeys));
            }
        } else {
            Stopwatch stopwatch = Stopwatch.createStarted();
            otherLoading.forEach((key, holder) -> {
                try {
                    V v = holder.get();
                    if (v != null) {
                        finalResult.put(key, v);
                    }
                } catch (Exception e) {
                    logger.error("Ops.", e);
                }
            });
            stats.stats(LoadingMergeStats.merge(otherLoading.size(),
                    stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)));
        }
        return finalResult;
    }

    private static final class LoadingHolder<K, V> implements Future<V> {

        private final Thread thread = Thread.currentThread();
        private final CountDownLatch latch;
        private final K key;
        private final Map<K, V> result;

        /**
         * @param task
         */
        public LoadingHolder(CountDownLatch latch, K key, Map<K, V> result) {
            this.latch = latch;
            this.key = key;
            this.result = result;
        }

        boolean isCurrent() {
            return Thread.currentThread() == thread;
        }

        public boolean isCancelled() {
            throw new UnsupportedOperationException();
        }

        public boolean isDone() {
            return latch.getCount() == 0;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException();
        }

        public V get() throws InterruptedException, ExecutionException {
            latch.await();
            return result.get(key);
        }

        public V get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            latch.await(timeout, unit);
            return result.get(key);
        }
    }

    public static final class Builder<K, V> {

        private long waitOtherLoadingTimeout;
        private Function<Collection<K>, Map<K, V>> loader;

        public Builder<K, V> timeout(long timeout, TimeUnit unit) {
            this.waitOtherLoadingTimeout = unit.toMillis(timeout);
            return this;
        }

        public Builder<K, V> loader(Function<Collection<K>, Map<K, V>> func) {
            this.loader = func;
            return this;
        }

        public LoadingMerger<K, V> build() {
            Preconditions.checkNotNull(loader);
            return new LoadingMerger<>(waitOtherLoadingTimeout, loader);
        }
    }

    public static final <K, V> Builder<K, V> newBuilder() {
        return new Builder<>();
    }

    public static final class LoadingMergeStats {

        private final AtomicLong loadCount = new AtomicLong();
        private final AtomicLong mergedKeys = new AtomicLong();
        private final AtomicLong loadCost = new AtomicLong();
        private final AtomicLong mergeWait = new AtomicLong();
        private final AtomicLong timeoutCount = new AtomicLong();
        private final long resetTime = System.currentTimeMillis();

        public static UnaryOperator<LoadingMergeStats> resetter() {
            return old -> new LoadingMergeStats();
        }

        private void doLoad(long cost) {
            loadCount.incrementAndGet();
            loadCost.addAndGet(cost);
        }

        private void doMerge(int mergedKeys, long wait) {
            this.mergedKeys.addAndGet(mergedKeys);
            mergeWait.addAndGet(wait);
        }

        private void doTimeout() {
            timeoutCount.incrementAndGet();
        }

        public long getResetTime() {
            return resetTime;
        }

        public long getLoadCount() {
            return loadCount.get();
        }

        public long getLoadCost() {
            return loadCost.get();
        }

        public long getMergedKeys() {
            return mergedKeys.get();
        }

        public long getMergeWait() {
            return mergeWait.get();
        }

        public long getTimeoutCount() {
            return timeoutCount.get();
        }

        public static Consumer<LoadingMergeStats> load(long cost) {
            return counter -> counter.doLoad(cost);
        }

        public static Consumer<LoadingMergeStats> merge(int mergedKeys, long wait) {
            return counter -> counter.doMerge(mergedKeys, wait);
        }

        public static Consumer<LoadingMergeStats> timeout() {
            return counter -> counter.doTimeout();
        }
    }

    public SingleStatsHelper<LoadingMergeStats> getStats() {
        return stats;
    }
}
