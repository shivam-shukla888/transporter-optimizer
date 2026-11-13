package com.freightfox.assignment.dto;

import java.math.BigDecimal;
import java.util.List;

public record AssignmentResponseDto(
        String status,
        BigDecimal totalCost,
        List<LaneAssignmentDto> assignments,
        List<Long> selectedTransporters
) {}
