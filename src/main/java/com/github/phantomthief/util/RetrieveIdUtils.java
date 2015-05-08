/**
 * 
 */
package com.github.phantomthief.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.github.phantomthief.stats.StatsHelper;
import com.github.phantomthief.tuple.Tuple;
import com.github.phantomthief.tuple.TwoTuple;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author w.vela <vela@longbeach-inc.com>
 *
 * @date 2014年4月9日 下午3:19:10
 */
public final class RetrieveIdUtils {

    private static final int MIN_INITIAL_CAPACITY = 16;

    public static <K, V> Map<K, V> get(Collection<K> keys, List<IMultiDataAccess<K, V>> list) {
        return get(keys, list, null);
    }

    public static <K, V> Map<K, V> get(Collection<K> keys, List<IMultiDataAccess<K, V>> list,
            StatsHelper<String, AccessCounter> statsHelper) {
        Map<K, V> result = Maps.newHashMapWithExpectedSize(keys.size());
        accessCollection(keys, result, list.iterator(), statsHelper);
        return result;
    }

    private static <K, V> void accessCollection(final Collection<K> sourceIds,
            final Map<K, V> result, final Iterator<IMultiDataAccess<K, V>> iterator,
            StatsHelper<String, AccessCounter> statsHelper) {

        if (iterator.hasNext() && CollectionUtils.isNotEmpty(sourceIds)) {
            final IMultiDataAccess<K, V> dao = iterator.next();
            long s = System.currentTimeMillis();
            final Map<K, V> retreivedModels = dao.get(sourceIds).entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            if (statsHelper != null && dao.getName() != null) {
                statsHelper.stats(dao.getName(), c -> c.statsGet((System.currentTimeMillis() - s),
                        retreivedModels.size(), sourceIds.size()));
            }

            TwoTuple<Boolean, Collection<K>> allKeysReady = allKeysReady(retreivedModels,
                    sourceIds);
            if (!allKeysReady.first) {
                // 不够，那么需要调用下级缓存

                // 下一级要抓的ids
                final Collection<K> nextIds = allKeysReady.second;

                // 本级要把上级处理完的缓存住
                accessCollection(nextIds, result, iterator, statsHelper);
                Map<K, V> dataToSet = subtractByKey(result, retreivedModels);
                if (!dataToSet.isEmpty()) {
                    long t = System.currentTimeMillis();
                    dao.set(dataToSet);
                    if (statsHelper != null && dao.getName() != null) {
                        statsHelper.stats(dao.getName(),
                                a -> a.statsSet((System.currentTimeMillis() - t)));
                    }
                }
            }
            result.putAll(retreivedModels);
        }
    }

    private static <K, V> TwoTuple<Boolean, Collection<K>> allKeysReady(Map<K, V> dataMap,
            Collection<K> allKeys) {
        boolean allReady = true;
        Set<K> leftKeys = Sets.newHashSetWithExpectedSize(
                Math.max(MIN_INITIAL_CAPACITY, allKeys.size() - dataMap.size()));
        for (K key : allKeys) {
            if (dataMap.get(key) == null) {
                allReady = false;
                leftKeys.add(key);
            }
        }
        return Tuple.<Boolean, Collection<K>> tuple(allReady, leftKeys);
    }

    private static <K, V> Map<K, V> subtractByKey(Map<K, V> a, Map<K, V> b) {
        Map<K, V> result = Maps.newHashMapWithExpectedSize(a.size());
        for (Entry<K, V> entry : a.entrySet()) {
            if (b.get(entry.getKey()) == null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

}
