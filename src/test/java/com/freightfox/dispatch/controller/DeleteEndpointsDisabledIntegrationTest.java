package com.freightfox.dispatch.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.delete-endpoints-enabled=false")
@AutoConfigureMockMvc
class DeleteEndpointsDisabledIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void allDeleteRoutesReturnStructuredForbiddenResponse() throws Exception {
        mvc.perform(delete("/api/dispatch/orders"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"));
        mvc.perform(delete("/api/dispatch/orders/some-id"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/dispatch/vehicles"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/dispatch/vehicles/some-id"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk());
    }
}
