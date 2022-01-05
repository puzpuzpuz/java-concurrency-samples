package io.puzpuzpuz.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

public class McsLock implements Lock {

    private final ThreadLocal<PaddedQueueNode> tlQNode = ThreadLocal.withInitial(PaddedQueueNode::new);
    private final AtomicReference<PaddedQueueNode> tailRef = new AtomicReference<>();

    @Override
    public void lock() {
        PaddedQueueNode qnode = tlQNode.get();
        PaddedQueueNode prev = tailRef.getAndSet(qnode);
        if (prev != null) {
            qnode.locked = 1;
            prev.next = qnode;
            while (qnode.locked == 1) {
                LockSupport.parkNanos(1);
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
        PaddedQueueNode qnode = tlQNode.get();
        if (qnode.next == null) {
            if (tailRef.compareAndSet(qnode, null)) {
                qnode.locked = 0;
                return;
            }
            while (qnode.next == null) {
                LockSupport.parkNanos(1);
            }
        }
        qnode.next.locked = 0;
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    private static class PaddedQueueNode extends QueueNode {
        @SuppressWarnings("unused")
        private long l1, l2, l3, l4, l5, l6;
    }

    private static abstract class QueueNode {
        volatile QueueNode next;
        volatile long locked;
    }
}
