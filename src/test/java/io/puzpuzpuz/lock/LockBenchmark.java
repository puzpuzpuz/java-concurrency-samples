package io.puzpuzpuz.lock;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Threads(Threads.MAX)
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class LockBenchmark {

    private static final int NUM_WORK_SPINS = 10;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LockBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .measurementIterations(10)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void testBaseline(Blackhole bh) {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int sum = 0;
        for (int i = 0; i < NUM_WORK_SPINS; i++) {
            sum += rnd.nextInt();
        }
        bh.consume(sum);
    }

    @Benchmark
    public void testLock(BenchmarkState state, Blackhole bh) {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        state.lock.lock();
        // emulate some work
        int sum = 0;
        for (int i = 0; i < NUM_WORK_SPINS; i++) {
            sum += rnd.nextInt();
        }
        bh.consume(sum);
        state.lock.unlock();
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param({"JUC_UNFAIR", "JUC_FAIR", "SPIN_LOCK", "TICKET_SPIN_LOCK", "MCS_LOCK", "MCS_SPIN_LOCK"})
        public LockType type;
        public Lock lock;

        @Setup(Level.Trial)
        public void setUp() {
            switch (type) {
                case JUC_UNFAIR:
                    lock = new ReentrantLock();
                    break;
                case JUC_FAIR:
                    lock = new ReentrantLock(true);
                    break;
                case SPIN_LOCK:
                    lock = new SpinLock();
                    break;
                case TICKET_SPIN_LOCK:
                    lock = new TicketSpinLock();
                    break;
                case MCS_LOCK:
                    lock = new McsLock();
                    break;
                case MCS_SPIN_LOCK:
                    lock = new McsSpinLock();
                    break;
                default:
                    throw new IllegalStateException("unknown lock type: " + type);
            }
        }
    }

    public enum LockType {
        JUC_UNFAIR, JUC_FAIR, SPIN_LOCK, TICKET_SPIN_LOCK, MCS_LOCK, MCS_SPIN_LOCK
    }
}
