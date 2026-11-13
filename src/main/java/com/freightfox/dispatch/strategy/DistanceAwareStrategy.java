package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.distance.Coordinate;
import com.freightfox.dispatch.distance.DistanceCalculator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Deterministic, bounded 2-opt open-route improvement applied after greedy allocation.
 * A reversal affects only the two boundary edges (the distance metric is symmetric).
 * It never removes deliveries, changes vehicles, or violates capacity constraints.
 */
@Component
public class DistanceAwareStrategy implements DispatchStrategy {
    private static final int MAX_STOPS_PER_ROUTE = 1_000;
    private static final int MAX_SWEEPS = 3;
    private static final double MIN_IMPROVEMENT_KM = 1e-9;

    private final PriorityNearestFeasibleStrategy baseline;
    private final DistanceCalculator distance;

    public DistanceAwareStrategy(PriorityNearestFeasibleStrategy baseline, DistanceCalculator distance) {
        this.baseline = baseline;
        this.distance = distance;
    }

    @Override public String key() { return "distance-aware"; }

    @Override
    public DispatchPlan plan(List<DispatchOrder> orders, List<DispatchVehicle> fleet) {
        DispatchPlan first = baseline.plan(orders, fleet);
        List<DispatchPlan.VehicleRoute> routes = new ArrayList<>();

        for (var route : first.dispatchPlan()) {
            int count = route.assignedOrders().size();
            if (count < 3 || count > MAX_STOPS_PER_ROUTE) {
                routes.add(route);
                continue;
            }

            List<DispatchOrder> stops = new ArrayList<>(
                    route.assignedOrders().stream().map(DispatchPlan.Stop::order).toList());
            Coordinate depot = route.vehicle().initialLocation();
            boolean changed = false;

            for (int sweep = 0; sweep < MAX_SWEEPS; sweep++) {
                boolean improved = false;
                for (int i = 0; i < count - 1; i++) {
                    Coordinate previous = i == 0 ? depot : stops.get(i - 1).location();
                    for (int j = i + 1; j < count; j++) {
                        Coordinate firstStop = stops.get(i).location();
                        Coordinate lastStop = stops.get(j).location();

                        // The internal reversed edges preserve their lengths under Haversine.
                        double before = distance.distanceKm(previous, firstStop);
                        double after = distance.distanceKm(previous, lastStop);
                        if (j + 1 < count) {
                            Coordinate next = stops.get(j + 1).location();
                            before += distance.distanceKm(lastStop, next);
                            after += distance.distanceKm(firstStop, next);
                        }

                        if (after + MIN_IMPROVEMENT_KM < before) {
                            Collections.reverse(stops.subList(i, j + 1));
                            improved = true;
                            changed = true;
                        }
                    }
                }
                if (!improved) break;
            }

            // Recalculate once after local search, and retain the baseline on numeric ties.
            double candidateDistance = length(depot, stops);
            if (!changed || candidateDistance + MIN_IMPROVEMENT_KM >= route.distanceKm()) {
                routes.add(route);
                continue;
            }

            List<DispatchPlan.Stop> scheduled = new ArrayList<>(count);
            Coordinate previous = depot;
            double totalDistance = 0;
            for (DispatchOrder stop : stops) {
                double leg = distance.distanceKm(previous, stop.location());
                totalDistance += leg;
                scheduled.add(new DispatchPlan.Stop(stop, scheduled.size() + 1, leg));
                previous = stop.location();
            }
            routes.add(new DispatchPlan.VehicleRoute(route.vehicleId(), route.totalLoad(), totalDistance,
                    route.remainingCapacity(), route.capacity(), List.copyOf(scheduled), route.vehicle()));
        }
        return DispatchPlans.assemble(routes, first.unassignedOrders(), orders.size());
    }

    private double length(Coordinate depot, List<DispatchOrder> stops) {
        double km = 0;
        Coordinate previous = depot;
        for (DispatchOrder stop : stops) {
            km += distance.distanceKm(previous, stop.location());
            previous = stop.location();
        }
        return km;
    }
}
