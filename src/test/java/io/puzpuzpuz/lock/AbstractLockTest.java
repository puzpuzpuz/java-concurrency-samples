package io.puzpuzpuz.lock;

import org.junit.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

abstract class AbstractLockTest {

    protected void testHammerLock(Lock lock, int threads, int iterations) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger anomalies = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            LockerThread locker = new LockerThread(lock, barrier, latch, anomalies, iterations);
            locker.start();
        }

        latch.await();

        Assert.assertEquals(0, anomalies.get());
    }

    protected static class LockerThread extends Thread {

        private final Lock lock;
        private final CyclicBarrier barrier;
        private final CountDownLatch latch;
        private final AtomicInteger anomalies;
        private final int iterations;

        private LockerThread(Lock lock, CyclicBarrier barrier, CountDownLatch latch, AtomicInteger anomalies, int iterations) {
            this.lock = lock;
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
                    lock.lock();
                    try {
                        int n = anomalies.incrementAndGet();
                        if (n != 1) {
                            throw new IllegalStateException("anomalies value: " + n);
                        }
                        LockSupport.parkNanos(10);
                        anomalies.decrementAndGet();
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }
    }
}

