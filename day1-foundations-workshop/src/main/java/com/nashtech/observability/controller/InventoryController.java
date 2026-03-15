package com.nashtech.observability.controller;

import com.nashtech.observability.dto.InventoryItemResponse;
import com.nashtech.observability.metrics.RequestMetrics;
import com.nashtech.observability.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    private final InventoryService inventoryService;
    private final RequestMetrics requestMetrics;

    public InventoryController(InventoryService inventoryService, RequestMetrics requestMetrics) {
        this.inventoryService = inventoryService;
        this.requestMetrics = requestMetrics;
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<InventoryItemResponse> getItem(@PathVariable String itemId) {
        long startNanos = System.nanoTime();
        MDC.put("itemId", itemId);
        log.info("Received inventory lookup request");

        try {
            InventoryItemResponse response = inventoryService.fetchItem(itemId);
            requestMetrics.increment("success");
            log.debug("Inventory lookup completed successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            requestMetrics.increment("error");
            log.error("Inventory lookup failed", ex);
            throw ex;
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("Inventory lookup finished in {} ms", elapsedMs);
            MDC.remove("itemId");
        }
    }
}
