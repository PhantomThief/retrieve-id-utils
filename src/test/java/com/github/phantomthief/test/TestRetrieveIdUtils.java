/**
 * 
 */
package com.github.phantomthief.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.junit.Test;

import com.github.phantomthief.stats.StatsHelper;
import com.github.phantomthief.util.AccessCounter;
import com.github.phantomthief.util.IMultiDataAccess;
import com.github.phantomthief.util.RetrieveIdUtils;

/**
 * @author w.vela
 */
public class TestRetrieveIdUtils {

    @Test
    public void test() {
        StatsHelper<String, AccessCounter> statsHelper = StatsHelper.<AccessCounter> newBuilder() //
                .setCounterReset(old -> new AccessCounter()) //
                .build();

        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);
        Map<Integer, String> firstSet = new HashMap<>();
        Map<Integer, String> result = RetrieveIdUtils.get(ids, Arrays.asList( //
                new IMultiDataAccess<Integer, String>() {

                    @Override
                    public String getName() {
                        return "first one";
                    }

                    @Override
                    public Map<Integer, String> get(Collection<Integer> keys) {
                        return firstGet(keys);
                    }

                    @Override
                    public void set(Map<Integer, String> dataMap) {
                        firstSet.putAll(dataMap);
                    }
                }, //
                new IMultiDataAccess<Integer, String>() {

                    @Override
                    public String getName() {
                        return "second one";
                    }

                    @Override
                    public Map<Integer, String> get(Collection<Integer> keys) {
                        return secondGet(keys);
                    }

                }), statsHelper);
        for (Integer id : ids) {
            if (id < 4) {
                assert(("a" + id).equals(result.get(id)));
            } else {
                assert(!result.containsKey(id));
            }
        }
        for (Entry<Integer, String> entry : firstSet.entrySet()) {
            assert(entry.getKey() < 4);
            assert(("a" + entry.getKey()).equals(entry.getValue()));
        }

        System.out.println(statsHelper.getFriendlyStats(Function.identity(), Function.identity()));
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
}
