package com.github.phantomthief.util;

import static com.github.phantomthief.tuple.Tuple.tuple;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.github.phantomthief.tuple.TwoTuple;

/**
 * @author w.vela
 */
@Deprecated
public class PairKeyRequestContextCache<K1, K2, V> extends RequestContextCache<TwoTuple<K1, K2>, V> {

    public PairKeyRequestContextCache() {
        super();
    }

    public V getMultiKey(K1 key1, K2 key2) {
        return get(tuple(key1, key2));
    }

    public void setMultiKey(K1 key1, K2 key2, V value) {
        set(tuple(key1, key2), value);
    }

    public void removeMultiKey(K1 key1, K2 key2) {
        remove(tuple(key1, key2));
    }

    public IMultiDataAccess<K1, V> accessFixedSecondKey(K2 key2) {
        return new IMultiDataAccess<K1, V>() {

            @Override
            public Map<K1, V> get(Collection<K1> keys) {
                Map<K1, V> result = new HashMap<>();
                for (K1 k1 : keys) {
                    V v = PairKeyRequestContextCache.this.get(tuple(k1, key2));
                    if (v != null) {
                        result.put(k1, v);
                    }
                }
                return result;
            }

            @Override
            public void set(Map<K1, V> dataMap) {
                Map<TwoTuple<K1, K2>, V> mapToSet = newHashMapWithExpectedSize(dataMap.size());
                dataMap.entrySet().stream().filter(entry -> entry.getValue() != null).forEach(
                        entry -> mapToSet.put(tuple(entry.getKey(), key2), entry.getValue()));
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
                    V v = PairKeyRequestContextCache.this.get(tuple(key1, k2));
                    if (v != null) {
                        result.put(k2, v);
                    }
                }
                return result;
            }

            @Override
            public void set(Map<K2, V> dataMap) {
                Map<TwoTuple<K1, K2>, V> mapToSet = newHashMapWithExpectedSize(dataMap.size());
                dataMap.entrySet().stream().filter(entry -> entry.getValue() != null).forEach(
                        entry -> mapToSet.put(tuple(key1, entry.getKey()), entry.getValue()));
                PairKeyRequestContextCache.this.set(mapToSet);
            }
        };
    }
}
