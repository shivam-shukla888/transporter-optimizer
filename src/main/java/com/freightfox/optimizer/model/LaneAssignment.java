package com.freightfox.optimizer.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents the assignment of a single lane to a chosen transporter with the corresponding quote cost.
 */
public record LaneAssignment(long laneId, long transporterId, BigDecimal cost) implements Comparable<LaneAssignment> {

    public LaneAssignment {
        if (laneId <= 0) {
            throw new IllegalArgumentException("Lane ID must be positive, got: " + laneId);
        }
        if (transporterId <= 0) {
            throw new IllegalArgumentException("Transporter ID must be positive, got: " + transporterId);
        }
        Objects.requireNonNull(cost, "Cost cannot be null");
    }

    @Override
    public int compareTo(LaneAssignment other) {
        int laneCmp = Long.compare(this.laneId, other.laneId);
        if (laneCmp != 0) {
            return laneCmp;
        }
        return Long.compare(this.transporterId, other.transporterId);
    }
}
