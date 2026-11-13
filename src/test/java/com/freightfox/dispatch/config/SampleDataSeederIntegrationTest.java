package com.freightfox.dispatch.config;

import com.freightfox.dispatch.repository.OrderRepository;
import com.freightfox.dispatch.repository.VehicleRepository;
import com.freightfox.dispatch.service.DispatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.seed-sample-data=true")
class SampleDataSeederIntegrationTest {
    @Autowired OrderRepository orders;
    @Autowired VehicleRepository vehicles;
    @Autowired DispatchService planning;

    @Test void optInStartupSeedingLoadsOfficialDataset() {
        assertThat(orders.count()).isEqualTo(30);
        assertThat(vehicles.count()).isEqualTo(5);
        var summary = planning.plan().summary();
        assertThat(summary.totalOrders()).isEqualTo(30);
        assertThat(summary.assignedOrders()).isEqualTo(22);
        assertThat(summary.unassignedOrders()).isEqualTo(8);
        assertThat(summary.totalDistance()).isEqualTo("73.42 km");
    }
}
