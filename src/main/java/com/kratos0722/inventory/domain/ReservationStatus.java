package com.kratos0722.inventory.domain;

/** Lifecycle of a stock reservation. */
public enum ReservationStatus {
    /** Stock is held for this reservation but not yet shipped. */
    RESERVED,
    /** The held stock has been consumed (shipped/fulfilled). */
    CONFIRMED,
    /** The hold was cancelled and the stock returned to available. */
    RELEASED
}
