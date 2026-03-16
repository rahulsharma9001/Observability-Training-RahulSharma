package com.nashtech.observability.service;

import com.nashtech.observability.dto.InventoryItemResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final int minDelayMs;
    private final int maxDelayMs;
    private final double errorRate;
    private final Tracer tracer;

    public InventoryServiceImpl(
            @Value("${app.simulation.min-delay-ms:150}") int minDelayMs,
            @Value("${app.simulation.max-delay-ms:900}") int maxDelayMs,
            @Value("${app.simulation.error-rate:0.30}") double errorRate,
            Tracer tracer
    ) {
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.errorRate = errorRate;
        this.tracer = tracer;
    }

    @Override
    public InventoryItemResponse fetchItem(String itemId) {
        Span validateStockSpan = tracer.nextSpan().name("validateStock").start();
        try (Tracer.SpanInScope spanInScope = tracer.withSpan(validateStockSpan)) {
            validateStockSpan.tag("item.id", itemId);
            log.debug("Running stock validation span for itemId={}", itemId);
            simulateRandomDelay();
            maybeThrowRandomError(itemId);
        } catch (RuntimeException ex) {
            validateStockSpan.error(ex);
            throw ex;
        } finally {
            validateStockSpan.end();
        }

        int quantity = ThreadLocalRandom.current().nextInt(0, 101);
        log.debug("Simulated successful inventory lookup for itemId={} with quantity={}", itemId, quantity);

        return new InventoryItemResponse(
                itemId,
                "item-" + itemId,
                quantity,
                Instant.now()
        );
    }

    private void simulateRandomDelay() {
        int delay = ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1);
        log.debug("Applying simulated processing delay of {} ms", delay);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted while simulating delay", ex);
        }
    }

    private void maybeThrowRandomError(String itemId) {
        boolean shouldFail = ThreadLocalRandom.current().nextDouble() < errorRate;
        if (shouldFail) {
            throw new IllegalStateException("Simulated inventory backend failure for itemId=" + itemId);
        }
    }
}
