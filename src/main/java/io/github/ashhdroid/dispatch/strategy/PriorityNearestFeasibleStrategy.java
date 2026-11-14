package io.github.ashhdroid.dispatch.strategy;

import io.github.ashhdroid.dispatch.distance.DistanceCalculator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** Deterministic priority-first greedy heuristic; not a globally optimal CVRP solver. */
@Component
public class PriorityNearestFeasibleStrategy implements DispatchStrategy {
    private final DistanceCalculator distances;
    public PriorityNearestFeasibleStrategy(DistanceCalculator distances) { this.distances = distances; }
    @Override public String key() { return "greedy"; }

    @Override public DispatchPlan plan(List<DispatchOrder> orders, List<DispatchVehicle> vehicles) {
        List<VehicleState> states = vehicles.stream()
                .sorted(Comparator.comparing(DispatchVehicle::vehicleId))
                .map(VehicleState::new).toList();
        List<DispatchOrder> prioritized = orders.stream()
                .sorted(Comparator.comparingInt((DispatchOrder order) -> order.priority().ordinal())
                        .thenComparing(DispatchOrder::packageWeight, Comparator.reverseOrder())
                        .thenComparing(DispatchOrder::orderId)).toList();
        BigDecimal maxCapacity = vehicles.stream().map(DispatchVehicle::capacity)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<DispatchPlan.Unassigned> unassigned = new ArrayList<>();
        for (DispatchOrder order : prioritized) {
            VehicleState closest = null;
            double best = Double.POSITIVE_INFINITY;
            for (VehicleState state : states) {
                if (state.remainingCapacity().compareTo(order.packageWeight()) < 0) continue;
                double leg = distances.distanceKm(state.position(), order.location());
                if (closest == null || Double.compare(leg, best) < 0 ||
                        (Double.compare(leg, best) == 0 && state.id().compareTo(closest.id()) < 0)) {
                    closest = state;
                    best = leg;
                }
            }
            if (closest != null) {
                closest.assign(order, best);
            } else {
                UnassignedReason reason = states.isEmpty() ? UnassignedReason.NO_VEHICLES_AVAILABLE
                        : order.packageWeight().compareTo(maxCapacity) > 0
                        ? UnassignedReason.EXCEEDS_MAX_VEHICLE_CAPACITY
                        : UnassignedReason.INSUFFICIENT_REMAINING_CAPACITY;
                unassigned.add(new DispatchPlan.Unassigned(order, reason));
            }
        }
        var routes = states.stream().map(VehicleState::route).toList();
        return DispatchPlans.assemble(routes, unassigned, orders.size());
    }
}
