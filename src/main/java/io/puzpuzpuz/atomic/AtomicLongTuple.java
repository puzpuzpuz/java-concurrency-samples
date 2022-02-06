package io.puzpuzpuz.atomic;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public class AtomicLongTuple {

    private static final VarHandle VH_VERSION;
    private static final VarHandle VH_X;
    private static final VarHandle VH_Y;

    // version is also used as a TTAS spinlock to synchronize writers
    @SuppressWarnings("unused")
    private long version;
    @SuppressWarnings("unused")
    private long x;
    @SuppressWarnings("unused")
    private long y;

    private final TupleHolder writerHolder = new TupleHolder();
    // reader stats: x - total number of attempts, y - number of successful snapshots
    private final ThreadLocal<PaddedTupleHolder> readerStats = ThreadLocal.withInitial(PaddedTupleHolder::new);

    public void read(TupleHolder holder) {
        final PaddedTupleHolder stats = readerStats.get();
        for (;;) {
            stats.x++;
            final long version = (long) VH_VERSION.getAcquire(this);
            if ((version & 1) == 1) {
                // A write is in progress. Keep busy spinning.
                Thread.yield();
                continue;
            }

            // Read the tuple.
            holder.x = (long) VH_X.getAcquire(this);
            holder.y = (long) VH_Y.getAcquire(this);

            final long currentVersion = (long) VH_VERSION.getAcquire(this);
            if (currentVersion == version) {
                // The version didn't change, so the atomic snapshot succeeded.
                stats.y++;
                return;
            }
        }
    }

    public void write(Consumer<TupleHolder> writer) {
        for (;;) {
            final long version = (long) VH_VERSION.getAcquire(this);
            if ((version & 1) == 1) {
                // A write is in progress. Keep busy spinning.
                Thread.yield();
                continue;
            }

            // Try to update the version to an odd value (write intent).
            final long currentVersion = (long) VH_VERSION.compareAndExchangeAcquire(this, version, version + 1);
            if (currentVersion != version) {
                // Someone else started writing. Back off and try again.
                LockSupport.parkNanos(10);
                continue;
            }

            // Apply the write.
            writerHolder.x = (long) VH_X.getAcquire(this);
            writerHolder.y = (long) VH_Y.getAcquire(this);
            writer.accept(writerHolder);
            VH_X.setRelease(this, writerHolder.x);
            VH_Y.setRelease(this, writerHolder.y);

            // Update the version to an even value (write finished).
            VH_VERSION.setRelease(this, version + 2);
            return;
        }
    }

    TupleHolder readerStats() {
        return readerStats.get();
    }

    static {
        try {
            VH_VERSION = MethodHandles.lookup().findVarHandle(AtomicLongTuple.class, "version", long.class);
            VH_X = MethodHandles.lookup().findVarHandle(AtomicLongTuple.class, "x", long.class);
            VH_Y = MethodHandles.lookup().findVarHandle(AtomicLongTuple.class, "y", long.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
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
