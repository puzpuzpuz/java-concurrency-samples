package io.puzpuzpuz.atomic;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public class AtomicLongTuple extends PaddedTuple {

    private static final VarHandle VH_VERSION;
    private static final VarHandle VH_X;
    private static final VarHandle VH_Y;
    private static final VarHandle VH_Z;

    private final TupleHolder writerHolder = new PaddedTupleHolder();
    // reader stats: x - total number of attempts, y - number of successful snapshots
    private final ThreadLocal<TupleHolder> readerStats = ThreadLocal.withInitial(PaddedTupleHolder::new);

    public void read(TupleHolder holder) {
        final TupleHolder stats = readerStats.get();
        for (;;) {
            stats.x++;
            final long version = (long) VH_VERSION.getAcquire(this);
            if ((version & 1) == 1) {
                // A write is in progress. Back off and keep spinning.
                LockSupport.parkNanos(1);
                continue;
            }

            // Read the tuple.
            holder.x = (long) VH_X.getOpaque(this);
            holder.y = (long) VH_Y.getOpaque(this);
            holder.z = (long) VH_Z.getOpaque(this);

            // We don't want the below load to bubble up, hence the fence.
            VarHandle.loadLoadFence();

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
                // Another write is in progress. Back off and keep spinning.
                LockSupport.parkNanos(1);
                continue;
            }

            // Try to update the version to an odd value (write intent).
            // We don't use compareAndExchangeRelease here to avoid an additional
            // full fence following this call.
            final long currentVersion = (long) VH_VERSION.compareAndExchange(this, version, version + 1);
            if (currentVersion != version) {
                // Someone else started writing. Back off and try again.
                LockSupport.parkNanos(10);
                continue;
            }

            // Apply the write.
            writerHolder.x = (long) VH_X.getOpaque(this);
            writerHolder.y = (long) VH_Y.getOpaque(this);
            writerHolder.z = (long) VH_Z.getOpaque(this);
            writer.accept(writerHolder);
            VH_X.setOpaque(this, writerHolder.x);
            VH_Y.setOpaque(this, writerHolder.y);
            VH_Z.setOpaque(this, writerHolder.z);

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
            VH_Z = MethodHandles.lookup().findVarHandle(AtomicLongTuple.class, "z", long.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class TupleHolder {
        public long x;
        public long y;
        public long z;
    }

    public static class PaddedTupleHolder extends TupleHolder {
        @SuppressWarnings("unused")
        private long l1, l2, l3, l4, l5;
    }
}

class Tuple {
    // version is also used as a TTAS spinlock to synchronize writers
    @SuppressWarnings("unused")
    long version;
    @SuppressWarnings("unused")
    long x;
    @SuppressWarnings("unused")
    long y;
    @SuppressWarnings("unused")
    long z;
}

class PaddedTuple extends Tuple {
    @SuppressWarnings("unused")
    private long l1, l2, l3, l4;
}
