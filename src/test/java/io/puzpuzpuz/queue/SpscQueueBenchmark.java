package io.puzpuzpuz.queue;

import org.jctools.queues.SpscArrayQueue;
import org.jctools.queues.atomic.SpscAtomicArrayQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

@State(Scope.Group)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SpscQueueBenchmark {

    public static final int QUEUE_CAPACITY = 1_000;

    private static final Object element = new Object();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SpscQueueBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .warmupTime(TimeValue.seconds(3))
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(3))
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @AuxCounters
    @State(Scope.Thread)
    public static class PollCounters {
        public long pollsFailed;
        public long pollsMade;
    }

    @AuxCounters
    @State(Scope.Thread)
    public static class OfferCounters {
        public long offersFailed;
        public long offersMade;
    }

    @Benchmark
    @Group()
    @GroupThreads()
    public void write(BenchmarkState state, OfferCounters counters) {
        if (!state.queue.offer(element)) {
            counters.offersFailed++;
            Thread.yield();
        } else {
            counters.offersMade++;
            Blackhole.consumeCPU(10);
        }
    }

    @Benchmark
    @Group()
    @GroupThreads()
    public void read(BenchmarkState state, PollCounters counters) {
        if (state.queue.poll() == null) {
            counters.pollsFailed++;
            Thread.yield();
        } else {
            counters.pollsMade++;
            Blackhole.consumeCPU(10);
        }
    }

    @State(Scope.Group)
    public static class BenchmarkState {

        @Param({
                "SPSC_QUEUE", "ARRAY_BLOCKING_QUEUE", "JCTOOLS_QUEUE", "JCTOOLS_ATOMIC_QUEUE"
        })
        public QueueType type;
        public Queue<Object> queue;

        @Setup(Level.Trial)
        public void setUp() {
            switch (type) {
                case SPSC_QUEUE:
                    queue = new SpscBoundedQueue<>(QUEUE_CAPACITY);
                    break;
                case ARRAY_BLOCKING_QUEUE:
                    queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
                    break;
                case JCTOOLS_QUEUE:
                    queue = new SpscArrayQueue<>(QUEUE_CAPACITY);
                    break;
                case JCTOOLS_ATOMIC_QUEUE:
                    queue = new SpscAtomicArrayQueue<>(QUEUE_CAPACITY);
                    break;
                default:
                    throw new IllegalStateException("unknown queue type: " + type);
            }
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            synchronized (queue) {
                queue.clear();
            }
        }
    }

    public enum QueueType {
        SPSC_QUEUE, ARRAY_BLOCKING_QUEUE, JCTOOLS_ATOMIC_QUEUE, JCTOOLS_QUEUE
    }
}
