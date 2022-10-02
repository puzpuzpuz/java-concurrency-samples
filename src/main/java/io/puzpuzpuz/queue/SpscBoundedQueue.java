package io.puzpuzpuz.queue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class SpscBoundedQueue<E> implements Queue<E> {

    private final Object[] data;
    private final PaddedAtomicInteger producerIdx = new PaddedAtomicInteger();
    private final PaddedAtomicInteger producerCachedIdx = new PaddedAtomicInteger();
    private final PaddedAtomicInteger consumerIdx = new PaddedAtomicInteger();
    private final PaddedAtomicInteger consumerCachedIdx = new PaddedAtomicInteger();

    public SpscBoundedQueue(int size) {
        this.data = new Object[size + 1];
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        final int idx = producerIdx.getOpaque();
        int nextIdx = idx + 1;
        if (nextIdx == data.length) {
            nextIdx = 0;
        }
        int cachedIdx = consumerCachedIdx.getPlain();
        if (nextIdx == cachedIdx) {
            cachedIdx = consumerIdx.getAcquire();
            consumerCachedIdx.setPlain(cachedIdx);
            if (nextIdx == cachedIdx) {
                return false;
            }
        }
        data[idx] = e;
        producerIdx.setRelease(nextIdx);
        return true;
    }

    @Override
    public E poll() {
        final int idx = consumerIdx.getOpaque();
        int cachedIdx = producerCachedIdx.getPlain();
        if (idx == cachedIdx) {
            cachedIdx = producerIdx.getAcquire();
            producerCachedIdx.setPlain(cachedIdx);
            if (idx == cachedIdx) {
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

    @Override
    public void clear() {
        Arrays.fill(data, null);
        producerIdx.set(0);
        producerCachedIdx.set(0);
        consumerIdx.set(0);
        consumerCachedIdx.set(0);
    }

    @Override
    public int size() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean isEmpty() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean contains(Object o) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Iterator<E> iterator() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Object[] toArray() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean add(E e) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean remove(Object o) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public E remove() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public E element() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public E peek() {
        throw new RuntimeException("not implemented");
    }

    static class PaddedAtomicInteger extends AtomicInteger {
        @SuppressWarnings("unused")
        private int i1, i2, i3, i4, i5, i6, i7, i8,
                i9, i10, i11, i12, i13, i14, i15;
    }
}
