package com.freightfox.optimizer.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Result returned by the optimization engine.
 */
public record OptimizationResult(
        boolean feasible,
        BigDecimal totalCost,
        List<Long> selectedTransporters,
        List<LaneAssignment> assignments,
        String failureReason
) {

    public static OptimizationResult success(BigDecimal totalCost, List<Long> selectedTransporters, List<LaneAssignment> assignments) {
        return new OptimizationResult(true, totalCost, Collections.unmodifiableList(selectedTransporters), Collections.unmodifiableList(assignments), null);
    }

    public static OptimizationResult infeasible(String reason) {
        return new OptimizationResult(false, BigDecimal.ZERO, Collections.emptyList(), Collections.emptyList(), reason);
    }
}
