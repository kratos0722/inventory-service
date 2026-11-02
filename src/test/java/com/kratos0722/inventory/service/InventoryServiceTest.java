package com.kratos0722.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kratos0722.inventory.domain.Product;
import com.kratos0722.inventory.domain.Reservation;
import com.kratos0722.inventory.domain.ReservationStatus;
import com.kratos0722.inventory.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Exercises the service logic against a real (H2) persistence layer. */
@SpringBootTest
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService service;

    @Autowired
    private ProductRepository products;

    private Product newProduct(String sku, int qty) {
        return service.createProduct(sku, "Widget", "A widget", new BigDecimal("9.99"), qty);
    }

    @Test
    void createProduct_persists() {
        Product p = newProduct("SKU-1", 10);
        assertEquals("SKU-1", products.findBySku("SKU-1").orElseThrow().getSku());
        assertEquals(10, p.getQuantityAvailable());
        assertEquals(0, p.getQuantityReserved());
    }

    @Test
    void createProduct_duplicateSku_throws() {
        newProduct("SKU-DUP", 5);
        assertThrows(DuplicateSkuException.class, () -> newProduct("SKU-DUP", 3));
    }

    @Test
    void reserve_decrementsAvailable_andCreatesReservation() {
        newProduct("SKU-2", 10);
        Reservation r = service.reserve("key-1", "SKU-2", 4);

        assertEquals(ReservationStatus.RESERVED, r.getStatus());
        assertEquals(4, r.getQuantity());

        Product p = service.getProduct("SKU-2");
        assertEquals(6, p.getQuantityAvailable());
        assertEquals(4, p.getQuantityReserved());
    }

    @Test
    void reserve_insufficientStock_throws() {
        newProduct("SKU-3", 2);
        assertThrows(InsufficientStockException.class, () -> service.reserve("key-2", "SKU-3", 5));

        Product p = service.getProduct("SKU-3");
        assertEquals(2, p.getQuantityAvailable());
        assertEquals(0, p.getQuantityReserved());
    }

    @Test
    void reserve_isIdempotentOnKey() {
        newProduct("SKU-4", 10);
        Reservation first = service.reserve("same-key", "SKU-4", 3);
        Reservation replay = service.reserve("same-key", "SKU-4", 3);

        assertEquals(first.getId(), replay.getId());
        // Stock was only taken once.
        assertEquals(7, service.getProduct("SKU-4").getQuantityAvailable());
    }

    @Test
    void reserve_unknownSku_throws() {
        assertThrows(ProductNotFoundException.class, () -> service.reserve("key-x", "MISSING", 1));
    }

    @Test
    void confirm_consumesReservedStock() {
        newProduct("SKU-5", 10);
        Reservation r = service.reserve("key-c", "SKU-5", 4);

        Reservation confirmed = service.confirm(r.getId());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());

        Product p = service.getProduct("SKU-5");
        assertEquals(6, p.getQuantityAvailable());
        assertEquals(0, p.getQuantityReserved());
    }

    @Test
    void release_returnsStockToAvailable() {
        newProduct("SKU-6", 10);
        Reservation r = service.reserve("key-r", "SKU-6", 4);

        Reservation released = service.release(r.getId());
        assertEquals(ReservationStatus.RELEASED, released.getStatus());

        Product p = service.getProduct("SKU-6");
        assertEquals(10, p.getQuantityAvailable());
        assertEquals(0, p.getQuantityReserved());
    }

    @Test
    void confirm_twice_isRejected() {
        newProduct("SKU-7", 10);
        Reservation r = service.reserve("key-cc", "SKU-7", 2);
        service.confirm(r.getId());
        assertThrows(IllegalReservationStateException.class, () -> service.confirm(r.getId()));
    }

    @Test
    void differentKeys_areDistinctReservations() {
        newProduct("SKU-8", 10);
        Reservation a = service.reserve("k-a", "SKU-8", 1);
        Reservation b = service.reserve("k-b", "SKU-8", 1);
        assertNotEquals(a.getId(), b.getId());
        assertEquals(8, service.getProduct("SKU-8").getQuantityAvailable());
    }
}
