package com.kratos0722.inventory.service;

/** Thrown when no product exists for a given SKU. */
public class ProductNotFoundException extends InventoryException {
    public ProductNotFoundException(String sku) {
        super("No product found for SKU '" + sku + "'");
    }
}
