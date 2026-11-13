package com.freightfox.dataset.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record DatasetInputRequest(
        @Valid
        @NotEmpty(message = "Lanes list cannot be empty")
        List<LaneInputDto> lanes,

        @Valid
        @NotEmpty(message = "Transporters list cannot be empty")
        List<TransporterInputDto> transporters
) {}
