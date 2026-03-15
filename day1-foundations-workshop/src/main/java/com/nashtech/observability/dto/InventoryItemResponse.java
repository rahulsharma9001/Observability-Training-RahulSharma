package com.nashtech.observability.dto;

import java.time.Instant;

public record InventoryItemResponse(
        String itemId,
        String itemName,
        int availableQuantity,
        Instant timestamp
) {
}
