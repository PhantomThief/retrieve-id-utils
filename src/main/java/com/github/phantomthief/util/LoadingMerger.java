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
import java.util.function.Function;

import com.google.common.base.Preconditions;

/**
 * 
 * @author w.vela
 */
public class LoadingMerger<K, V> implements IMultiDataAccess<K, V> {

    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LoadingMerger.class);

    private final ConcurrentMap<K, LoadingHolder<K, V>> currentLoading = new ConcurrentHashMap<>();
    private final long waitOtherLoadingTimeout;
    private final Function<Collection<K>, Map<K, V>> loader;

    /**
     * @param waitOtherLoadingTimeout
     * @param loader
     */
    private LoadingMerger(long waitOtherLoadingTimeout, Function<Collection<K>, Map<K, V>> loader) {
        this.waitOtherLoadingTimeout = waitOtherLoadingTimeout;
        this.loader = loader;
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
            if (!remainedKeys.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("found timeout retrieve ids:{}", remainedKeys);
                }
                finalResult.putAll(loader.apply(remainedKeys));
            }
        } else {
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
}
