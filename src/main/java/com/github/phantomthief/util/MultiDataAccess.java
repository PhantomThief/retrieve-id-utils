/**
 * 
 */
package com.github.phantomthief.util;

/**
 * @author w.vela
 */
@Deprecated
public abstract class MultiDataAccess<K, V> implements IMultiDataAccess<K, V> {

    private final String name;

    /**
     * @param name
     */
    public MultiDataAccess(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
