package io.github.ashhdroid.dispatch.mapper;

import io.github.ashhdroid.dispatch.dto.DispatchPlanResponse;
import io.github.ashhdroid.dispatch.strategy.DispatchPlan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Converts the independent routing model into the assignment's public JSON contract. */
@Component
public class DispatchPlanMapper {
    public DispatchPlanResponse toResponse(DispatchPlan plan, String strategy) {
        var routes = plan.dispatchPlan().stream().map(route -> {
            var stops = route.assignedOrders().stream().map(stop -> {
                var order = stop.order();
                return new DispatchPlanResponse.OrderView(order.orderId(), order.location().latitude(),
                        order.location().longitude(), order.address(), order.packageWeight(), order.priority(),
                        stop.sequence(), round(stop.legDistanceKm()));
            }).toList();
            double usage = route.capacity().signum() == 0 ? 0 :
                    route.totalLoad().multiply(BigDecimal.valueOf(100))
                            .divide(route.capacity(), 2, RoundingMode.HALF_UP).doubleValue();
            return new DispatchPlanResponse.VehiclePlan(route.vehicleId(), route.totalLoad(),
                    km(route.distanceKm()), stops, route.remainingCapacity(), usage);
        }).toList();
        var unassigned = plan.unassignedOrders().stream().map(item -> {
            var order = item.order();
            return new DispatchPlanResponse.UnassignedOrder(order.orderId(), order.packageWeight(),
                    order.priority(), order.address(), item.reason());
        }).toList();
        var summary = plan.summary();
        return new DispatchPlanResponse(routes, unassigned, new DispatchPlanResponse.Summary(
                summary.totalOrders(), summary.assignedOrders(), summary.unassignedOrders(),
                summary.totalLoad(), summary.totalCapacity(), km(summary.distanceKm()), strategy));
    }
    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
    private static String km(double v) { return String.format(Locale.ROOT, "%.2f km", v); }
}
