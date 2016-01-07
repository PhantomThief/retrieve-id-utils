/**
 * 
 */
package com.github.phantomthief.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author w.vela
 */
public interface IMultiDataAccess<K, V> {

    @Deprecated
    public default String getName() {
        return null;
    }

    public default Map<K, V> get(Collection<K> keys) {
        return Collections.emptyMap();
    }

    public default void set(Map<K, V> dataMap) {
    }
}
