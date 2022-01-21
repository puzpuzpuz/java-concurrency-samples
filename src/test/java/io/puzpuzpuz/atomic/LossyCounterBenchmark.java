package io.puzpuzpuz.atomic;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Threads(Threads.MAX)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class LossyCounterBenchmark {

    private final AtomicLong counter = new AtomicLong();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LossyCounterBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .measurementIterations(10)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void testBaseline(Blackhole bh) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long inc = rnd.nextLong(1000);
        bh.consume(inc);
    }

    @Benchmark
    public void testLossyAcquireRelease() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long v = counter.getAcquire();
        long inc = rnd.nextLong(1000);
        counter.setRelease(v + inc);
    }

    @Benchmark
    public void testLossyDefault() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long v = counter.get();
        long inc = rnd.nextLong(1000);
        counter.set(v + inc);
    }

    @Benchmark
    public void testAtomicIncrement() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long inc = rnd.nextLong(1000);
        counter.addAndGet(inc);
    }

    @Benchmark
    public void testAtomicCas() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long inc = rnd.nextLong(1000);
        long v;
        do {
            v = counter.get();
        } while (!counter.compareAndSet(v, v + inc));
    }
}
