package com.github.phantomthief.util;

import static com.google.common.collect.Maps.filterKeys;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.GuardedBy;

import org.apache.commons.collections4.MapUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @author w.vela
 *
 * use https://github.com/PhantomThief/scope instead.
 */
@Deprecated
public class RequestContextCache<K, V> extends RequestContextHolder
                                implements IMultiDataAccess<K, V> {

    private static final String PREFIX = "_c";
    // 如果同时在使用的id超过组合数，会进入死循环。这里random的id调整到4预期能避免触发BUG
    private static final int THREAD_LOCAL_NAME_LENGTH = 4;
    @GuardedBy("self")
    private static final Set<String> ALL_NAMES = new HashSet<>();

    private static volatile boolean enabled;

    private final String uniqueNameForRequestContext;

    public RequestContextCache() {
        synchronized (ALL_NAMES) {
            String uniqName;
            do {
                uniqName = PREFIX + randomAlphanumeric(THREAD_LOCAL_NAME_LENGTH);
            } while (ALL_NAMES.contains(uniqName));
            ALL_NAMES.add(uniqName);
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

    @Override
    protected void finalize() throws Throwable {
        synchronized (ALL_NAMES) {
            ALL_NAMES.remove(uniqueNameForRequestContext);
        }
        super.finalize();
    }
}
