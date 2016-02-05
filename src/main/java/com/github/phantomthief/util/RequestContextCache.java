/**
 * 
 */
package com.github.phantomthief.util;

import static com.google.common.collect.Maps.filterKeys;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyMap;
import static java.util.Collections.synchronizedSet;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections4.MapUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @author w.vela
 */
public class RequestContextCache<K, V> extends RequestContextHolder implements
                                                                   IMultiDataAccess<K, V> {

    private static final String PREFIX = "_c";

    private static final int THREAD_LOCAL_NAME_LENGTH = 2;

    private static final Map<String, RequestContextCache<?, ?>> ALL_NAMES = new HashMap<>();
    private final String declareLocation;
    private final Set<Class<?>> valueTypes = synchronizedSet(new HashSet<>());
    private final AtomicLong request = new AtomicLong();
    private final AtomicLong hit = new AtomicLong();
    private final AtomicLong set = new AtomicLong();
    private final AtomicLong remove = new AtomicLong();
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
        String location;
        try {
            location = currentThread().getStackTrace()[locationCallStackDepth()].toString();
        } catch (Throwable e) {
            location = null;
        }
        declareLocation = location;
    }

    public static List<RequestContextCache<?, ?>.CacheStats> getStats() {
        synchronized (RequestContextCache.class) {
            return ALL_NAMES.values().stream().map(i -> i.new CacheStats()).collect(toList());
        }
    }

    protected int locationCallStackDepth() {
        return 2;
    }

    @SuppressWarnings("unchecked")
    private Map<K, V> init() {
        try {
            RequestAttributes attrs = currentRequestAttributes();
            ConcurrentHashMap<K, V> concurrentHashMap = (ConcurrentHashMap<K, V>) attrs
                    .getAttribute(uniqueNameForRequestContext, RequestAttributes.SCOPE_REQUEST);
            if (concurrentHashMap == null) {
                synchronized (attrs) {
                    concurrentHashMap = (ConcurrentHashMap<K, V>) attrs.getAttribute(
                            uniqueNameForRequestContext, RequestAttributes.SCOPE_REQUEST);
                    if (concurrentHashMap == null) {
                        concurrentHashMap = new ConcurrentHashMap<>();
                        attrs.setAttribute(uniqueNameForRequestContext, concurrentHashMap,
                                RequestAttributes.SCOPE_REQUEST);
                    }
                }
            }
            return concurrentHashMap;
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
        request.addAndGet(1);
        if ((thisCache = init()) != null) {
            V v = thisCache.get(key);
            if (v != null) {
                hit.addAndGet(1);
            }
            return v;
        } else {
            return null;
        }
    }

    @Override
    public Map<K, V> get(Collection<K> keys) {
        Map<K, V> thisCache;
        request.addAndGet(size(keys));
        if (isNotEmpty(keys) && (thisCache = init()) != null) {
            Map<K, V> map = unmodifiableMap(filterKeys(thisCache, keys::contains));
            hit.addAndGet(map.size());
            return map;
        } else {
            return emptyMap();
        }
    }

    @Override
    public void set(Map<K, V> dataMap) {
        Map<K, V> thisMap = init();
        if (thisMap != null) {
            if (MapUtils.isNotEmpty(dataMap)) {
                set.addAndGet(size(dataMap));
                thisMap.putAll(dataMap);
                valueTypes.addAll(dataMap.values().stream() //
                        .filter(Objects::nonNull) //
                        .map(Object::getClass) //
                        .distinct() //
                        .collect(toList()));
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
                set.addAndGet(1);
                thisMap.put(key, value);
                valueTypes.add(value.getClass());
            }
        }
    }

    public void remove(K key) {
        Map<K, V> thisMap = init();
        if (thisMap != null) {
            remove.incrementAndGet();
            thisMap.remove(key);
        }
    }

    public final class CacheStats {

        public double getHitRate() {
            return (double) getHitCount() / getRequestCount();
        }

        public long getHitCount() {
            return hit.get();
        }

        public long getRequestCount() {
            return request.get();
        }

        public long getSetCount() {
            return set.get();
        }

        public long getRemoveCount() {
            return remove.get();
        }

        public String getDeclareLocation() {
            return declareLocation;
        }

        public Collection<Class<?>> getValueTypes() {
            return valueTypes;
        }
    }

}
