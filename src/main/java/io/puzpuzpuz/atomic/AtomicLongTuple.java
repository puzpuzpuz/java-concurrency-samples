package io.puzpuzpuz.atomic;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public class AtomicLongTuple {

    // [version, x, y] array
    // version is also used as a TTAS spinlock to synchronize writers
    private final AtomicLongArray data = new AtomicLongArray(3);
    private final TupleHolder writerHolder = new TupleHolder();
    // reader stats: x - total number of attempts, y - number of successful snapshots
    private final ThreadLocal<PaddedTupleHolder> readerStats = ThreadLocal.withInitial(PaddedTupleHolder::new);

    public void read(TupleHolder holder) {
        final PaddedTupleHolder stats = readerStats.get();
        for (;;) {
            stats.x++;
            final long version = data.getAcquire(0);
            if ((version & 1) == 1) {
                // A write is in progress. Keep busy spinning.
                Thread.yield();
                continue;
            }

            // Read the tuple.
            holder.x = data.getAcquire(1);
            holder.y = data.getAcquire(2);

            if (data.getAcquire(0) == version) {
                // The version didn't change, so the atomic snapshot succeeded.
                stats.y++;
                return;
            }
        }
    }

    public void write(Consumer<TupleHolder> writer) {
        for (;;) {
            final long version = data.getOpaque(0);
            if ((version & 1) == 1) {
                // A write is in progress. Keep busy spinning.
                Thread.yield();
                continue;
            }

            // Try to update the version to an odd value (write intent).
            if (data.compareAndExchangeAcquire(0, version, version + 1) != version) {
                // Someone else started writing. Back off and try again.
                LockSupport.parkNanos(10);
                continue;
            }

            // Apply the write.
            writerHolder.x = data.getPlain(1);
            writerHolder.y = data.getPlain(2);
            writer.accept(writerHolder);
            data.setRelease(1, writerHolder.x);
            data.setRelease(2, writerHolder.y);

            // Update the version to an even value (write finished).
            data.setRelease(0, version + 2);
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
