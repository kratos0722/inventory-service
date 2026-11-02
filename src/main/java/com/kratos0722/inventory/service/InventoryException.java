package com.kratos0722.inventory.service;

/** Base type for domain errors raised by the inventory service. */
public abstract class InventoryException extends RuntimeException {
    protected InventoryException(String message) {
        super(message);
    }
}
