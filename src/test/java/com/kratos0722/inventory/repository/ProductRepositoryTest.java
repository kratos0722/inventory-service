package com.kratos0722.inventory.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kratos0722.inventory.domain.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Verifies the guarded atomic reservation update at the persistence layer. */
@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository products;

    private Long persist(String sku, int qty) {
        Product p = products.saveAndFlush(new Product(sku, "Widget", null, new BigDecimal("1.00"), qty));
        return p.getId();
    }

    @Test
    void reserveStock_withEnough_appliesAndMovesQuantity() {
        Long id = persist("R-1", 10);

        int applied = products.reserveStock(id, 4);

        assertEquals(1, applied);
        Product p = products.findById(id).orElseThrow();
        assertEquals(6, p.getQuantityAvailable());
        assertEquals(4, p.getQuantityReserved());
    }

    @Test
    void reserveStock_withoutEnough_isRejectedAndLeavesStockUntouched() {
        Long id = persist("R-2", 3);

        int applied = products.reserveStock(id, 5);

        assertEquals(0, applied);
        Product p = products.findById(id).orElseThrow();
        assertEquals(3, p.getQuantityAvailable());
        assertEquals(0, p.getQuantityReserved());
    }

    @Test
    void reserveStock_exactlyAllRemaining_isAllowed() {
        Long id = persist("R-3", 5);

        assertEquals(1, products.reserveStock(id, 5));
        Product p = products.findById(id).orElseThrow();
        assertEquals(0, p.getQuantityAvailable());
        assertEquals(5, p.getQuantityReserved());
    }
}
