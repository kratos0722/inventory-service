package com.kratos0722.inventory.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for reserving stock. */
public record ReserveRequest(
        @NotBlank @Size(max = 128) String idempotencyKey,
        @NotBlank @Size(max = 64) String sku,
        @Min(1) int quantity) {
}
