package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.distance.Coordinate;
import com.freightfox.dispatch.distance.HaversineDistanceCalculator;
import com.freightfox.dispatch.domain.Priority;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PriorityNearestFeasibleStrategyTest {
    private final PriorityNearestFeasibleStrategy strategy =
            new PriorityNearestFeasibleStrategy(new HaversineDistanceCalculator());
    private static final Coordinate CENTER = new Coordinate(0, 0);

    private static DispatchOrder order(String id, int weight, Priority priority, Coordinate point) {
        return new DispatchOrder(id, point, "Test point", BigDecimal.valueOf(weight), priority);
    }
    private static DispatchVehicle vehicle(String id, int capacity, Coordinate point) {
        return new DispatchVehicle(id, BigDecimal.valueOf(capacity), point, "Depot");
    }

    @Test void highPriorityIsAllocatedBeforeMediumEvenIfInputOrderIsReversed() {
        var result = strategy.plan(List.of(order("medium", 10, Priority.MEDIUM, CENTER),
                order("high", 10, Priority.HIGH, CENTER)), List.of(vehicle("truck", 10, CENTER)));
        assertThat(result.dispatchPlan().getFirst().assignedOrders()).extracting(stop -> stop.order().orderId()).containsExactly("high");
        assertThat(result.unassignedOrders()).extracting(un -> un.order().orderId()).containsExactly("medium");
    }
    @Test void nearestFeasibleVehicleWins() {
        var result = strategy.plan(List.of(order("1", 10, Priority.HIGH, CENTER)),
                List.of(vehicle("far", 10, new Coordinate(20,20)), vehicle("near", 10, CENTER)));
        assertThat(result.dispatchPlan().stream().filter(v -> v.vehicleId().equals("near")).findFirst().orElseThrow()
                .assignedOrders()).hasSize(1);
    }
    @Test void fallsBackWhenNearestIsFull() {
        var result = strategy.plan(List.of(order("a", 5, Priority.HIGH, CENTER),
                order("b", 5, Priority.HIGH, CENTER)), List.of(vehicle("a", 5, CENTER),
                vehicle("b", 10, new Coordinate(1,1))));
        assertThat(result.dispatchPlan().get(0).totalLoad()).isEqualByComparingTo("5");
        assertThat(result.dispatchPlan().get(1).totalLoad()).isEqualByComparingTo("5");
    }
    @Test void exactCapacityIsAllowedAndNeverExceeded() {
        var result = strategy.plan(List.of(order("a", 5, Priority.HIGH, CENTER),
                order("b", 7, Priority.MEDIUM, CENTER)), List.of(vehicle("truck", 12, CENTER)));
        assertThat(result.dispatchPlan().getFirst().totalLoad()).isEqualByComparingTo("12");
        assertThat(result.unassignedOrders()).isEmpty();
    }
    @Test void oversizedOrderGetsDistinctReason() {
        var result = strategy.plan(List.of(order("huge", 16, Priority.HIGH, CENTER)),
                List.of(vehicle("truck", 15, CENTER)));
        assertThat(result.unassignedOrders().getFirst().reason()).isEqualTo(UnassignedReason.EXCEEDS_MAX_VEHICLE_CAPACITY);
    }
    @Test void missingFleetGetsDistinctReason() {
        var result = strategy.plan(List.of(order("a", 2, Priority.LOW, CENTER)), List.of());
        assertThat(result.dispatchPlan()).isEmpty();
        assertThat(result.unassignedOrders().getFirst().reason()).isEqualTo(UnassignedReason.NO_VEHICLES_AVAILABLE);
    }
    @Test void emptyOrdersReturnZeroDistancesAndLoadsForAllVehicles() {
        var result = strategy.plan(List.of(), List.of(vehicle("truck", 15, CENTER)));
        assertThat(result.dispatchPlan().getFirst().distanceKm()).isZero();
        assertThat(result.dispatchPlan().getFirst().totalLoad()).isEqualByComparingTo("0");
        assertThat(result.summary().totalOrders()).isZero();
    }
    @Test void deterministicWhenInputCollectionsAreReordered() {
        var a = order("a", 2, Priority.HIGH, CENTER);
        var b = order("b", 2, Priority.HIGH, CENTER);
        var x = vehicle("X", 2, CENTER);
        var y = vehicle("Y", 2, CENTER);
        assertThat(strategy.plan(List.of(a,b), List.of(x,y)))
                .isEqualTo(strategy.plan(List.of(b,a), List.of(y,x)));
    }
    @Test void computesIncrementalDistancesAndUnassignedCapacity() {
        var first = order("a", 6, Priority.HIGH, new Coordinate(0,1));
        var second = order("b", 6, Priority.MEDIUM, new Coordinate(0,2));
        var third = order("c", 1, Priority.LOW, new Coordinate(0,3));
        var result = strategy.plan(List.of(first,second,third), List.of(vehicle("truck", 12, CENTER)));
        assertThat(result.dispatchPlan().getFirst().distanceKm()).isCloseTo(222.39, org.assertj.core.data.Offset.offset(0.01));
        assertThat(result.summary().totalLoad()).isEqualByComparingTo("12");
        assertThat(result.unassignedOrders().getFirst().reason()).isEqualTo(UnassignedReason.INSUFFICIENT_REMAINING_CAPACITY);
    }
    @Test void heavierOrderComesFirstWithinSamePriority() {
        var result = strategy.plan(List.of(order("small", 2, Priority.HIGH, CENTER),
                order("large", 8, Priority.HIGH, CENTER)), List.of(vehicle("truck", 8, CENTER)));
        assertThat(result.dispatchPlan().getFirst().assignedOrders())
                .extracting(stop -> stop.order().orderId()).containsExactly("large");
        assertThat(result.unassignedOrders().getFirst().order().orderId()).isEqualTo("small");
    }
    @Test void vehicleIdBreaksEqualDistanceTies() {
        var result = strategy.plan(List.of(order("order", 1, Priority.HIGH, CENTER)),
                List.of(vehicle("B", 5, CENTER), vehicle("A", 5, CENTER)));
        assertThat(result.dispatchPlan().getFirst().vehicleId()).isEqualTo("A");
        assertThat(result.dispatchPlan().getFirst().assignedOrders()).hasSize(1);
    }
    @Test void fakeDistanceCalculatorCanControlVehicleSelection() {
        var fake = new PriorityNearestFeasibleStrategy((from, to) -> from.latitude());
        var result = fake.plan(List.of(order("a", 1, Priority.HIGH, CENTER)),
                List.of(vehicle("A", 1, new Coordinate(9,0)), vehicle("B", 1, new Coordinate(2,0))));
        assertThat(result.dispatchPlan().get(1).assignedOrders()).hasSize(1);
    }
}
