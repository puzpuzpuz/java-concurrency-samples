package io.puzpuzpuz.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

public class McsSpinLock implements Lock {

    private final ThreadLocal<PaddedNode> tlNode = ThreadLocal.withInitial(PaddedNode::new);
    private final AtomicReference<PaddedNode> tailRef = new AtomicReference<>();

    @Override
    public void lock() {
        final PaddedNode localNode = tlNode.get();
        final PaddedNode prevNode = tailRef.getAndSet(localNode);
        if (prevNode != null) {
            localNode.locked = 1;
            prevNode.next = localNode;
            while (localNode.locked == 1) {
                LockSupport.parkNanos(10);
            }
        }
    }

    @Override
    public void lockInterruptibly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlock() {
        final PaddedNode localNode = tlNode.get();
        if (localNode.next == null) {
            if (tailRef.compareAndSet(localNode, null)) {
                return;
            }
            while (localNode.next == null) {
                LockSupport.parkNanos(1);
            }
        }
        localNode.next.locked = 0;
        localNode.next = null;
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    private static class PaddedNode extends Node {
        @SuppressWarnings("unused")
        private long l1, l2, l3, l4, l5, l6;
    }

    private static abstract class Node {
        volatile Node next;
        volatile long locked;
    }
}
