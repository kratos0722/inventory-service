package com.kratos0722.inventory.service;

/** Thrown when no reservation exists for a given id. */
public class ReservationNotFoundException extends InventoryException {
    public ReservationNotFoundException(Long id) {
        super("No reservation found with id " + id);
    }
}
