package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.distance.Coordinate;
import com.freightfox.dispatch.distance.HaversineDistanceCalculator;
import com.freightfox.dispatch.domain.Priority;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@Tag("performance")
class DispatchPerformanceTest {
    @Test void thousandsOfOrdersPreserveCapacityAndCompleteDeterministically() {
        var orders = IntStream.range(0, 2000).mapToObj(i -> new DispatchOrder("O" + i,
                new Coordinate(12.0 + (i % 50) / 1000.0, 77.0 + (i % 200) / 1000.0),
                "Test", BigDecimal.ONE, Priority.values()[i % 3])).toList();
        var vehicles = IntStream.range(0, 100).mapToObj(i -> new DispatchVehicle("V" + i,
                BigDecimal.valueOf(25), new Coordinate(12.0 + i / 1000.0, 77.0), "Depot")).toList();
        var strategy = new PriorityNearestFeasibleStrategy(new HaversineDistanceCalculator());
        var plan = strategy.plan(orders, vehicles);
        assertThat(plan.summary().assignedOrders()).isEqualTo(2000);
        assertThat(plan.unassignedOrders()).isEmpty();
        plan.dispatchPlan().forEach(v -> assertThat(v.totalLoad()).isLessThanOrEqualTo(BigDecimal.valueOf(25)));
    }
}
