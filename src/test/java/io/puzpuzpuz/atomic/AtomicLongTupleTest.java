package io.puzpuzpuz.atomic;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class AtomicLongTupleTest {

    @Test
    public void testSerial() {
        AtomicLongTuple tuple = new AtomicLongTuple();
        AtomicLongTuple.TupleHolder holder = new AtomicLongTuple.TupleHolder();

        tuple.read(holder);
        Assert.assertEquals(0, holder.x);
        Assert.assertEquals(0, holder.y);

        tuple.write(h -> {
            h.x = 1;
            h.y = 2;
        });

        tuple.read(holder);
        Assert.assertEquals(1, holder.x);
        Assert.assertEquals(2, holder.y);
    }

    @Test
    public void testHammerSingleWriter() throws InterruptedException {
        testHammer(4, 1, 10_000);
    }

    @Test
    public void testHammerMultipleWriters() throws InterruptedException {
        testHammer(4, 2, 10_000);
    }

    private void testHammer(int readers, int writers, int iterations) throws InterruptedException {
        AtomicLongTuple tuple = new AtomicLongTuple();

        CyclicBarrier barrier = new CyclicBarrier(readers + writers);
        CountDownLatch latch = new CountDownLatch(readers + writers);
        AtomicInteger anomalies = new AtomicInteger();

        for (int i = 0; i < readers; i++) {
            ReaderThread reader = new ReaderThread(tuple, barrier, latch, anomalies, iterations);
            reader.start();
        }

        for (int i = 0; i < writers; i++) {
            WriterThread writer = new WriterThread(tuple, barrier, latch, anomalies, iterations);
            writer.start();
        }

        latch.await();

        Assert.assertEquals(0, anomalies.get());
    }

    protected static class ReaderThread extends Thread {

        private final AtomicLongTuple tuple;
        private final CyclicBarrier barrier;
        private final CountDownLatch latch;
        private final AtomicInteger anomalies;
        private final int iterations;
        private final AtomicLongTuple.TupleHolder holder = new AtomicLongTuple.TupleHolder();

        private ReaderThread(AtomicLongTuple tuple, CyclicBarrier barrier, CountDownLatch latch, AtomicInteger anomalies, int iterations) {
            this.tuple = tuple;
            this.barrier = barrier;
            this.latch = latch;
            this.anomalies = anomalies;
            this.iterations = iterations;
        }

        @Override
        public void run() {
            try {
                barrier.await();
                long prevX = 0;
                long prevY = 0;
                while (prevX != iterations) {
                    tuple.read(holder);
                    if (holder.x != holder.y || holder.x < prevX || holder.y < prevY) {
                        anomalies.incrementAndGet();
                    }
                    prevX = holder.x;
                    prevY = holder.y;
                    LockSupport.parkNanos(1);
                }
                AtomicLongTuple.TupleHolder stats = tuple.readerStats();
                System.out.println("reader [threadId=" + Thread.currentThread().getId() + "] stats: " +
                        "total attempts=" + stats.x + ", successes=" + stats.y + ", ratio=" + ((double) stats.y / stats.x));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }
    }

    protected static class WriterThread extends Thread {

        private final AtomicLongTuple tuple;
        private final CyclicBarrier barrier;
        private final CountDownLatch latch;
        private final AtomicInteger anomalies;
        private final int iterations;

        private WriterThread(AtomicLongTuple tuple, CyclicBarrier barrier, CountDownLatch latch, AtomicInteger anomalies, int iterations) {
            this.tuple = tuple;
            this.barrier = barrier;
            this.latch = latch;
            this.anomalies = anomalies;
            this.iterations = iterations;
        }

        @Override
        public void run() {
            try {
                barrier.await();
                for (int i = 0; i < iterations; i++) {
                    final int expectedValue = i;
                    tuple.write(h -> {
                        if (h.x < expectedValue) {
                            // Another writer overwrote our value.
                            anomalies.incrementAndGet();
                        }
                        if (h.x == expectedValue) {
                            h.x++;
                            h.y++;
                        }
                    });
                    LockSupport.parkNanos(50);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }
    }
}
