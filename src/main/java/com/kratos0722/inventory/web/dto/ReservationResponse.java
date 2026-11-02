package com.kratos0722.inventory.web.dto;

import com.kratos0722.inventory.domain.Reservation;
import java.time.Instant;

/** API view of a {@link Reservation}. */
public record ReservationResponse(
        Long id,
        String idempotencyKey,
        String sku,
        int quantity,
        String status,
        Instant createdAt) {

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getIdempotencyKey(),
                r.getSku(),
                r.getQuantity(),
                r.getStatus().name(),
                r.getCreatedAt());
    }
}
