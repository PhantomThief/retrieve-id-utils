/**
 * 
 */
package com.github.phantomthief.util;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author w.vela
 */
public class AccessCounter {

    private final AtomicLong requestKeyCount = new AtomicLong();
    private final AtomicLong hitKeyCount = new AtomicLong();

    private final AtomicLong getCount = new AtomicLong();
    private final AtomicLong getCost = new AtomicLong();
    private final AtomicLong setCount = new AtomicLong();
    private final AtomicLong setCost = new AtomicLong();

    public long getRequestKeyCount() {
        return requestKeyCount.get();
    }

    public long getHitKeyCount() {
        return hitKeyCount.get();
    }

    public long getGetCost() {
        return getCost.get();
    }

    public long getSetCost() {
        return setCost.get();
    }

    public long getGetCount() {
        return getCount.get();
    }

    public long getSetCount() {
        return setCount.get();
    }

    public void statsGet(long getCost, long hitCount, long requestCount) {
        this.getCost.addAndGet(getCost);
        this.hitKeyCount.addAndGet(hitCount);
        this.requestKeyCount.addAndGet(requestCount);
        this.getCount.incrementAndGet();
    }

    public void statsSet(long setCost) {
        this.setCost.addAndGet(setCost);
        this.setCount.incrementAndGet();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
