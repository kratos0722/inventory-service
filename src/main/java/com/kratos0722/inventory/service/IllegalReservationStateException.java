package com.kratos0722.inventory.service;

/** Thrown when a reservation transition is not allowed from its current state. */
public class IllegalReservationStateException extends InventoryException {
    public IllegalReservationStateException(String message) {
        super(message);
    }
}
