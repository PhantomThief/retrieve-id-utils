/**
 * 
 */
package com.github.phantomthief.util;

import static com.google.common.collect.Maps.filterKeys;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @author w.vela
 */
public class RequestContextCache<K, V> extends RequestContextHolder
                                implements IMultiDataAccess<K, V> {

    private static final String PREFIX = "_c";
    private static final int THREAD_LOCAL_NAME_LENGTH = 4;
    private static final Map<String, RequestContextCache<?, ?>> ALL_NAMES = new HashMap<>();

    private static volatile boolean enabled;

    private String uniqueNameForRequestContext;

    public RequestContextCache() {
        synchronized (RequestContextCache.class) {
            String uniqName;
            do {
                uniqName = PREFIX + randomAlphanumeric(THREAD_LOCAL_NAME_LENGTH);
            } while (ALL_NAMES.containsKey(uniqName));
            ALL_NAMES.put(uniqName, this);
            uniqueNameForRequestContext = uniqName;
        }
    }

    public static void enable() {
        enabled = true;
    }

    @SuppressWarnings("unchecked")
    private Map<K, V> init() {
        if (!enabled) { // 性能优先,短路掉
            return null;
        }
        try {
            RequestAttributes attrs = currentRequestAttributes();
            Map<K, V> map = (Map<K, V>) attrs.getAttribute(uniqueNameForRequestContext,
                    SCOPE_REQUEST);
            if (map == null) {
                map = new HashMap<>();
                attrs.setAttribute(uniqueNameForRequestContext, map, SCOPE_REQUEST);
            }
            return map;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 请尽量使用
     * {@link com.github.phantomthief.util.RequestContextCache#get(Collection)}
     */
    public V get(K key) {
        Map<K, V> thisCache;
        if ((thisCache = init()) != null) {
            return thisCache.get(key);
        } else {
            return null;
        }
    }

    @Override
    public Map<K, V> get(Collection<K> keys) {
        Map<K, V> thisCache;
        if (isNotEmpty(keys) && (thisCache = init()) != null) {
            return unmodifiableMap(filterKeys(thisCache, keys::contains));
        } else {
            return emptyMap();
        }
    }

    @Override
    public void set(Map<K, V> dataMap) {
        Map<K, V> thisMap = init();
        if (thisMap != null) {
            if (MapUtils.isNotEmpty(dataMap)) {
                thisMap.putAll(dataMap);
            }
        }
    }

    /**
     * 请尽量使用
     * {@link com.github.phantomthief.util.RequestContextCache#set(Map)}
     *
     */
    public void set(K key, V value) {
        Map<K, V> thisMap = init();
        if (thisMap != null) {
            if (key != null && value != null) {
                thisMap.put(key, value);
            }
        }
    }

    public void remove(K key) {
        Map<K, V> thisMap = init();
        if (thisMap != null) {
            thisMap.remove(key);
        }
    }
}
