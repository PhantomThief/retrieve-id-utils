/**
 * 
 */
package com.github.phantomthief.util;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author w.vela
 */
public final class RetrieveIdUtils {

    public static <K, V> V get(K key, List<IMultiDataAccess<K, V>> list) {
        return get(singleton(key), list).get(key);
    }

    /**
     * 多级 cache 带回流, 不缓存 null value.
     *
     * @return map without null value.
     */
    public static <K, V> Map<K, V> get(Collection<K> keys, List<IMultiDataAccess<K, V>> list) {
        return getByIterator(keys, list.iterator());
    }

    private static <K, V> Map<K, V> getByIterator(Collection<K> keys,
            Iterator<IMultiDataAccess<K, V>> iterator) {
        if (!iterator.hasNext() || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        IMultiDataAccess<K, V> currentDao = iterator.next();
        Map<K, V> currentResult = excludeNullValues(currentDao.get(keys));
        Set<K> leftKeys = subtract(keys, currentResult.keySet());

        Map<K, V> result = new HashMap<>(currentResult);

        Map<K, V> lowerResult = getByIterator(leftKeys, iterator);
        if (!lowerResult.isEmpty()) {
            currentDao.set(lowerResult);
            result.putAll(lowerResult);
        }
        return result;
    }

    private static <K, V> Map<K, V> excludeNullValues(Map<K, V> map) {
        return map.entrySet().stream().filter(e -> e.getValue() != null)
                .collect(toMap(Entry::getKey, Entry::getValue));
    }

    private static <K> Set<K> subtract(Collection<K> one, Set<K> another) {
        return one.stream().filter(o -> !another.contains(o)).collect(Collectors.toSet());
    }

}
