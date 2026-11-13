package com.freightfox.dispatch.controller;

import com.freightfox.dispatch.service.OrderService;
import com.freightfox.dispatch.service.VehicleService;
import com.freightfox.dispatch.service.DispatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DispatchControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private OrderService orders;
    @MockitoBean private VehicleService vehicles;
    @MockitoBean private DispatchService dispatch;

    @Test void validOrdersAccepted() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"orders":[{"orderId":"O1","latitude":28,"longitude":77,"address":"Delhi","packageWeight":1,"priority":"high"}]}
                  """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"));
    }
    @Test void outOfRangeCoordinatesRejected() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"orders":[{"orderId":"O1","latitude":91,"longitude":77,"address":"Delhi","packageWeight":1,"priority":"HIGH"}]}
                  """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("orders[0].latitude"));
    }
    @Test void negativeWeightRejected() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"orders":[{"orderId":"O1","latitude":28,"longitude":77,"address":"Delhi","packageWeight":-1,"priority":"HIGH"}]}
                  """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value("error"));
    }
    @Test void emptyOrdersRejected() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"orders\":[]}"))
                .andExpect(status().isBadRequest());
    }
    @Test void invalidPriorityReported() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"orders":[{"orderId":"O1","latitude":28,"longitude":77,"address":"Delhi","packageWeight":1,"priority":"URGENT"}]}
                  """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("priority must be one of HIGH, MEDIUM, LOW"));
    }
    @Test void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{bad-json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value("error"));
    }
    @Test void blankVehicleIdRejected() throws Exception {
        mvc.perform(post("/api/dispatch/vehicles").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"vehicles":[{"vehicleId":"","capacity":10,"currentLatitude":28,"currentLongitude":77,"currentAddress":"Delhi"}]}
                  """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value("error"));
    }
    @Test void unsupportedMethodReturns405() throws Exception {
        mvc.perform(put("/api/dispatch/orders")).andExpect(status().isMethodNotAllowed());
    }
}
