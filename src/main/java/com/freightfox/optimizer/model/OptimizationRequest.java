package com.freightfox.optimizer.model;

import java.util.List;
import java.util.Objects;

/**
 * Pure domain request passed to the optimization engine.
 */
public record OptimizationRequest(
        List<Lane> lanes,
        List<Transporter> transporters,
        List<LaneQuote> quotes,
        int maxTransporters
) {

    public OptimizationRequest {
        Objects.requireNonNull(lanes, "Lanes list cannot be null");
        Objects.requireNonNull(transporters, "Transporters list cannot be null");
        Objects.requireNonNull(quotes, "Quotes list cannot be null");
        if (maxTransporters <= 0) {
            throw new IllegalArgumentException("maxTransporters must be at least 1, got: " + maxTransporters);
        }
    }
}
