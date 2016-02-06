/**
 * 
 */
package com.github.phantomthief.util;

import static com.github.phantomthief.tuple.Tuple.tuple;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.google.common.collect.Sets.newHashSetWithExpectedSize;
import static java.lang.Math.max;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.phantomthief.tuple.TwoTuple;

/**
 * @author w.vela
 */
public final class RetrieveIdUtils {

    private static final int MIN_INITIAL_CAPACITY = 16;

    public static <K, V> V get(K key, List<IMultiDataAccess<K, V>> list) {
        return get(singleton(key), list).get(key);
    }

    public static <K, V> Map<K, V> get(Collection<K> keys, List<IMultiDataAccess<K, V>> list) {
        Map<K, V> result = newHashMapWithExpectedSize(keys.size());
        accessCollection(keys, result, list.iterator());
        return result;
    }

    private static <K, V> void accessCollection(final Collection<K> sourceIds,
            final Map<K, V> result, final Iterator<IMultiDataAccess<K, V>> iterator) {

        if (iterator.hasNext() && isNotEmpty(sourceIds)) {
            final IMultiDataAccess<K, V> dao = iterator.next();
            final Map<K, V> retreivedModels = dao.get(sourceIds).entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .collect(toMap(Entry::getKey, Entry::getValue));

            TwoTuple<Boolean, Collection<K>> allKeysReady = allKeysReady(retreivedModels, sourceIds);
            if (!allKeysReady.first) {
                // 不够，那么需要调用下级缓存

                // 下一级要抓的ids
                final Collection<K> nextIds = allKeysReady.second;

                // 本级要把上级处理完的缓存住
                accessCollection(nextIds, result, iterator);
                Map<K, V> dataToSet = subtractByKey(result, retreivedModels);
                if (!dataToSet.isEmpty()) {
                    dao.set(dataToSet);
                }
            }
            result.putAll(retreivedModels);
        }
    }

    private static <K, V> TwoTuple<Boolean, Collection<K>> allKeysReady(Map<K, V> dataMap,
            Collection<K> allKeys) {
        boolean allReady = true;
        Set<K> leftKeys = newHashSetWithExpectedSize(max(MIN_INITIAL_CAPACITY, allKeys.size()
                - dataMap.size()));
        for (K key : allKeys) {
            if (dataMap.get(key) == null) {
                allReady = false;
                leftKeys.add(key);
            }
        }
        return tuple(allReady, leftKeys);
    }

    private static <K, V> Map<K, V> subtractByKey(Map<K, V> a, Map<K, V> b) {
        Map<K, V> result = newHashMapWithExpectedSize(a.size());
        a.entrySet().stream().filter(entry -> b.get(entry.getKey()) == null)
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

}
