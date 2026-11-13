package com.freightfox.assignment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AssignmentRequestDto(
        @NotNull(message = "maxTransporters is required")
        @Min(value = 1, message = "maxTransporters must be at least 1")
        Integer maxTransporters
) {}
