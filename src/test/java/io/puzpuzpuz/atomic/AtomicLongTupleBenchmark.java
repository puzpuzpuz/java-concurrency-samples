package io.puzpuzpuz.atomic;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@Threads(Threads.MAX)
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AtomicLongTupleBenchmark {

    private final AtomicLongTuple tuple = new AtomicLongTuple();
    private final ThreadLocal<AtomicLongTuple.TupleHolder> readerHolder = ThreadLocal.withInitial(AtomicLongTuple.TupleHolder::new);

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(AtomicLongTupleBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .measurementIterations(10)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void testBaseline(Blackhole bh) {
        AtomicLongTuple.TupleHolder holder = readerHolder.get();
        bh.consume(holder);
    }

    @Benchmark
    public void testReadOnly() {
        AtomicLongTuple.TupleHolder holder = readerHolder.get();
        tuple.read(holder);
    }
}
