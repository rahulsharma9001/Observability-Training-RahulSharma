package com.nashtech.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RequestMetrics {

    public static final String METRIC_NAME = "inventory_requests_total";
    private final MeterRegistry meterRegistry;

    public RequestMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void increment(String status) {
        Counter.builder(METRIC_NAME)
                .description("Total inventory requests by outcome")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }
}
