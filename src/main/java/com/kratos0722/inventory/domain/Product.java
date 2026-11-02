package com.kratos0722.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A stock-keeping product.
 *
 * <p>Stock is split into {@code quantityAvailable} (free to reserve) and
 * {@code quantityReserved} (held by open reservations). Reservations move
 * quantity from available to reserved; confirming consumes reserved, releasing
 * returns it to available. The {@link Version} column guards general updates
 * with optimistic locking, while the reservation decrement itself is performed
 * as a guarded atomic SQL update (see the repository) so concurrent reservers
 * can never oversell.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
        // for JPA
    }

    public Product(String sku, String name, String description, BigDecimal unitPrice, int quantityAvailable) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.unitPrice = unitPrice;
        this.quantityAvailable = quantityAvailable;
        this.quantityReserved = 0;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Confirm a previously reserved quantity: it leaves the warehouse, so it is
     * removed from reserved and never returns to available.
     */
    public void consumeReserved(int quantity) {
        if (quantity > quantityReserved) {
            throw new IllegalStateException("Cannot confirm more than is reserved");
        }
        this.quantityReserved -= quantity;
    }

    /**
     * Release a previously reserved quantity back to available stock.
     */
    public void releaseReserved(int quantity) {
        if (quantity > quantityReserved) {
            throw new IllegalStateException("Cannot release more than is reserved");
        }
        this.quantityReserved -= quantity;
        this.quantityAvailable += quantity;
    }
}
