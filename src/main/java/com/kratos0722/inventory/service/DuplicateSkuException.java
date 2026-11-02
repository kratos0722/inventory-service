package com.kratos0722.inventory.service;

/** Thrown when creating a product whose SKU already exists. */
public class DuplicateSkuException extends InventoryException {
    public DuplicateSkuException(String sku) {
        super("A product with SKU '" + sku + "' already exists");
    }
}
