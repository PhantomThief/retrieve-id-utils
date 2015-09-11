/**
/**
 * 
 */
package com.github.phantomthief.test;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import com.github.phantomthief.util.LoadingMerger;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author w.vela
 */
public class LoadingMergerTest {

    private List<Integer> loadKeys = Collections.synchronizedList(new ArrayList<>());
    private List<Integer> slowLoadKeys = Collections.synchronizedList(new ArrayList<>());

    @Test
    public void testMerge() throws Exception {
        System.out.println("start to test normal load");
        LoadingMerger<Integer, String> loadingMerger = LoadingMerger.<Integer, String> newBuilder() //
                .timeout(1, TimeUnit.SECONDS) //
                .loader(this::load) //
                .build();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        executorService.execute(() -> {
            Map<Integer, String> result = loadingMerger.get(Arrays.asList(1, 2, 3));
            assert(result.size() == 3);
            assert(result.get(1).equals("1"));
            assert(result.get(2).equals("2"));
            assert(result.get(3).equals("3"));
            System.out.println("result:" + result);
        });
        executorService.execute(() -> {
            Map<Integer, String> result = loadingMerger.get(Arrays.asList(2, 3, 4));
            assert(result.size() == 3);
            assert(result.get(2).equals("2"));
            assert(result.get(3).equals("3"));
            assert(result.get(4).equals("4"));
            System.out.println("result:" + result);
        });
        sleepUninterruptibly(1, TimeUnit.SECONDS);
        executorService.execute(() -> {
            Map<Integer, String> result = loadingMerger.get(Arrays.asList(2, 3, 4));
            assert(result.size() == 3);
            assert(result.get(2).equals("2"));
            assert(result.get(3).equals("3"));
            assert(result.get(4).equals("4"));
            System.out.println("result:" + result);
        });
        MoreExecutors.shutdownAndAwaitTermination(executorService, 1, TimeUnit.DAYS);
        assert(loadKeys.size() == 7);
        for (int i = 1; i <= 4; i++) {
            assert(loadKeys.contains(i));
        }
        System.out.println("loaded keys:" + loadKeys);
    }

    private Map<Integer, String> load(Collection<Integer> keys) {
        sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        loadKeys.addAll(keys);
        System.out.println("loading keys:" + keys);
        return keys.stream().collect(toMap(identity(), Object::toString));
    }

    @Test
    public void testSlowMerge() throws Exception {
        System.out.println("start to test slow load");
        LoadingMerger<Integer, String> loadingMerger = LoadingMerger.<Integer, String> newBuilder() //
                .timeout(500, TimeUnit.MILLISECONDS) //
                .loader(this::slowLoad) //
                .build();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        executorService.execute(() -> {
            Map<Integer, String> result = loadingMerger.get(Arrays.asList(1, 2, 3));
            assert(result.size() == 3);
            assert(result.get(1).equals("1"));
            assert(result.get(2).equals("2"));
            assert(result.get(3).equals("3"));
            System.out.println("result:" + result);
        });
        executorService.execute(() -> {
            Map<Integer, String> result = loadingMerger.get(Arrays.asList(2, 3, 4));
            assert(result.size() == 3);
            assert(result.get(2).equals("2"));
            assert(result.get(3).equals("3"));
            assert(result.get(4).equals("4"));
            System.out.println("result:" + result);
        });
        MoreExecutors.shutdownAndAwaitTermination(executorService, 1, TimeUnit.DAYS);
        for (int i = 1; i <= 4; i++) {
            assert(slowLoadKeys.contains(i));
        }
        System.out.println("loaded keys:" + slowLoadKeys);
    }

    private Map<Integer, String> slowLoad(Collection<Integer> keys) {
        sleepUninterruptibly(RandomUtils.nextInt(2, 5), TimeUnit.SECONDS);
        slowLoadKeys.addAll(keys);
        System.out.println("slow loading keys:" + keys);
        return keys.stream().collect(toMap(identity(), Object::toString));
    }
}
