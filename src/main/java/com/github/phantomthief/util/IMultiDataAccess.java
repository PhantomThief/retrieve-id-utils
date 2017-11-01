package com.github.phantomthief.util;

import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author w.vela
 */
public interface IMultiDataAccess<K, V> {

    static <K, V> IMultiDataAccess<K, V> getOnly(Function<Collection<K>, Map<K, V>> getFunction) {
        return new IMultiDataAccess<K, V>() {

            @Override
            public Map<K, V> get(Collection<K> keys) {
                return getFunction.apply(keys);
            }
        };
    }

    static <K, V> IMultiDataAccess<K, V> setOnly(Consumer<Map<K, V>> setFunction) {
        return new IMultiDataAccess<K, V>() {

            @Override
            public void set(Map<K, V> dataMap) {
                setFunction.accept(dataMap);
            }
        };
    }

    static <K, V> IMultiDataAccessBuilder<K, V>
            get(Function<Collection<K>, Map<K, V>> getFunction) {
        return new IMultiDataAccessBuilder<>(getFunction);
    }

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

    class IMultiDataAccessBuilder<K, V> {

        private final Function<Collection<K>, Map<K, V>> getFunction;

        private IMultiDataAccessBuilder(Function<Collection<K>, Map<K, V>> getFunction) {
            this.getFunction = getFunction;
        }

        public IMultiDataAccess<K, V> set(Consumer<Map<K, V>> setFunction) {
            return new IMultiDataAccess<K, V>() {

                @Override
                public Map<K, V> get(Collection<K> keys) {
                    return getFunction.apply(keys);
                }

                @Override
                public void set(Map<K, V> dataMap) {
                    setFunction.accept(dataMap);
                }
            };
        }
    }
}
