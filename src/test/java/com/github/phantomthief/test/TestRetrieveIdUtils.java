package com.github.phantomthief.test;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.phantomthief.util.IMultiDataAccess;
import com.github.phantomthief.util.RetrieveIdUtils;
import com.github.phantomthief.util.AllFailedException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * @author w.vela
 */
class TestRetrieveIdUtils {

    @Test
    void test() {
        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);
        Map<Integer, String> firstSet = new HashMap<>();
        Map<Integer, String> result = RetrieveIdUtils.get(ids, Arrays.asList(
                new IMultiDataAccess<Integer, String>() {

                    @Override
                    public Map<Integer, String> get(Collection<Integer> keys) {
                        return firstGet(keys);
                    }

                    @Override
                    public void set(Map<Integer, String> dataMap) {
                        firstSet.putAll(dataMap);
                    }
                },
                new IMultiDataAccess<Integer, String>() {

                    @Override
                    public Map<Integer, String> get(Collection<Integer> keys) {
                        return secondGet(keys);
                    }

                }));
        for (Integer id : ids) {
            if (id < 4) {
                assert (("a" + id).equals(result.get(id)));
            } else {
                assert (!result.containsKey(id));
            }
        }
        for (Entry<Integer, String> entry : firstSet.entrySet()) {
            assert (entry.getKey() < 4);
            assert (("a" + entry.getKey()).equals(entry.getValue()));
        }
    }

    private Map<Integer, String> firstGet(Collection<Integer> ids) {
        Map<Integer, String> result = new HashMap<>();
        for (Integer id : ids) {
            if (id < 3) {
                result.put(id, "a" + id);
            }
        }
        return result;
    }

    private Map<Integer, String> secondGet(Collection<Integer> ids) {
        Map<Integer, String> result = new HashMap<>();
        for (Integer id : ids) {
            if (id < 4) {
                result.put(id, "a" + id);
            }
        }
        return result;
    }

    @Test
    void testGetFailSafe() {
        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);
        Map<Integer, String> expectResult = ImmutableMap.of(1, "1", 2, "2", 3, "3", 4, "4", 5, "5");

        IMultiDataAccess<Integer, String> errorDao = new IMultiDataAccess<Integer, String>() {
            @Override
            public Map<Integer, String> get(Collection<Integer> keys) {
                throw new RuntimeException();
            }
        };

        IMultiDataAccess<Integer, String> successDao = new IMultiDataAccess<Integer, String>() {
            @Override
            public Map<Integer, String> get(Collection<Integer> keys) {
                return keys.stream().map(k -> new SimpleEntry<>(k, String.valueOf(k)))
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            }
        };

        AtomicInteger counter = new AtomicInteger(0);
        IMultiDataAccess<Integer, String> counterDao = new IMultiDataAccess<Integer, String>() {
            @Override
            public Map<Integer, String> get(Collection<Integer> keys) {
                counter.incrementAndGet();
                return Collections.emptyMap();
            }
        };

        Map<Integer, String> result1 =
                RetrieveIdUtils.getFailSafeUnlessAllFailed(ids, Arrays.asList(errorDao, successDao, counterDao));
        Assertions.assertTrue(Maps.difference(expectResult, result1).areEqual());
        Assertions.assertEquals(0, counter.get());

        Map<Integer, String> result2 = RetrieveIdUtils.getFailSafeUnlessAllFailed(ids, Collections.emptyList());
        Assertions.assertTrue(result2.isEmpty());

        Assertions.assertThrows(AllFailedException.class,
                () -> RetrieveIdUtils.getFailSafeUnlessAllFailed(ids, Collections.singletonList(errorDao)));

        Assertions.assertThrows(AllFailedException.class,
                () -> RetrieveIdUtils.getFailSafeUnlessAllFailed(ids, Arrays.asList(errorDao, errorDao)));

        RetrieveIdUtils.getFailSafeUnlessAllFailed(ids, Arrays.asList(errorDao, counterDao, errorDao));
        Assertions.assertEquals(1, counter.get());

        counter.set(0);
        RetrieveIdUtils.getFailSafeUnlessAllFailed(ids, Arrays.asList(errorDao, counterDao, counterDao));
        Assertions.assertEquals(2, counter.get());
    }
}
