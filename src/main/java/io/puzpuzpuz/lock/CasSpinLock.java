package io.puzpuzpuz.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class CasSpinLock implements Lock {

    private final AtomicBoolean lock = new AtomicBoolean();

    @Override
    public void lock() {
        while (!lock.compareAndSet(false, true)) {}
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
