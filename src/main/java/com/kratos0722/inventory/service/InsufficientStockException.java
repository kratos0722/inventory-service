package com.kratos0722.inventory.service;

/** Thrown when a reservation asks for more units than are available. */
public class InsufficientStockException extends InventoryException {
    public InsufficientStockException(String sku, int requested) {
        super("Insufficient stock for SKU '" + sku + "' to reserve " + requested + " unit(s)");
    }
}
