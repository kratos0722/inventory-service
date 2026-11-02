package com.kratos0722.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kratos0722.inventory.domain.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductPaginationTest {

    @Autowired
    private InventoryService service;

    @Test
    void lists_products_in_pages() {
        for (int i = 0; i < 5; i++) {
            service.createProduct("PAGE-" + i, "Widget " + i, null, new BigDecimal("1.00"), 1);
        }

        Page<Product> first = service.listProducts(PageRequest.of(0, 2));
        assertEquals(2, first.getContent().size());
        assertEquals(5, first.getTotalElements());
        assertEquals(3, first.getTotalPages());
        assertEquals(0, first.getNumber());

        Page<Product> last = service.listProducts(PageRequest.of(2, 2));
        assertEquals(1, last.getContent().size(), "the third page holds the remaining item");
    }
}
