package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.distance.Coordinate;
import com.freightfox.dispatch.distance.DistanceCalculator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Route-only 2-opt improvement. Never removes a delivery, changes capacity or alters
 * the set of orders accepted by the greedy strategy. Retains original plan when no
 * shorter sequence exists. Exact same-location stops naturally offer zero improvement.
 */
@Component
public class DistanceAwareStrategy implements DispatchStrategy {
    private final PriorityNearestFeasibleStrategy baseline;
    private final DistanceCalculator distance;
    public DistanceAwareStrategy(PriorityNearestFeasibleStrategy baseline, DistanceCalculator distance) {
        this.baseline = baseline;
        this.distance = distance;
    }
    @Override public String key() { return "distance-aware"; }
    @Override public DispatchPlan plan(List<DispatchOrder> orders, List<DispatchVehicle> fleet) {
        DispatchPlan first = baseline.plan(orders, fleet);
        List<DispatchPlan.VehicleRoute> routes = new ArrayList<>();
        for (var route : first.dispatchPlan()) {
            // Keep large-route planning predictable in cost rather than running cubic local search.
            if (route.assignedOrders().size() < 3 || route.assignedOrders().size() > 60) {
                routes.add(route);
                continue;
            }
            List<DispatchOrder> stops = new ArrayList<>(route.assignedOrders().stream()
                    .map(DispatchPlan.Stop::order).toList());
            double best = length(route.vehicle().initialLocation(), stops);
            for (int sweep = 0; sweep < 3; sweep++) {
                boolean improved = false;
                for (int i = 0; i < stops.size() - 1; i++) {
                    for (int j = i + 1; j < stops.size(); j++) {
                        Collections.reverse(stops.subList(i, j + 1));
                        double candidate = length(route.vehicle().initialLocation(), stops);
                        if (candidate + 1e-9 < best) {
                            best = candidate;
                            improved = true;
                        } else {
                            Collections.reverse(stops.subList(i, j + 1));
                        }
                    }
                }
                if (!improved) break;
            }
            List<DispatchPlan.Stop> scheduled = new ArrayList<>();
            Coordinate previous = route.vehicle().initialLocation();
            for (var stop : stops) {
                scheduled.add(new DispatchPlan.Stop(stop, scheduled.size() + 1,
                        distance.distanceKm(previous, stop.location())));
                previous = stop.location();
            }
            routes.add(new DispatchPlan.VehicleRoute(route.vehicleId(), route.totalLoad(), best,
                    route.remainingCapacity(), route.capacity(), List.copyOf(scheduled), route.vehicle()));
        }
        return DispatchPlans.assemble(routes, first.unassignedOrders(), orders.size());
    }
    private double length(Coordinate depot, List<DispatchOrder> stops) {
        double km = 0;
        var previous = depot;
        for (var stop : stops) {
            km += distance.distanceKm(previous, stop.location());
            previous = stop.location();
        }
        return km;
    }
}
