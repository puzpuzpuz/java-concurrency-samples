package io.puzpuzpuz.map;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Threads(Threads.MAX)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class ConcurrentMapBenchmark {

    private static final int SIZE = 1_000;

    @Param({"99", "75"})
    public int readPercentage;
    private final ConcurrentHashMap<Long, Long> map = new ConcurrentHashMap<>();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ConcurrentMapBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .measurementIterations(10)
                .forks(1)
                .addProfiler(GCProfiler.class)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setUp() {
        map.clear();
        for (long i = 0; i < SIZE; i++) {
            map.put(i, i);
        }
    }

    @Benchmark
    public void testMap(Blackhole bh) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long key = rnd.nextLong(SIZE);
        int storeThreshold = 10 * readPercentage;
        int deleteThreshold = 10 * readPercentage + ((1000 - 10 * readPercentage) / 2);
        int op = rnd.nextInt(1000);
        if (op >= deleteThreshold) {
            map.remove(key);
        } else if (op >= storeThreshold) {
            map.put(key, key);
        } else {
            bh.consume(map.get(key));
        }
    }
}
