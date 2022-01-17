package io.puzpuzpuz.queue;

import java.util.concurrent.atomic.AtomicInteger;

public class SpscBoundedQueue<E> {

    private final Object[] data;
    private final PaddedAtomicInteger producerIdx = new PaddedAtomicInteger();
    private final PaddedAtomicInteger producerCachedIdx = new PaddedAtomicInteger();
    private final PaddedAtomicInteger consumerIdx = new PaddedAtomicInteger();
    private final PaddedAtomicInteger consumerCachedIdx = new PaddedAtomicInteger();

    public SpscBoundedQueue(int size) {
        this.data = new Object[size + 1];
    }

    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        final int idx = producerIdx.getPlain();
        int nextIdx = idx + 1;
        if (nextIdx == data.length) {
            nextIdx = 0;
        }
        int _consumerCachedIdx = consumerCachedIdx.getPlain();
        if (nextIdx == _consumerCachedIdx) {
            _consumerCachedIdx = consumerIdx.getAcquire();
            consumerCachedIdx.setPlain(_consumerCachedIdx);
            if (nextIdx == _consumerCachedIdx) {
                return false;
            }
        }
        data[idx] = e;
        producerIdx.setRelease(nextIdx);
        return true;
    }

    public E poll() {
        final int idx = consumerIdx.getPlain();
        int _producerCachedIdx = producerCachedIdx.getPlain();
        if (idx == _producerCachedIdx) {
            _producerCachedIdx = producerIdx.getAcquire();
            producerCachedIdx.setPlain(_producerCachedIdx);
            if (idx == _producerCachedIdx) {
                return null;
            }
        }
        final E element = (E) data[idx];
        data[idx] = null;
        int nextIdx = idx + 1;
        if (nextIdx == data.length) {
            nextIdx = 0;
        }
        consumerIdx.setRelease(nextIdx);
        return element;
    }

    static class PaddedAtomicInteger extends AtomicInteger {
        @SuppressWarnings("unused")
        private int i1, i2, i3, i4, i5, i6, i7, i8,
                i9, i10, i11, i12, i13, i14, i15;
    }
}
