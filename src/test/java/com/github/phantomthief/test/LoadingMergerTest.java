/**
/**
 * 
 */
package com.github.phantomthief.test;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import com.github.phantomthief.util.LoadingMerger;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author w.vela
 */
public class LoadingMergerTest {

    private final AtomicInteger slowLoad = new AtomicInteger();
    private List<Integer> loadKeys = Collections.synchronizedList(new ArrayList<>());
    private List<Integer> slowLoadKeys = Collections.synchronizedList(new ArrayList<>());

    @Test
    public void testMerge() throws Exception {
        System.out.println("start to test normal load");
        LoadingMerger<Integer, String> loadingMerger = LoadingMerger.<Integer, String> newBuilder() //
                .timeout(1, SECONDS) //
                .loader(this::load) //
                .build();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        executorService.execute(() -> {
            Map<Integer, String> result = loadingMerger.get(Arrays.asList(1, 2, 3));
            assertEquals(result.size(), 3);
            assertEquals(result.get(1), "1");
            assertEquals(result.get(2), "2");
            assertEquals(result.get(3), "3");
            System.out.println("result:" + result);
        });
        executorService.execute(() -> {
            Map<Integer, String> result = loadingMerger.get(Arrays.asList(2, 3, 4));
            assertEquals(result.size(), 3);
            assertEquals(result.get(2), "2");
            assertEquals(result.get(3), "3");
            assertEquals(result.get(4), "4");
            System.out.println("result:" + result);
        });
        sleepUninterruptibly(1, SECONDS);
        executorService.execute(() -> {
            Map<Integer, String> result = loadingMerger.get(Arrays.asList(2, 3, 4));
            assertEquals(result.size(), 3);
            assertEquals(result.get(2), "2");
            assertEquals(result.get(3), "3");
            assertEquals(result.get(4), "4");
            System.out.println("result:" + result);
        });
        MoreExecutors.shutdownAndAwaitTermination(executorService, 1, DAYS);
        assertEquals(loadKeys.size(), 7);
        for (int i = 1; i <= 4; i++) {
            assertTrue(loadKeys.contains(i));
        }
        System.out.println("loaded keys:" + loadKeys);
    }

    private Map<Integer, String> load(Collection<Integer> keys) {
        sleepUninterruptibly(500, MILLISECONDS);
        loadKeys.addAll(keys);
        System.out.println("loading keys:" + keys);
        return keys.stream().collect(toMap(identity(), Object::toString));
    }

    @Test
    public void testSlowMerge() throws Exception {
        System.out.println("start to test slow load");
        LoadingMerger<Integer, String> loadingMerger = LoadingMerger.<Integer, String> newBuilder() //
                .timeout(500, MILLISECONDS) //
                .loader(this::slowLoad) //
                .build();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        executorService
                .execute(() -> {
                    Map<Integer, String> result = loadingMerger.get(Arrays.asList(1, 2, 3, 10, 11,
                            12, 13));
                    assertEquals(result.size(), 7);
                    assertEquals(result.get(1), "1");
                    assertEquals(result.get(2), "2");
                    assertEquals(result.get(3), "3");
                    System.out.println("result:" + result);
                });
        executorService
                .execute(() -> {
                    Map<Integer, String> result = loadingMerger.get(Arrays.asList(2, 3, 4, 10, 11,
                            12, 13));
                    assertEquals(result.size(), 7);
                    assertEquals(result.get(2), "2");
                    assertEquals(result.get(3), "3");
                    assertEquals(result.get(4), "4");
                    System.out.println("result:" + result);
                });
        executorService.execute(() -> {
            Map<Integer, String> result = loadingMerger.get(Arrays.asList(3, 4));
            assertEquals(result.size(), 2);
            assertEquals(result.get(3), "3");
            assertEquals(result.get(4), "4");
            System.out.println("result:" + result);
        });
        MoreExecutors.shutdownAndAwaitTermination(executorService, 1, DAYS);
        for (int i = 1; i <= 4; i++) {
            assertTrue(slowLoadKeys.contains(i));
        }
        System.out.println("loaded keys:" + slowLoadKeys);
        System.out.println("slow load count:" + slowLoad);
    }

    private Map<Integer, String> slowLoad(Collection<Integer> keys) {
        System.out.println("slow loading keys start:" + keys);
        sleepUninterruptibly(RandomUtils.nextInt(2, 5), SECONDS);
        slowLoadKeys.addAll(keys);
        System.out.println("slow loading keys end:" + keys);
        slowLoad.incrementAndGet();
        return keys.stream().collect(toMap(identity(), Object::toString));
    }
}
