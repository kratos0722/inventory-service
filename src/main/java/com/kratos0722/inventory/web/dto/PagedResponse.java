package com.kratos0722.inventory.web.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A stable, explicit page envelope for list endpoints. Returning Spring's
 * {@code Page} directly is discouraged because its JSON shape is not a stable
 * contract; this record pins the fields clients depend on.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
