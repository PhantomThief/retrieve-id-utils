/**
 * 
 */
package com.github.phantomthief.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface IMultiDataAccess<K, V> {

    public default Map<K, V> get(Collection<K> keys) {
        return Collections.emptyMap();
    }

    public default void set(Map<K, V> dataMap) {
    };
}