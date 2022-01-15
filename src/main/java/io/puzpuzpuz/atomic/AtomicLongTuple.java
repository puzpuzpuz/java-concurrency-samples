package io.puzpuzpuz.atomic;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public class AtomicLongTuple {

    // [version, x, y] array
    // version is also used as a TTAS spinlock to synchronize writers
    private final AtomicLongArray array = new AtomicLongArray(3);
    private final TupleHolder writerHolder = new TupleHolder();
    // reader stats: x - total number of attempts, y - number of successful snapshots
    private final ThreadLocal<PaddedTupleHolder> readerStats = ThreadLocal.withInitial(PaddedTupleHolder::new);

    public void read(TupleHolder holder) {
        final PaddedTupleHolder stats = readerStats.get();
        for (;;) {
            stats.x++;
            final long version = array.getAcquire(0);
            if ((version & 1) == 1) {
                // A write is in progress. Keep busy spinning.
                Thread.yield();
                continue;
            }

            // Read the tuple.
            holder.x = array.getAcquire(1);
            holder.y = array.getAcquire(2);

            if (array.getAcquire(0) == version) {
                // The version didn't change, so the atomic snapshot succeeded.
                stats.y++;
                return;
            }
        }
    }

    public void write(Consumer<TupleHolder> writer) {
        for (;;) {
            final long version = array.getAcquire(0);
            if ((version & 1) == 1) {
                // A write is in progress. Keep busy spinning.
                Thread.yield();
                continue;
            }

            // Try to update the version to an odd value (write intent).
            if (array.compareAndExchangeRelease(0, version, version + 1) != version) {
                // Someone else started writing. Back off and try again.
                LockSupport.parkNanos(10);
                continue;
            }

            // Apply the write.
            writerHolder.x = array.getPlain(1);
            writerHolder.y = array.getPlain(2);
            writer.accept(writerHolder);
            array.setRelease(1, writerHolder.x);
            array.setRelease(2, writerHolder.y);

            // Update the version to an even value (write finished).
            array.setRelease(0, version + 2);
            return;
        }
    }

    TupleHolder readerStats() {
        return readerStats.get();
    }

    public static class TupleHolder {
        public long x;
        public long y;
    }

    static class PaddedTupleHolder extends TupleHolder {
        @SuppressWarnings("unused")
        private long l1, l2, l3, l4, l5, l6;
    }
}
