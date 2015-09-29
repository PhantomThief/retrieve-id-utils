/**
 * 
 */
package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.phantomthief.stats.n.DurationStats;
import com.github.phantomthief.stats.n.counter.Duration;
import com.github.phantomthief.stats.n.impl.SimpleDurationStats;
import com.google.common.base.Stopwatch;

/**
 * 
 * @author w.vela
 */
public class LoadingMerger<K, V> implements IMultiDataAccess<K, V> {

    private static org.slf4j.Logger logger = getLogger(LoadingMerger.class);

    private final ConcurrentMap<K, LoadingHolder<K, V>> currentLoading = new ConcurrentHashMap<>();
    private final long waitOtherLoadingTimeout;
    private final Function<Collection<K>, Map<K, V>> loader;
    private final DurationStats<LoadingMergeStats> stats;
    private final String name;

    /**
     * @param waitOtherLoadingTimeout
     * @param loader
     */
    private LoadingMerger(long waitOtherLoadingTimeout, Function<Collection<K>, Map<K, V>> loader,
            boolean enableStats, String name) {
        this.waitOtherLoadingTimeout = waitOtherLoadingTimeout;
        if (enableStats) {
            this.stats = SimpleDurationStats.newBuilder().build(LoadingMergeStats::new);
            this.loader = wrapStats(stats, loader);
        } else {
            this.stats = null;
            this.loader = loader;
        }
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    private Function<Collection<K>, Map<K, V>> wrapStats(DurationStats<LoadingMergeStats> stats,
            Function<Collection<K>, Map<K, V>> inner) {
        return keys -> {
            Stopwatch stopwatch = Stopwatch.createStarted();
            try {
                return inner.apply(keys);
            } finally {
                stats.stat(LoadingMergeStats.load(stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)));
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
        if (logger.isDebugEnabled()) {
            logger.debug("found other to load:{}", otherLoading.keySet());
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
                    V v = holder.get(remained, MILLISECONDS);
                    if (v != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("found other already get for key:{}->{}", key, v);
                        }
                        finalResult.put(key, v);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (TimeoutException e) {
                    if (stats != null) {
                        stats.stat(LoadingMergeStats.timeout());
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("found timeout retrieve id:{}", key);
                    }
                    remainedKeys.add(key);
                } catch (Throwable e) {
                    remainedKeys.add(key);
                    logger.error("Ops.", e);
                }
                s = System.currentTimeMillis() - s;
                remained -= s;
            }
            if (stats != null) {
                stats.stat(LoadingMergeStats.merge(otherLoading.size(),
                        stopwatch.stop().elapsed(MILLISECONDS)));
            }
            if (!remainedKeys.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("found timeout retrieve ids:{}, ready to get sync.", remainedKeys);
                }
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
            if (stats != null) {
                stats.stat(LoadingMergeStats.merge(otherLoading.size(),
                        stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)));
            }
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

        public V get() throws InterruptedException {
            latch.await();
            return result.get(key);
        }

        public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            if (latch.await(timeout, unit)) {
                return result.get(key);
            } else {
                throw new TimeoutException();
            }
        }
    }

    public static final class Builder<K, V> {

        private long waitOtherLoadingTimeout;
        private Function<Collection<K>, Map<K, V>> loader;
        private String name;
        private boolean enableStats;

        public Builder<K, V> enableStats() {
            this.enableStats = true;
            return this;
        }

        public Builder<K, V> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<K, V> timeout(long timeout, TimeUnit unit) {
            this.waitOtherLoadingTimeout = unit.toMillis(timeout);
            return this;
        }

        public Builder<K, V> loader(Function<Collection<K>, Map<K, V>> func) {
            this.loader = func;
            return this;
        }

        public LoadingMerger<K, V> build() {
            checkNotNull(loader);
            return new LoadingMerger<>(waitOtherLoadingTimeout, loader, enableStats, name);
        }
    }

    public static final <K, V> Builder<K, V> newBuilder() {
        return new Builder<>();
    }

    public static final class LoadingMergeStats implements Duration {

        private final AtomicLong loadCount = new AtomicLong();
        private final AtomicLong mergedKeys = new AtomicLong();
        private final AtomicLong loadCost = new AtomicLong();
        private final AtomicLong mergeWait = new AtomicLong();
        private final AtomicLong timeoutCount = new AtomicLong();

        private long duration;

        /**
         * @param duration
         */
        public LoadingMergeStats(long duration) {
            this.duration = duration;
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

        /* (non-Javadoc)
         * @see com.github.phantomthief.stats.n.counter.Duration#duration()
         */
        @Override
        public long duration() {
            return duration;
        }

        /* (non-Javadoc)
         * @see com.github.phantomthief.stats.n.counter.Duration#setDuration(long)
         */
        @Override
        public void setDuration(long duration) {
            this.duration = duration;
        }
    }

    public Map<Long, LoadingMergeStats> getStats() {
        return stats.getStats();
    }
}
