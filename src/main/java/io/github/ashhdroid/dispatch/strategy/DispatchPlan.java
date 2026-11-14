package io.github.ashhdroid.dispatch.strategy;

import java.math.BigDecimal;
import java.util.List;

/** Backend planning model without HTTP, JSON formatting, or JPA dependencies. */
public record DispatchPlan(List<VehicleRoute> dispatchPlan,
                           List<Unassigned> unassignedOrders,
                           Summary summary) {
    public record Stop(DispatchOrder order, int sequence, double legDistanceKm) {}
    public record VehicleRoute(String vehicleId, BigDecimal totalLoad, double distanceKm,
                               BigDecimal remainingCapacity, BigDecimal capacity,
                               List<Stop> assignedOrders, DispatchVehicle vehicle) {}
    public record Unassigned(DispatchOrder order, UnassignedReason reason) {}
    public record Summary(int totalOrders, int assignedOrders, int unassignedOrders,
                          BigDecimal totalLoad, BigDecimal totalCapacity, double distanceKm) {}
}
