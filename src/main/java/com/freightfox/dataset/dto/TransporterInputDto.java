package com.freightfox.dataset.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record TransporterInputDto(
        @NotNull(message = "Transporter ID is required")
        @Positive(message = "Transporter ID must be positive")
        Long id,

        String name,

        @Valid
        @NotNull(message = "laneQuotes cannot be null")
        List<LaneQuoteInputDto> laneQuotes
) {}
