package com.freightfox.dataset.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LaneInputDto(
        @NotNull(message = "Lane ID is required")
        @Positive(message = "Lane ID must be positive")
        Long id,

        String origin,
        String destination
) {}
