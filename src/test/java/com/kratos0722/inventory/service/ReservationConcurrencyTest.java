package com.kratos0722.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kratos0722.inventory.domain.Product;
import com.kratos0722.inventory.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Proves the reservation path cannot oversell under concurrency.
 *
 * <p>With only {@code STOCK} units available but {@code THREADS} reservers
 * racing for one unit each, exactly {@code STOCK} reservations must succeed and
 * the rest must fail with {@link InsufficientStockException}. Final stock must
 * be fully consumed and conserved (available + reserved unchanged in total).
 * There is intentionally no {@code @Transactional} here, so each reservation
 * commits independently — a real race.
 */
@SpringBootTest
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=20000")
class ReservationConcurrencyTest {

    private static final int STOCK = 50;
    private static final int THREADS = 200;

    @Autowired
    private InventoryService service;

    @Autowired
    private ProductRepository products;

    @Test
    void concurrentReservations_neverOversell() throws Exception {
        String sku = "CONC-" + System.nanoTime();
        service.createProduct(sku, "Widget", null, new BigDecimal("1.00"), STOCK);

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        var unexpected = new ConcurrentLinkedQueue<Throwable>();

        try {
            for (int i = 0; i < THREADS; i++) {
                final int n = i;
                Callable<Void> task = () -> {
                    start.await();
                    try {
                        service.reserve("race-" + n, sku, 1);
                        succeeded.incrementAndGet();
                    } catch (InsufficientStockException e) {
                        insufficient.incrementAndGet();
                    } catch (Throwable t) {
                        unexpected.add(t);
                    }
                    return null;
                };
                pool.submit(task);
            }
            start.countDown();
            pool.shutdown();
            boolean done = pool.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(true, done, "reservation tasks did not finish in time");
        } finally {
            pool.shutdownNow();
        }

        if (!unexpected.isEmpty()) {
            throw new AssertionError("Unexpected error during concurrent reservations: "
                    + unexpected.peek(), unexpected.peek());
        }

        assertEquals(STOCK, succeeded.get(), "exactly the available units should be reservable");
        assertEquals(THREADS - STOCK, insufficient.get(), "the rest should be rejected");

        Product p = service.getProduct(sku);
        assertEquals(0, p.getQuantityAvailable(), "all stock consumed");
        assertEquals(STOCK, p.getQuantityReserved(), "all consumed stock accounted as reserved");
    }
}
