/**
 * 
 */
package com.github.phantomthief.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * 
 * @author w.vela
 */
public final class GlobalCache<K, V> implements IMultiDataAccess<K, V> {

    private final Cache<K, V> cache = CacheBuilder.newBuilder().weakKeys().weakValues().build();

    @Override
    public Map<K, V> get(Collection<K> keys) {
        Map<K, V> result = new HashMap<>();
        for (K key : keys) {
            V value = cache.getIfPresent(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    @Override
    public void set(Map<K, V> dataMap) {
        cache.putAll(dataMap);
    };

    public void remove(K key) {
        cache.invalidate(key);
    }

}
