package com.kratos0722.inventory.repository;

import com.kratos0722.inventory.domain.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    /**
     * Atomically move {@code qty} units from available to reserved, but only if
     * at least {@code qty} are available. The {@code quantity_available >= :qty}
     * guard in the WHERE clause lets the database serialize concurrent reservers:
     * exactly the callers that find enough stock succeed, so overselling is
     * impossible without any application-level locking or retry.
     *
     * @return 1 if the reservation was applied, 0 if there was insufficient stock
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Product p
           set p.quantityAvailable = p.quantityAvailable - :qty,
               p.quantityReserved  = p.quantityReserved + :qty
         where p.id = :id
           and p.quantityAvailable >= :qty
        """)
    int reserveStock(@Param("id") Long id, @Param("qty") int qty);
}
