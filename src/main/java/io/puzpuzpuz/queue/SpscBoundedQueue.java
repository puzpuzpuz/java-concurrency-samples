package io.puzpuzpuz.queue;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class SpscBoundedQueue<E> {

    private static final int INTS_PER_CACHE_LINE = 16;
    private static final int CONSUMER_INDEX = 0;
    private static final int CONSUMER_CACHED_INDEX = INTS_PER_CACHE_LINE;
    private static final int PRODUCER_INDEX = 2 * INTS_PER_CACHE_LINE;
    private static final int PRODUCER_CACHED_INDEX = 3 * INTS_PER_CACHE_LINE;

    private final Object[] data;
    private final AtomicIntegerArray indexes = new AtomicIntegerArray(4 * INTS_PER_CACHE_LINE);

    public SpscBoundedQueue(int size) {
        this.data = new Object[size + 1];
    }

    public boolean offer(E e) {
        final int idx = indexes.getOpaque(PRODUCER_INDEX);
        int nextIdx = idx + 1;
        if (nextIdx == data.length) {
            nextIdx = 0;
        }
        int consumerCachedIdx = indexes.getPlain(CONSUMER_CACHED_INDEX);
        if (nextIdx == consumerCachedIdx) {
            consumerCachedIdx = indexes.getAcquire(CONSUMER_INDEX);
            indexes.setPlain(CONSUMER_CACHED_INDEX, consumerCachedIdx);
            if (nextIdx == consumerCachedIdx) {
                return false;
            }
        }
        data[idx] = e;
        indexes.setRelease(PRODUCER_INDEX, nextIdx);
        return true;
    }

    public E poll() {
        final int idx = indexes.getOpaque(CONSUMER_INDEX);
        int producerCachedIdx = indexes.getPlain(PRODUCER_CACHED_INDEX);
        if (idx == producerCachedIdx) {
            producerCachedIdx = indexes.getAcquire(PRODUCER_INDEX);
            indexes.setPlain(PRODUCER_CACHED_INDEX, producerCachedIdx);
            if (idx == producerCachedIdx) {
                return null;
            }
        }
        final E element = (E) data[idx];
        data[idx] = null;
        int nextIdx = idx + 1;
        if (nextIdx == data.length) {
            nextIdx = 0;
        }
        indexes.setRelease(CONSUMER_INDEX, nextIdx);
        return element;
    }
}
