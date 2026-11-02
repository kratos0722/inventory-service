package com.kratos0722.inventory.web.dto;

import com.kratos0722.inventory.domain.Product;
import java.math.BigDecimal;

/** API view of a {@link Product}. */
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal unitPrice,
        int quantityAvailable,
        int quantityReserved) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getDescription(),
                p.getUnitPrice(),
                p.getQuantityAvailable(),
                p.getQuantityReserved());
    }
}
