package com.freightfox.dispatch.service;

import com.freightfox.dispatch.repository.OrderRepository;
import com.freightfox.dispatch.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ManagementEndpointsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrderRepository orders;
    @Autowired VehicleRepository vehicles;
    @BeforeEach void reset() { orders.deleteAllInBatch(); vehicles.deleteAllInBatch(); }

    @Test void readDeleteAndStrategyEndpointsBehaveConsistently() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"orders":[{"orderId":"O1","latitude":0,"longitude":0,"address":"A","packageWeight":1,"priority":"HIGH"},
                               {"orderId":"O2","latitude":1,"longitude":1,"address":"B","packageWeight":2,"priority":"LOW"}]}
                    """)).andExpect(status().isOk());
        mvc.perform(post("/api/dispatch/vehicles").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"vehicles":[{"vehicleId":"V1","capacity":10,"currentLatitude":0,"currentLongitude":0,"currentAddress":"Depot"}]}
                    """)).andExpect(status().isOk());
        mvc.perform(get("/api/dispatch/orders?page=0&size=1&priority=HIGH"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].orderId").value("O1"));
        mvc.perform(get("/api/dispatch/orders/O2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orderId").value("O2"));
        mvc.perform(get("/api/dispatch/plan?strategy=distance-aware"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.strategy").value("distance-aware"))
                .andExpect(jsonPath("$.summary.assignedOrders").value(2))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].sequence").isNumber());
        mvc.perform(delete("/api/dispatch/orders/O2")).andExpect(status().isNoContent());
        mvc.perform(get("/api/dispatch/orders/O2")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/dispatch/orders")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/dispatch/vehicles")).andExpect(status().isNoContent());
        mvc.perform(get("/api/dispatch/plan")).andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalOrders").value(0));
    }
}
