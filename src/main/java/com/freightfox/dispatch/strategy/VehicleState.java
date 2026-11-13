package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.distance.Coordinate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Mutable, per-planning-request state. Never shared with the database or other requests. */
final class VehicleState {
    private final DispatchVehicle vehicle;
    private final List<DispatchPlan.Stop> assignments = new ArrayList<>();
    private BigDecimal remainingCapacity;
    private BigDecimal totalLoad = BigDecimal.ZERO;
    private Coordinate position;
    private double traveledKm;

    VehicleState(DispatchVehicle vehicle) {
        this.vehicle = vehicle;
        remainingCapacity = vehicle.capacity();
        position = vehicle.initialLocation();
    }
    String id() { return vehicle.vehicleId(); }
    BigDecimal remainingCapacity() { return remainingCapacity; }
    Coordinate position() { return position; }
    double traveledKm() { return traveledKm; }
    BigDecimal totalLoad() { return totalLoad; }
    DispatchPlan.VehicleRoute route() {
        return new DispatchPlan.VehicleRoute(id(), normalized(totalLoad), traveledKm,
                normalized(remainingCapacity), normalized(vehicle.capacity()), List.copyOf(assignments), vehicle);
    }
    void assign(DispatchOrder order, double legKm) {
        if (remainingCapacity.compareTo(order.packageWeight()) < 0) {
            throw new IllegalStateException("Cannot exceed vehicle capacity");
        }
        remainingCapacity = remainingCapacity.subtract(order.packageWeight());
        totalLoad = totalLoad.add(order.packageWeight());
        traveledKm += legKm;
        position = order.location();
        assignments.add(new DispatchPlan.Stop(order, assignments.size() + 1, legKm));
    }
    static BigDecimal normalized(BigDecimal value) {
        var stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
