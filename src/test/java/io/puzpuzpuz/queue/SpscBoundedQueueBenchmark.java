package io.puzpuzpuz.queue;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Group)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SpscBoundedQueueBenchmark {

    public static final String GROUP_NAME = "SpscBoundedQueue";
    public static final int OPS_PER_ITERATION = 1_000_000;

    private static final Object element = new Object();
    private final SpscBoundedQueue<Object> queue = new SpscBoundedQueue<>(1_000);

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SpscBoundedQueueBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .measurementIterations(3)
                .addProfiler("gc")
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    @Group(GROUP_NAME)
    @GroupThreads()
    public void write(Control control) {
        if (control.stopMeasurement) {
            return;
        }
        for (int i = 0; i < OPS_PER_ITERATION; i++) {
            while (!queue.offer(element)) {
                Thread.yield();
            }
        }
    }

    @Benchmark
    @Group(GROUP_NAME)
    @GroupThreads()
    public void read(Control control) {
        if (control.stopMeasurement) {
            return;
        }
        for (int i = 0; i < OPS_PER_ITERATION; i++) {
            while (queue.poll() == null) {
                Thread.yield();
            }
        }
    }
}
