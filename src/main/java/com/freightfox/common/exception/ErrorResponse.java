package com.freightfox.common.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String status,
        String message,
        List<String> details,
        Instant timestamp
) {
    public static ErrorResponse of(String message, List<String> details) {
        return new ErrorResponse("error", message, details, Instant.now());
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse("error", message, List.of(), Instant.now());
    }
}
