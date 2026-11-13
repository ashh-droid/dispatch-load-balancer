package com.freightfox.dispatch.controller;

import com.freightfox.dispatch.service.OrderService;
import com.freightfox.dispatch.service.VehicleService;
import com.freightfox.dispatch.service.DispatchService;
import com.freightfox.dispatch.exception.DuplicateIdException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DispatchHttpErrorsTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService orders;
    @MockitoBean VehicleService vehicles;
    @MockitoBean DispatchService dispatch;
    private final String valid = """
        {"orders":[{"orderId":"O1","latitude":28,"longitude":77,"address":"Delhi","packageWeight":1,"priority":"HIGH"}]}
        """;

    @Test void wrongRequestContentTypeIs415RatherThan500() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.TEXT_PLAIN).content(valid))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value("error"));
    }
    @Test void unacceptableResponseTypeIs406() throws Exception {
        mvc.perform(get("/api/dispatch/plan").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }
    @Test void validJsonWithWrongTypeReportsField() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON)
                .content(valid.replace("\"packageWeight\":1", "\"packageWeight\":\"abc\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("orders[0].packageWeight"));
    }
    @Test void invalidPageNumberTypeIs400() throws Exception {
        mvc.perform(get("/api/dispatch/orders?page=abc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value("error"));
    }
    @Test void blankBodyIs400() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
    @Test void unknownPathIs404() throws Exception {
        mvc.perform(get("/api/dispatch/not-a-resource")).andExpect(status().isNotFound());
    }
    @Test void invalidStrategyIs400() throws Exception {
        doThrow(new com.freightfox.dispatch.exception.InvalidStrategyException("strategy must be one of greedy, distance-aware"))
                .when(dispatch).plan("unknown");
        mvc.perform(get("/api/dispatch/plan?strategy=unknown"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value("error"));
    }
    @Test void duplicatesAre400() throws Exception {
        doThrow(new DuplicateIdException("Duplicate orderId in request: O1")).when(orders).accept(any());
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Duplicate orderId in request: O1"));
    }
    @Test void whitespaceInBusinessIdIsRejected() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON)
                .content(valid.replace("\"O1\"", "\" O1\"")))
                .andExpect(status().isBadRequest());
    }
    @Test void oversizedListIsRejected() throws Exception {
        String item = "{\"orderId\":\"O1\",\"latitude\":28,\"longitude\":77,\"address\":\"Delhi\",\"packageWeight\":1,\"priority\":\"HIGH\"}";
        String body = "{\"orders\":[" + (item + ",").repeat(10000) + item + "]}";
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
