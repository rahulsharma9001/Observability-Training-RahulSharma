package com.nashtech.observability.controller;

import com.nashtech.observability.dto.InventoryItemResponse;
import com.nashtech.observability.metrics.RequestMetrics;
import com.nashtech.observability.service.InventoryService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void shouldIncrementSuccessCounterWhenRequestSucceeds() throws Exception {
        double before = counterValue("success");
        when(inventoryService.fetchItem("42"))
                .thenReturn(new InventoryItemResponse("42", "item-42", 12, Instant.now()));

        mockMvc.perform(get("/api/items/42"))
                .andExpect(status().isOk());

        double after = counterValue("success");
        assertThat(after).isEqualTo(before + 1.0);
    }

    @Test
    void shouldIncrementErrorCounterWhenRequestFails() throws Exception {
        double before = counterValue("error");
        when(inventoryService.fetchItem("99"))
                .thenThrow(new IllegalStateException("simulated-error"));

        mockMvc.perform(get("/api/items/99"))
                .andExpect(status().isInternalServerError());

        double after = counterValue("error");
        assertThat(after).isEqualTo(before + 1.0);
    }

    private double counterValue(String status) {
        var counter = meterRegistry.find(RequestMetrics.METRIC_NAME)
                .tag("status", status)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }
}
