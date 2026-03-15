package com.nashtech.observability.dto;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String correlationId,
        Instant timestamp
) {
}
