/**
 * 
 */
package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

/**
 * @author w.vela
 */
public final class RandomCombinedUtils {

    public static <K, V> Map<K, V> combine(Collection<K> keys,
            Collection<IMultiDataAccess<K, V>> dataAccesses, boolean doSetData) {
        checkNotNull(dataAccesses);
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyMap();
        }

        List<IMultiDataAccess<K, V>> accesses = new ArrayList<>(dataAccesses);
        Collections.shuffle(accesses);
        Map<K, V> result = Maps.newHashMapWithExpectedSize(keys.size());
        SetMultimap<IMultiDataAccess<K, V>, K> missedKeys = HashMultimap.create();
        Set<K> leftKeys = new HashSet<>(keys);
        for (IMultiDataAccess<K, V> iMultiDataAccess : accesses) {
            Map<K, V> thisResult = iMultiDataAccess.get(leftKeys);
            missedKeys.putAll(iMultiDataAccess,
                    CollectionUtils.subtract(leftKeys, thisResult.keySet()));
            result.putAll(thisResult);
            leftKeys.removeAll(result.keySet());
            if (leftKeys.isEmpty()) {
                break;
            }
        }
        if (doSetData) {
            missedKeys.asMap().forEach((iMultiDataAccess, ids) -> {
                Map<K, V> toSet = Maps.newHashMapWithExpectedSize(ids.size());
                for (K k : ids) {
                    V value = result.get(k);
                    if (value != null) {
                        toSet.put(k, value);
                    }
                }
                if (!toSet.isEmpty()) {
                    iMultiDataAccess.set(toSet);
                }
            });
        }
        return result;
    }
}
