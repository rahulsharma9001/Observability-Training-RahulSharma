package com.nashtech.observability.service;

import com.nashtech.observability.dto.InventoryItemResponse;

public interface InventoryService {
    InventoryItemResponse fetchItem(String itemId);
}
