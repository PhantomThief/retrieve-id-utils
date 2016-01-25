/**
 * 
 */
package com.github.phantomthief.util;

import static com.google.common.cache.CacheBuilder.newBuilder;

import java.util.Collection;
import java.util.Map;

import com.google.common.cache.Cache;

/**
 * @author w.vela
 */
public class CacheBasedDataAccess<K, V> implements IMultiDataAccess<K, V> {

    private final Cache<K, V> cache;

    public CacheBasedDataAccess() {
        this(newBuilder().weakKeys().weakValues().build());
    }

    /**
     * @param cache
     */
    public CacheBasedDataAccess(Cache<K, V> cache) {
        this.cache = cache;
    }

    @Override
    public Map<K, V> get(Collection<K> keys) {
        return cache.getAllPresent(keys);
    }

    @Override
    public void set(Map<K, V> dataMap) {
        cache.putAll(dataMap);
    }
}
