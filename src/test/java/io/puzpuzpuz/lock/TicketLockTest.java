package io.puzpuzpuz.lock;

import org.junit.Test;

import java.util.concurrent.locks.Lock;

public class TicketLockTest extends AbstractLockTest {

    @Test
    public void testSerialLock() {
        final Lock lock = new TicketLock();
        for (int i = 0; i < 32; i++) {
            lock.lock();
            lock.unlock();
        }
    }

    @Test
    public void testHammerLock() throws Exception {
        final Lock lock = new TicketLock();
        testHammerLock(lock, 4, 1000);
    }
}
