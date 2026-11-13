package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.distance.Coordinate;
import java.math.BigDecimal;

public record DispatchVehicle(String vehicleId, BigDecimal capacity,
                              Coordinate initialLocation, String currentAddress) {}
