package com.github.phantomthief.util;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

/**
 * @author w.vela
 */
public final class RetrieveIdUtils {

    private static final Logger logger = LoggerFactory.getLogger(RetrieveIdUtils.class);
    @SuppressWarnings("UnstableApiUsage")
    private static final RateLimiter rateLimiter = RateLimiter.create(1);

    private static void rateLog(Runnable doLog) {
        //noinspection UnstableApiUsage
        if (rateLimiter.tryAcquire()) {
            doLog.run();
        }
    }

    public static <K, V> V getOne(K key, Iterable<IMultiDataAccess<K, V>> list) {
        return get(singleton(key), list).get(key);
    }

    /**
     * 多级 cache 带回流, 不缓存 null value.
     *
     * @return map without null value.
     */
    public static <K, V> Map<K, V> get(Collection<K> keys, Iterable<IMultiDataAccess<K, V>> list) {
        return getByIterator(keys, list.iterator());
    }

    private static <K, V> Map<K, V> getByIterator(Collection<K> keys,
            Iterator<IMultiDataAccess<K, V>> iterator) {
        if (!iterator.hasNext() || keys.isEmpty()) {
            return emptyMap();
        }

        IMultiDataAccess<K, V> currentDao = iterator.next();
        Map<K, V> originalResult = currentDao.get(keys);
        Set<K> leftKeys = new HashSet<>();
        Map<K, V> result = newHashMapWithExpectedSize(keys.size());
        for (K key : keys) {
            V value = originalResult.get(key);
            if (value == null) {
                leftKeys.add(key);
            } else {
                result.put(key, value);
            }
        }

        Map<K, V> lowerResult = getByIterator(leftKeys, iterator);
        if (!lowerResult.isEmpty()) {
            currentDao.set(lowerResult);
            result.putAll(lowerResult);
        }
        return result;
    }

    /**
     * 多级 cache 带回流, 不缓存 null value.
     * 如果有单个 IMultiDataAccess 抛出异常，会 fail-safe 并尝试后续的节点
     *
     * @return map without null value.
     * @throws AllFailedException 如果所有 IMultiDataAccess 都抛出异常，则抛出 AllFailException
     */
    public static <K, V> Map<K, V> getFailSafeUnlessAllFailed(Collection<K> keys, Iterable<IMultiDataAccess<K, V>> list) {
        Iterator<IMultiDataAccess<K, V>> iterator = list.iterator();
        if (!iterator.hasNext() || keys.isEmpty()) {
            return emptyMap();
        }
        boolean allFail = true;

        Set<K> leftKeys = new HashSet<>(keys);
        Map<K, V> result = newHashMapWithExpectedSize(leftKeys.size());
        while (iterator.hasNext() && !leftKeys.isEmpty()) {
            IMultiDataAccess<K, V> dao = iterator.next();
            try {
                Map<K, V> currentResult = dao.get(leftKeys);
                currentResult.forEach((k, v) -> {
                    result.put(k, v);
                    leftKeys.remove(k);
                });
                allFail = false;
            } catch (Throwable t) {
                rateLog(() -> logger.warn("[fail safe]", t));
            }
        }

        if (allFail) {
            throw new AllFailedException();
        }
        return result;
    }

}
