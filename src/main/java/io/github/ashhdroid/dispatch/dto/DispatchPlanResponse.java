package io.github.ashhdroid.dispatch.dto;

import io.github.ashhdroid.dispatch.domain.Priority;
import io.github.ashhdroid.dispatch.strategy.UnassignedReason;
import java.math.BigDecimal;
import java.util.List;

public record DispatchPlanResponse(List<VehiclePlan> dispatchPlan,
                                   List<UnassignedOrder> unassignedOrders,
                                   Summary summary) {
    public record OrderView(String orderId, double latitude, double longitude, String address,
                            BigDecimal packageWeight, Priority priority,
                            int sequence, double distanceFromPreviousKm) {}
    public record VehiclePlan(String vehicleId, BigDecimal totalLoad, String totalDistance,
                              List<OrderView> assignedOrders, BigDecimal remainingCapacity,
                              double utilizationPercent) {}
    public record UnassignedOrder(String orderId, BigDecimal packageWeight,
                                  Priority priority, String address, UnassignedReason reason) {}
    public record Summary(int totalOrders, int assignedOrders, int unassignedOrders,
                          BigDecimal totalLoad, BigDecimal totalCapacity,
                          String totalDistance, String strategy) {}
}
