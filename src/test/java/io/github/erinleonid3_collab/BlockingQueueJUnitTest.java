package io.github.erinleonid3_collab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class BlockingQueueJUnitTest {
    @Test
    void fifoOrder() throws InterruptedException {
        BlockingQueue<Integer> q = new BlockingQueue<>(5);
        q.enqueue(1);
        q.enqueue(2);
        q.enqueue(3);
        assertEquals(1, q.dequeue());
        assertEquals(2, q.dequeue());
        assertEquals(3, q.dequeue());
        assertEquals(0, q.size());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void enqueueBlocksWhenFull() throws Exception {
        BlockingQueue<Integer> q = new BlockingQueue<>(1);
        q.enqueue(1);

        Thread producer = new Thread(() -> {
            try {
                q.enqueue(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.setDaemon(true);
        producer.start();

        Thread.sleep(150);
        assertTrue(producer.isAlive());

        q.dequeue();
        producer.join(1000);
        assertFalse(producer.isAlive());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void dequeueBlocksWhenEmpty() throws Exception {
        BlockingQueue<Integer> q = new BlockingQueue<>(2);

        Thread consumer = new Thread(() -> {
            try {
                q.dequeue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.setDaemon(true);
        consumer.start();

        Thread.sleep(150);
        assertTrue(consumer.isAlive());

        q.enqueue(42);
        consumer.join(1000);
        assertFalse(consumer.isAlive());
    }

    @Test
    void rejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new BlockingQueue<>(0));
        assertThrows(IllegalArgumentException.class, () -> new BlockingQueue<>(-1));
    }

    @Test
    void rejectsNullItem() {
        BlockingQueue<Integer> q = new BlockingQueue<>(1);
        assertThrows(NullPointerException.class, () -> q.enqueue(null));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void noLostItemsUnderContention() throws Exception {
        final int PRODUCERS = 4, CONSUMERS = 4, PER_PRODUCER = 500, TOTAL = PRODUCERS * PER_PRODUCER;
        BlockingQueue<Integer> q = new BlockingQueue<>(10);
        AtomicInteger consumed = new AtomicInteger();

        for (int i = 0; i < CONSUMERS; i++) {
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        q.dequeue();
                        consumed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            t.setDaemon(true);
            t.start();
        }

        Thread[] producers = new Thread[PRODUCERS];
        for (int p = 0; p < PRODUCERS; p++) {
            producers[p] = new Thread(() -> {
                try {
                    for (int i = 0; i < PER_PRODUCER; i++) q.enqueue(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            producers[p].start();
        }
        for (Thread t : producers) t.join();

        long deadline = System.currentTimeMillis() + 5000;
        while (consumed.get() < TOTAL && System.currentTimeMillis() < deadline) Thread.sleep(10);
        assertEquals(TOTAL, consumed.get());
    }
}
