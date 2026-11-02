package com.kratos0722.inventory.service;

import com.kratos0722.inventory.domain.Product;
import com.kratos0722.inventory.domain.Reservation;
import com.kratos0722.inventory.domain.ReservationStatus;
import com.kratos0722.inventory.repository.ProductRepository;
import com.kratos0722.inventory.repository.ReservationRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for products and stock reservations.
 *
 * <p>The reservation flow is deliberately concurrency-safe: instead of reading
 * stock, deciding in Java, and writing back (which races under load), it issues
 * a single guarded SQL update ({@link ProductRepository#reserveStock}) that the
 * database serializes. Combined with the unique idempotency key on
 * {@link Reservation}, a retried or duplicated request can never hold stock
 * twice, and concurrent requests can never oversell.
 */
@Service
public class InventoryService {

    private final ProductRepository products;
    private final ReservationRepository reservations;

    public InventoryService(ProductRepository products, ReservationRepository reservations) {
        this.products = products;
        this.reservations = reservations;
    }

    @Transactional
    public Product createProduct(String sku, String name, String description, BigDecimal unitPrice, int quantity) {
        if (products.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }
        return products.save(new Product(sku, name, description, unitPrice, quantity));
    }

    @Transactional(readOnly = true)
    public Product getProduct(String sku) {
        return products.findBySku(sku).orElseThrow(() -> new ProductNotFoundException(sku));
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts() {
        return products.findAll();
    }

    @Transactional(readOnly = true)
    public Reservation getReservation(Long id) {
        return reservations.findById(id).orElseThrow(() -> new ReservationNotFoundException(id));
    }

    /**
     * Reserve {@code quantity} units of {@code sku}.
     *
     * <p>Idempotent on {@code idempotencyKey}: replaying a request returns the
     * reservation created the first time without holding additional stock.
     *
     * @throws ProductNotFoundException   if the SKU does not exist
     * @throws InsufficientStockException if too few units are available
     */
    @Transactional
    public Reservation reserve(String idempotencyKey, String sku, int quantity) {
        Reservation existing = reservations.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return existing;
        }

        Product product = getProduct(sku);

        int applied = products.reserveStock(product.getId(), quantity);
        if (applied == 0) {
            throw new InsufficientStockException(sku, quantity);
        }

        return reservations.save(new Reservation(idempotencyKey, product.getId(), sku, quantity));
    }

    /** Confirm a reservation: held stock is consumed and leaves inventory. */
    @Transactional
    public Reservation confirm(Long reservationId) {
        Reservation reservation = getReservation(reservationId);
        requireStatus(reservation, ReservationStatus.RESERVED, "confirm");

        Product product = products.findById(reservation.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(reservation.getSku()));
        product.consumeReserved(reservation.getQuantity());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservation;
    }

    /** Release a reservation: held stock returns to available. */
    @Transactional
    public Reservation release(Long reservationId) {
        Reservation reservation = getReservation(reservationId);
        requireStatus(reservation, ReservationStatus.RESERVED, "release");

        Product product = products.findById(reservation.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(reservation.getSku()));
        product.releaseReserved(reservation.getQuantity());
        reservation.setStatus(ReservationStatus.RELEASED);
        return reservation;
    }

    private void requireStatus(Reservation reservation, ReservationStatus expected, String action) {
        if (reservation.getStatus() != expected) {
            throw new IllegalReservationStateException(
                    "Cannot " + action + " reservation " + reservation.getId()
                            + " in state " + reservation.getStatus());
        }
    }
}
