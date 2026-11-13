package com.freightfox.dispatch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freightfox.dispatch.dto.OrdersRequest;
import com.freightfox.dispatch.dto.VehiclesRequest;
import com.freightfox.dispatch.service.OrderService;
import com.freightfox.dispatch.service.VehicleService;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads synthetic sample records on startup only when explicitly enabled.
 * Useful on ephemeral hosts where the default in-memory H2 database resets.
 */
@Component
@ConditionalOnProperty(prefix = "app", name = "seed-sample-data", havingValue = "true")
public class SampleDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SampleDataSeeder.class);
    private final ObjectMapper json;
    private final OrderService orders;
    private final VehicleService vehicles;

    public SampleDataSeeder(ObjectMapper json, OrderService orders, VehicleService vehicles) {
        this.json = json;
        this.orders = orders;
        this.vehicles = vehicles;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        OrdersRequest orderInput;
        VehiclesRequest vehicleInput;
        try (InputStream input = new ClassPathResource("samples/orders.json").getInputStream()) {
            orderInput = json.readValue(input, OrdersRequest.class);
        }
        try (InputStream input = new ClassPathResource("samples/vehicles.json").getInputStream()) {
            vehicleInput = json.readValue(input, VehiclesRequest.class);
        }
        // Upsert rather than append: a restart remains deterministic.
        orders.accept(orderInput);
        vehicles.accept(vehicleInput);
        log.info("Loaded synthetic demo dataset: {} orders, {} vehicles",
                orderInput.orders().size(), vehicleInput.vehicles().size());
    }
}
