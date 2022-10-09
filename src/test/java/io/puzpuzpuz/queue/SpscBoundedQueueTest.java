package io.puzpuzpuz.queue;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class SpscBoundedQueueTest {

    @Test
    public void testSerial() {
        SpscBoundedQueue<Integer> queue = new SpscBoundedQueue<>(10);

        Assert.assertNull(queue.poll());

        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(queue.offer(i));
        }
        Assert.assertFalse(queue.offer(42));

        for (int i = 0; i < 10; i++) {
            Assert.assertEquals((Integer) i, queue.poll());
        }
        Assert.assertNull(queue.poll());
    }

    @Test
    public void testHammer() throws InterruptedException {
        final int iterations = 1_000_000;

        SpscBoundedQueue<Integer> queue = new SpscBoundedQueue<>(10);

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger anomalies = new AtomicInteger();

        ConsumerThread consumer = new ConsumerThread(queue, barrier, latch, anomalies, iterations);
        consumer.start();

        ProducerThread producer = new ProducerThread(queue, barrier, latch, anomalies, iterations);
        producer.start();

        latch.await();

        Assert.assertEquals(0, anomalies.get());
    }

    protected static class ConsumerThread extends Thread {

        private final SpscBoundedQueue<Integer> queue;
        private final CyclicBarrier barrier;
        private final CountDownLatch latch;
        private final AtomicInteger anomalies;
        private final int iterations;

        private ConsumerThread(
                SpscBoundedQueue<Integer> queue,
                CyclicBarrier barrier,
                CountDownLatch latch,
                AtomicInteger anomalies,
                int iterations
        ) {
            this.queue = queue;
            this.barrier = barrier;
            this.latch = latch;
            this.anomalies = anomalies;
            this.iterations = iterations;
        }

        @Override
        public void run() {
            try {
                barrier.await();
                int prev = -1;
                while (prev != iterations - 1) {
                    Integer element = queue.poll();
                    if (element == null) {
                        continue;
                    }
                    if (element != prev + 1) {
                        anomalies.incrementAndGet();
                    }
                    prev = element;
                }
            } catch (Exception e) {
                e.printStackTrace();
                anomalies.incrementAndGet();
            } finally {
                latch.countDown();
            }
        }
    }

    protected static class ProducerThread extends Thread {

        private final SpscBoundedQueue<Integer> queue;
        private final CyclicBarrier barrier;
        private final CountDownLatch latch;
        private final AtomicInteger anomalies;
        private final int iterations;

        private ProducerThread(
                SpscBoundedQueue<Integer> queue,
                CyclicBarrier barrier,
                CountDownLatch latch,
                AtomicInteger anomalies,
                int iterations
        ) {
            this.queue = queue;
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
                    while (!queue.offer(i)) {
                        // busy spin
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                anomalies.incrementAndGet();
            } finally {
                latch.countDown();
            }
        }
    }
}
