/**
 * 
 */
package com.github.phantomthief.util;

import java.util.Collection;
import java.util.Map;

import com.google.common.cache.Cache;

/**
 * @author w.vela
 */
public class CacheBasedDataAccess<K, V> implements IMultiDataAccess<K, V> {

    private final Cache<K, V> cache;

    private CacheBasedDataAccess(Cache<K, V> cache) {
        this.cache = cache;
    }

    public static <K, V> IMultiDataAccess<K, V> of(Cache<K, V> cache) {
        return new CacheBasedDataAccess<>(cache);
    }

    @Override
    public Map<K, V> get(Collection<K> keys) {
        return cache.getAllPresent(keys);
    }

    @Override
    public void set(Map<K, V> dataMap) {
        if (dataMap != null) {
            dataMap.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .forEach(entry -> cache.put(entry.getKey(), entry.getValue()));
        }
    }
}
