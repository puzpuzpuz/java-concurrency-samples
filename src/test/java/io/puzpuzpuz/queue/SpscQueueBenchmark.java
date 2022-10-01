package io.puzpuzpuz.queue;

import org.jctools.queues.SpscArrayQueue;
import org.jctools.queues.atomic.SpscAtomicArrayQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.Control;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@State(Scope.Group)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SpscQueueBenchmark {

    public static final int OPS_PER_ITERATION = 1_000_000;
    public static final int QUEUE_CAPACITY = 1_000;

    private static final Object element = new Object();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SpscQueueBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .measurementIterations(3)
                .addProfiler("gc")
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    @Group()
    @GroupThreads()
    public void write(BenchmarkState state, Control control) {
        if (control.stopMeasurement) {
            return;
        }
        for (int i = 0; i < OPS_PER_ITERATION; i++) {
            while (!state.queue.offer(element)) {
                LockSupport.parkNanos(1);
            }
            Blackhole.consumeCPU(10);
        }
    }

    @Benchmark
    @Group()
    @GroupThreads()
    public void read(BenchmarkState state, Control control) {
        if (control.stopMeasurement) {
            return;
        }
        for (int i = 0; i < OPS_PER_ITERATION; i++) {
            while (state.queue.poll() == null) {
                LockSupport.parkNanos(1);
            }
            Blackhole.consumeCPU(10);
        }
    }

    @State(Scope.Benchmark)
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
    }

    public enum QueueType {
        SPSC_QUEUE, ARRAY_BLOCKING_QUEUE, JCTOOLS_ATOMIC_QUEUE, JCTOOLS_QUEUE
    }
}
