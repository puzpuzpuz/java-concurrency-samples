package io.puzpuzpuz.lock;

import org.junit.Test;

import java.util.concurrent.locks.Lock;

public class BackoffCasSpinLockTest extends AbstractLockTest {

    @Test
    public void testSerialLock() {
        final Lock lock = new BackoffCasSpinLock();
        for (int i = 0; i < 32; i++) {
            lock.lock();
            lock.unlock();
        }
    }

    @Test
    public void testHammerLock() throws Exception {
        final Lock lock = new BackoffCasSpinLock();
        testHammerLock(lock, 4, 1000);
    }
}
