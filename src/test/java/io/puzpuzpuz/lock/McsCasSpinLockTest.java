package io.puzpuzpuz.lock;

import org.junit.Test;

import java.util.concurrent.locks.Lock;

public class McsCasSpinLockTest extends AbstractLockTest {

    @Test
    public void testSerialLock() {
        final Lock lock = new McsSpinLock();
        for (int i = 0; i < 32; i++) {
            lock.lock();
            lock.unlock();
        }
    }

    @Test
    public void testHammerLock() throws Exception {
        final Lock lock = new McsSpinLock();
        testHammerLock(lock, 4, 10_000);
    }
}
