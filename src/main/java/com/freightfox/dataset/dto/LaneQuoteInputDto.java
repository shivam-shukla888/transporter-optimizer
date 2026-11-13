package com.freightfox.dataset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record LaneQuoteInputDto(
        @NotNull(message = "Lane ID is required")
        @Positive(message = "Lane ID must be positive")
        Long laneId,

        @NotNull(message = "Quote is required")
        @DecimalMin(value = "0.00", message = "Quote cannot be negative")
        BigDecimal quote
) {}
