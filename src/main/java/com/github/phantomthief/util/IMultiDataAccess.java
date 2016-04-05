/**
 * 
 */
package com.github.phantomthief.util;

import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.Map;

/**
 * @author w.vela
 */
public interface IMultiDataAccess<K, V> {

    /**
     * @param keys without null.
     * @return map without null values.
     */
    default Map<K, V> get(Collection<K> keys) {
        return emptyMap();
    }

    /**
     * @param dataMap without null values.
     */
    default void set(Map<K, V> dataMap) {
    }
}
