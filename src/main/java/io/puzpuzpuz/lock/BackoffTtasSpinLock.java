package io.puzpuzpuz.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

public class BackoffTtasSpinLock implements Lock {

    private static final long MIN_DELAY = 1;
    private static final long MAX_DELAY = 16;

    private final AtomicBoolean lock = new AtomicBoolean();

    @Override
    public void lock() {
        long delay = MIN_DELAY;
        for (;;) {
            while (lock.get()) {}
            if (!lock.getAndSet(true)) {
                return;
            }
            LockSupport.parkNanos(delay);
            if (delay < MAX_DELAY) {
                delay *= 2;
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
        lock.set(false);
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }
}
