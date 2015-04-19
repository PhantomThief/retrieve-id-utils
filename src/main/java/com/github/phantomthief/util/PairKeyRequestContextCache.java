/**
 * 
 */
package com.github.phantomthief.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.github.phantomthief.tuple.Tuple;
import com.github.phantomthief.tuple.TwoTuple;
import com.google.common.collect.Maps;

/**
 * @author w.vela
 */
public class PairKeyRequestContextCache<K1, K2, V>
        extends RequestContextCache<TwoTuple<K1, K2>, V> {

    public IMultiDataAccess<K1, V> accessFixedSecondKey(K2 key2) {
        return new IMultiDataAccess<K1, V>() {

            @Override
            public Map<K1, V> get(Collection<K1> keys) {
                Map<K1, V> result = new HashMap<>();
                for (K1 k1 : keys) {
                    V v = PairKeyRequestContextCache.this.get(Tuple.tuple(k1, key2));
                    if (v != null) {
                        result.put(k1, v);
                    }
                }
                return result;
            }

            @Override
            public void set(Map<K1, V> dataMap) {
                Map<TwoTuple<K1, K2>, V> mapToSet = Maps.newHashMapWithExpectedSize(dataMap.size());
                for (Entry<K1, V> entry : dataMap.entrySet()) {
                    if (entry.getValue() != null) {
                        mapToSet.put(Tuple.tuple(entry.getKey(), key2), entry.getValue());
                    }
                }
                PairKeyRequestContextCache.this.set(mapToSet);
            }

        };
    }

    public IMultiDataAccess<K2, V> accessFixedFirstKey(K1 key1) {
        return new IMultiDataAccess<K2, V>() {

            @Override
            public Map<K2, V> get(Collection<K2> keys) {
                Map<K2, V> result = new HashMap<>();
                for (K2 k2 : keys) {
                    V v = PairKeyRequestContextCache.this.get(Tuple.tuple(key1, k2));
                    if (v != null) {
                        result.put(k2, v);
                    }
                }
                return result;
            }

            @Override
            public void set(Map<K2, V> dataMap) {
                Map<TwoTuple<K1, K2>, V> mapToSet = Maps.newHashMapWithExpectedSize(dataMap.size());
                for (Entry<K2, V> entry : dataMap.entrySet()) {
                    if (entry.getValue() != null) {
                        mapToSet.put(Tuple.tuple(key1, entry.getKey()), entry.getValue());
                    }
                }
                PairKeyRequestContextCache.this.set(mapToSet);
            }

        };
    }
}
