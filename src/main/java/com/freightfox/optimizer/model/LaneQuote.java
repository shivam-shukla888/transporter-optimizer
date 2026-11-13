package com.freightfox.optimizer.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Pure domain model representing a submitted quote for a lane from a transporter.
 * Monetary amount is represented as a normalized BigDecimal with scale 2 to avoid precision loss.
 */
public record LaneQuote(long laneId, long transporterId, BigDecimal quoteAmount) {

    public LaneQuote {
        if (laneId <= 0) {
            throw new IllegalArgumentException("Lane ID must be positive, got: " + laneId);
        }
        if (transporterId <= 0) {
            throw new IllegalArgumentException("Transporter ID must be positive, got: " + transporterId);
        }
        Objects.requireNonNull(quoteAmount, "Quote amount cannot be null");
        if (quoteAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quote amount cannot be negative, got: " + quoteAmount);
        }
        quoteAmount = quoteAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public LaneQuote(long laneId, long transporterId, long quoteAmount) {
        this(laneId, transporterId, BigDecimal.valueOf(quoteAmount).setScale(2, RoundingMode.HALF_UP));
    }
}
