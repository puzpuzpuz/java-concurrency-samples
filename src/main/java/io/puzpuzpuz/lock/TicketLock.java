package io.puzpuzpuz.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

public class TicketLock implements Lock {

    private final PaddedAtomicLong ticketSeq = new PaddedAtomicLong();
    private final PaddedAtomicLong servedTicket = new PaddedAtomicLong();

    @Override
    public void lock() {
        final long ticket = ticketSeq.incrementAndGet();
        for (;;) {
            final long served = servedTicket.get();
            final long queueSize = ticket - served - 1;
            if (queueSize == 0) {
                break;
            }
            if (queueSize < 0) {
                throw new IllegalStateException("unlock was called without prior locking: " + queueSize);
            }
            LockSupport.parkNanos(10 * queueSize);
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
        servedTicket.incrementAndGet();
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    private static class PaddedAtomicLong extends AtomicLong {
        @SuppressWarnings("unused")
        private long l1, l2, l3, l4, l5, l6, l7;
    }
}
