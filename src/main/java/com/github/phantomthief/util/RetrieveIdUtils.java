package com.github.phantomthief.util;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author w.vela
 */
public final class RetrieveIdUtils {

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
}
