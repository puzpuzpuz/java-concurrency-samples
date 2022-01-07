package io.puzpuzpuz.lock;

import org.junit.Test;

import java.util.concurrent.locks.Lock;

public class TicketCasSpinLockTest extends AbstractLockTest {

    @Test
    public void testSerialLock() {
        final Lock lock = new TicketSpinLock();
        for (int i = 0; i < 32; i++) {
            lock.lock();
            lock.unlock();
        }
    }

    @Test
    public void testHammerLock() throws Exception {
        final Lock lock = new TicketSpinLock();
        testHammerLock(lock, 4, 1000);
    }
}
