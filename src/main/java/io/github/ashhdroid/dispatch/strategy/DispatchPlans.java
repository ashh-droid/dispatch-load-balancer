package io.github.ashhdroid.dispatch.strategy;

import java.math.BigDecimal;
import java.util.List;

/** Common result aggregation shared by planning strategies. */
final class DispatchPlans {
    private DispatchPlans() {}
    static DispatchPlan assemble(List<DispatchPlan.VehicleRoute> routes,
                                 List<DispatchPlan.Unassigned> unassigned, int orderCount) {
        BigDecimal load = routes.stream().map(DispatchPlan.VehicleRoute::totalLoad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal capacity = routes.stream().map(DispatchPlan.VehicleRoute::capacity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double distance = routes.stream().mapToDouble(DispatchPlan.VehicleRoute::distanceKm).sum();
        return new DispatchPlan(List.copyOf(routes), List.copyOf(unassigned),
                new DispatchPlan.Summary(orderCount, orderCount - unassigned.size(), unassigned.size(),
                        VehicleState.normalized(load), VehicleState.normalized(capacity), distance));
    }
}
