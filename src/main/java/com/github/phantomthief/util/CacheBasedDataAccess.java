/**
 * 
 */
package com.github.phantomthief.util;

import java.util.Collection;
import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @author w.vela
 */
public class CacheBasedDataAccess<K, V> implements IMultiDataAccess<K, V> {

    private final Cache<K, V> cache;
    private final String name;

    public CacheBasedDataAccess() {
        this(CacheBuilder.newBuilder().weakKeys().weakValues().build());
    }

    /**
     * @param cache
     */
    public CacheBasedDataAccess(Cache<K, V> cache) {
        this(CacheBuilder.newBuilder().weakKeys().weakValues().build(), null);
    }

    /**
     * @param cache
     * @param name
     */
    private CacheBasedDataAccess(Cache<K, V> cache, String name) {
        this.cache = cache;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
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
