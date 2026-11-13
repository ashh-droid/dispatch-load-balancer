package com.freightfox.dispatch.strategy;

/** Machine-readable reasons why a delivery cannot fit the selected plan. */
public enum UnassignedReason {
    NO_VEHICLES_AVAILABLE,
    EXCEEDS_MAX_VEHICLE_CAPACITY,
    INSUFFICIENT_REMAINING_CAPACITY
}
