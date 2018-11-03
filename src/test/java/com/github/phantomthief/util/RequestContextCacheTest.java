package com.github.phantomthief.util;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author w.vela
 * Created on 2018-11-03.
 */
class RequestContextCacheTest {

    @Disabled
    @Test
    void test() {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executor.execute(() -> {
                while (true) {
                    test1();
                    sleepUninterruptibly(1, SECONDS);
                }
            });
        }
        shutdownAndAwaitTermination(executor, 1, DAYS);
    }

    private void test1() {
        RequestContextCache cache = new RequestContextCache();
    }
}