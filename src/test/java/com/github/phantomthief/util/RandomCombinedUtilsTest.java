/**
 * 
 */
package com.github.phantomthief.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * @author w.vela
 */
public class RandomCombinedUtilsTest {

    private static Map<Integer, String> mem1 = new HashMap<>();
    private static Map<Integer, String> mem2 = new HashMap<>();
    private static Map<Integer, String> mem3 = new HashMap<>();

    @BeforeClass
    public static void setup() {
        for (int i = 0; i < 1000; i++) {
            switch (RandomUtils.nextInt(0, 3)) {
                case 0:
                    mem1.put(i, i + "");
                    break;
                case 1:
                    mem2.put(i, i + "");
                    break;
                case 2:
                    mem3.put(i, i + "");
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Test method for {@link com.github.phantomthief.util.RandomCombinedUtils#combine(java.util.Collection, java.util.Collection, boolean)}.
     */
    @Test
    public void testCombine() {
        IMultiDataAccess<Integer, String> data1 = wrap(mem1);
        IMultiDataAccess<Integer, String> data2 = wrap(mem2);
        IMultiDataAccess<Integer, String> data3 = wrap(mem3);

        for (int i = 0; i < 100; i++) {
            Set<Integer> keys = randomKey(0, 1000, 20);
            Map<Integer, String> result = RandomCombinedUtils.combine(keys,
                    ImmutableList.of(data1, data2, data3), true);
            assertEquals(keys.size(), result.size());
            for (Integer integer : keys) {
                assertEquals(String.valueOf(integer), result.get(integer));
            }
        }
    }

    private Set<Integer> randomKey(int from, int end, int size) {
        Set<Integer> result = Sets.newHashSetWithExpectedSize(size);
        while (result.size() < size) {
            result.add(RandomUtils.nextInt(from, end));
        }
        return result;
    }

    private IMultiDataAccess<Integer, String> wrap(Map<Integer, String> map) {
        return new IMultiDataAccess<Integer, String>() {

            @Override
            public Map<Integer, String> get(Collection<Integer> keys) {
                Map<Integer, String> result = new HashMap<>();
                for (Integer integer : keys) {
                    String value = map.get(integer);
                    if (value != null) {
                        result.put(integer, value);
                    }
                }
                return result;
            }

            @Override
            public void set(Map<Integer, String> dataMap) {
                dataMap.keySet().forEach(keyToSet -> assertFalse(map.containsKey(keyToSet)));
                map.putAll(dataMap);
            }
        };
    }
}
