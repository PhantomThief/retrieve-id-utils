/**
 * 
 */
package com.github.phantomthief.util;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.github.phantomthief.stats.n.counter.Duration;

/**
 * @author w.vela
 */
public class AccessCounter implements Duration {

    private final AtomicLong requestKeyCount = new AtomicLong();
    private final AtomicLong hitKeyCount = new AtomicLong();

    private final AtomicLong getCount = new AtomicLong();
    private final AtomicLong getCost = new AtomicLong();
    private final AtomicLong setCount = new AtomicLong();
    private final AtomicLong setCost = new AtomicLong();

    private long duration;;

    /**
     * @param duration
     */
    public AccessCounter(long duration) {
        this.duration = duration;
    }

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

    /* (non-Javadoc)
     * @see com.github.phantomthief.stats.n.counter.Duration#duration()
     */
    @Override
    public long duration() {
        return duration;
    }

    /* (non-Javadoc)
     * @see com.github.phantomthief.stats.n.counter.Duration#setDuration(long)
     */
    @Override
    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
