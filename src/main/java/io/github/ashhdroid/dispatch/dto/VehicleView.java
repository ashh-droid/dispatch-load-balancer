package io.github.ashhdroid.dispatch.dto;

import java.math.BigDecimal;

/** Read-only API representation, independent of request validation. */
public record VehicleView(String vehicleId, BigDecimal capacity, double currentLatitude,
                          double currentLongitude, String currentAddress) {}
