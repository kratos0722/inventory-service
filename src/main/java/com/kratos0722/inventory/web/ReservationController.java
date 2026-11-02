package com.kratos0722.inventory.web;

import com.kratos0722.inventory.domain.Reservation;
import com.kratos0722.inventory.service.InventoryService;
import com.kratos0722.inventory.web.dto.ReservationResponse;
import com.kratos0722.inventory.web.dto.ReserveRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final InventoryService service;

    public ReservationController(InventoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveRequest request) {
        Reservation reservation = service.reserve(
                request.idempotencyKey(),
                request.sku(),
                request.quantity());
        return ResponseEntity
                .created(URI.create("/api/v1/reservations/" + reservation.getId()))
                .body(ReservationResponse.from(reservation));
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable Long id) {
        return ReservationResponse.from(service.getReservation(id));
    }

    @PostMapping("/{id}/confirm")
    public ReservationResponse confirm(@PathVariable Long id) {
        return ReservationResponse.from(service.confirm(id));
    }

    @PostMapping("/{id}/release")
    public ReservationResponse release(@PathVariable Long id) {
        return ReservationResponse.from(service.release(id));
    }
}
