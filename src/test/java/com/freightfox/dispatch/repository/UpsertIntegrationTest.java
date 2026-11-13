package com.freightfox.dispatch.repository;

import com.freightfox.dispatch.domain.Priority;
import com.freightfox.dispatch.dto.*;
import com.freightfox.dispatch.exception.DuplicateIdException;
import com.freightfox.dispatch.service.OrderService;
import com.freightfox.dispatch.service.VehicleService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class UpsertIntegrationTest {
    @Autowired private OrderService orders;
    @Autowired private VehicleService vehicles;
    @Autowired private OrderRepository orderRepository;
    @Autowired private VehicleRepository vehicleRepository;

    @BeforeEach void clean() { orderRepository.deleteAllInBatch(); vehicleRepository.deleteAllInBatch(); }

    @Test void postingSameOrderIdUpdatesInsteadOfDuplicating() {
        orders.accept(new OrdersRequest(List.of(new OrderInput("X", 10.0, 10.0, "old",
                BigDecimal.valueOf(7), Priority.LOW))));
        orders.accept(new OrdersRequest(List.of(new OrderInput("X", 11.0, 11.0, "new",
                BigDecimal.valueOf(8), Priority.HIGH))));
        assertThat(orderRepository.count()).isOne();
        var updated = orderRepository.findById("X").orElseThrow();
        assertThat(updated.getAddress()).isEqualTo("new");
        assertThat(updated.getPackageWeight()).isEqualByComparingTo("8");
        assertThat(updated.getPriority()).isEqualTo(Priority.HIGH);
    }
    @Test void postingSameVehicleIdUpdatesInsteadOfDuplicating() {
        vehicles.accept(new VehiclesRequest(List.of(new VehicleInput("V", BigDecimal.TEN, 0.0, 0.0, "old"))));
        vehicles.accept(new VehiclesRequest(List.of(new VehicleInput("V", BigDecimal.valueOf(12), 1.0, 1.0, "new"))));
        assertThat(vehicleRepository.count()).isOne();
        assertThat(vehicleRepository.findById("V").orElseThrow().getCapacity()).isEqualByComparingTo("12");
    }
    @Test void duplicateIdInOneBatchIsRejectedBeforeWritingAnything() {
        var batch = new OrdersRequest(List.of(
                new OrderInput("X", 0.0, 0.0, "one", BigDecimal.ONE, Priority.HIGH),
                new OrderInput("X", 0.0, 0.0, "two", BigDecimal.TEN, Priority.LOW)));
        assertThatThrownBy(() -> orders.accept(batch)).isInstanceOf(DuplicateIdException.class)
                .hasMessageContaining("Duplicate orderId");
        assertThat(orderRepository.count()).isZero();
    }
    @Test void canUpsertSeveralHundredRowsInBatches() {
        var input = java.util.stream.IntStream.range(0, 600)
                .mapToObj(i -> new OrderInput("O" + i, 0.0, 0.0, "sample", BigDecimal.ONE, Priority.LOW))
                .toList();
        orders.accept(new OrdersRequest(input));
        assertThat(orderRepository.count()).isEqualTo(600);
        orders.accept(new OrdersRequest(input));
        assertThat(orderRepository.count()).isEqualTo(600);
    }
}
