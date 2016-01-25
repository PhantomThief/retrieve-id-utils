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

    default Map<K, V> get(Collection<K> keys) {
        return emptyMap();
    }

    default void set(Map<K, V> dataMap) {
    }
}
