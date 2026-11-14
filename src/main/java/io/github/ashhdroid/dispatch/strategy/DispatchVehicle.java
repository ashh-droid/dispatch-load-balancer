package io.github.ashhdroid.dispatch.strategy;

import io.github.ashhdroid.dispatch.distance.Coordinate;
import java.math.BigDecimal;

public record DispatchVehicle(String vehicleId, BigDecimal capacity,
                              Coordinate initialLocation, String currentAddress) {}
