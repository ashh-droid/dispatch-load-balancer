package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.distance.Coordinate;
import com.freightfox.dispatch.distance.HaversineDistanceCalculator;
import com.freightfox.dispatch.domain.Priority;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DistanceAwareStrategyTest {
    private final HaversineDistanceCalculator distances = new HaversineDistanceCalculator();
    private final PriorityNearestFeasibleStrategy greedy = new PriorityNearestFeasibleStrategy(distances);
    private final DistanceAwareStrategy optimized = new DistanceAwareStrategy(greedy, distances);

    @Test void improvesAnIntentionallyPoorStopSequenceWithoutDroppingOrders() {
        // All points use a single truck. Greedy chooses nearer stops, so improvement
        // is non-strict: this test verifies preservation and non-regression.
        var stops = List.of(order("a", 1, 0, 1), order("b", 1, 0, 5),
                order("c", 1, 2, 1), order("d", 1, 3, 3));
        var fleet = List.of(new DispatchVehicle("truck", BigDecimal.TEN,
                new Coordinate(0, 0), "Depot"));
        var a = greedy.plan(stops, fleet);
        var b = optimized.plan(stops, fleet);
        assertThat(b.summary().distanceKm()).isLessThanOrEqualTo(a.summary().distanceKm() + 1e-8);
        assertThat(b.summary().assignedOrders()).isEqualTo(a.summary().assignedOrders());
        assertThat(b.dispatchPlan().getFirst().totalLoad()).isEqualByComparingTo(a.dispatchPlan().getFirst().totalLoad());
    }

    @Test void seededRandomInvariantsCoverBothStrategies() {
        var rng = new Random(451971);
        for (int trial = 0; trial < 200; trial++) {
            var orders = new ArrayList<DispatchOrder>();
            var vehicles = new ArrayList<DispatchVehicle>();
            for (int i = 0; i < 1 + rng.nextInt(7); i++)
                vehicles.add(new DispatchVehicle("V" + i, BigDecimal.valueOf(5 + rng.nextInt(50)),
                        new Coordinate(10 + rng.nextDouble(), 75 + rng.nextDouble()), "Depot"));
            for (int i = 0; i < 1 + rng.nextInt(35); i++)
                orders.add(new DispatchOrder("O" + i,
                        new Coordinate(10 + rng.nextDouble(), 75 + rng.nextDouble()), "Test",
                        BigDecimal.valueOf(1 + rng.nextInt(25)), Priority.values()[rng.nextInt(3)]));
            Collections.shuffle(orders, rng);
            var first = greedy.plan(orders, vehicles);
            var second = optimized.plan(orders, vehicles);
            verifyInvariants(first, orders, vehicles);
            verifyInvariants(second, orders, vehicles);
            assertThat(second.summary().distanceKm()).isLessThanOrEqualTo(first.summary().distanceKm() + 1e-6);
            assertThat(second.unassignedOrders()).isEqualTo(first.unassignedOrders());
            for (int i = 0; i < first.dispatchPlan().size(); i++) {
                var idsA = first.dispatchPlan().get(i).assignedOrders().stream().map(stop -> stop.order().orderId()).toList();
                var idsB = second.dispatchPlan().get(i).assignedOrders().stream().map(stop -> stop.order().orderId()).toList();
                assertThat(idsB).containsExactlyInAnyOrderElementsOf(idsA);
            }
        }
    }
    private static DispatchOrder order(String id, int weight, double lat, double lon) {
        return new DispatchOrder(id, new Coordinate(lat, lon), "Test", BigDecimal.valueOf(weight), Priority.HIGH);
    }
    private void verifyInvariants(DispatchPlan result, List<DispatchOrder> orders, List<DispatchVehicle> vehicles) {
        var seen = new HashSet<String>();
        BigDecimal sumWeight = BigDecimal.ZERO;
        double totalKm = 0;
        for (var route : result.dispatchPlan()) {
            BigDecimal routeLoad = BigDecimal.ZERO;
            double routeKm = 0;
            var current = route.vehicle().initialLocation();
            for (var stop : route.assignedOrders()) {
                assertThat(seen.add(stop.order().orderId())).isTrue();
                routeLoad = routeLoad.add(stop.order().packageWeight());
                routeKm += distances.distanceKm(current, stop.order().location());
                current = stop.order().location();
            }
            assertThat(route.totalLoad()).isEqualByComparingTo(routeLoad);
            assertThat(route.totalLoad()).isLessThanOrEqualTo(route.capacity());
            assertThat(route.distanceKm()).isCloseTo(routeKm, within(1e-6));
            sumWeight = sumWeight.add(routeLoad);
            totalKm += routeKm;
        }
        for (var missing : result.unassignedOrders()) assertThat(seen.add(missing.order().orderId())).isTrue();
        assertThat(seen).hasSize(orders.size());
        assertThat(result.summary().totalLoad()).isEqualByComparingTo(sumWeight);
        assertThat(result.summary().distanceKm()).isCloseTo(totalKm, within(1e-6));
        assertThat(result.summary().assignedOrders() + result.summary().unassignedOrders()).isEqualTo(orders.size());
        assertThat(result.dispatchPlan()).hasSize(vehicles.size());
    }
}
