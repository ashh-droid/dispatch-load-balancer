package com.freightfox.dispatch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freightfox.dispatch.repository.OrderRepository;
import com.freightfox.dispatch.repository.VehicleRepository;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SampleDatasetIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private OrderRepository orders;
    @Autowired private VehicleRepository vehicles;

    @BeforeEach void reset() { orders.deleteAllInBatch(); vehicles.deleteAllInBatch(); }

    private String sample(String filename) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/samples/" + filename)) {
            if (in == null) throw new IllegalStateException("Missing sample " + filename);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void exactOfficialDatasetRegressesToExpectedGreedyResult() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON)
                .content(sample("orders.json"))).andExpect(status().isOk());
        mvc.perform(post("/api/dispatch/vehicles").contentType(MediaType.APPLICATION_JSON)
                .content(sample("vehicles.json"))).andExpect(status().isOk());

        String json = mvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.totalOrders").value(30))
                .andExpect(jsonPath("$.summary.assignedOrders").value(22))
                .andExpect(jsonPath("$.summary.unassignedOrders").value(8))
                .andExpect(jsonPath("$.summary.totalLoad").value(490))
                .andExpect(jsonPath("$.summary.totalCapacity").value(500))
                .andExpect(jsonPath("$.summary.totalDistance").value("73.42 km"))
                .andReturn().getResponse().getContentAsString();
        JsonNode plan = mapper.readTree(json);
        var expected = Map.of(
                "VEH001", "100|14.44 km|5",
                "VEH002", "80|0.00 km|3",
                "VEH003", "115|24.74 km|5",
                "VEH004", "90|19.80 km|4",
                "VEH005", "105|14.44 km|5");
        int high = 0, medium = 0, low = 0;
        for (JsonNode vehicle : plan.get("dispatchPlan")) {
            String actual = vehicle.get("totalLoad").decimalValue().stripTrailingZeros().toPlainString()
                    + "|" + vehicle.get("totalDistance").asText()
                    + "|" + vehicle.get("assignedOrders").size();
            assertThat(actual).isEqualTo(expected.get(vehicle.get("vehicleId").asText()));
            for (JsonNode order : vehicle.get("assignedOrders")) {
                switch (order.get("priority").asText()) {
                    case "HIGH" -> high++;
                    case "MEDIUM" -> medium++;
                    case "LOW" -> low++;
                    default -> throw new AssertionError("Unexpected priority");
                }
            }
        }
        assertThat(high).isEqualTo(12);
        assertThat(medium).isEqualTo(8);
        assertThat(low).isEqualTo(2);
        for (JsonNode missing : plan.get("unassignedOrders")) {
            assertThat(missing.get("priority").asText()).isEqualTo("LOW");
            assertThat(missing.get("reason").asText()).isEqualTo("INSUFFICIENT_REMAINING_CAPACITY");
        }
    }
}
