package com.freightfox.optimizer.model;

import java.util.Objects;

/**
 * Pure domain model representing a shipping lane.
 * Framework-independent and immutable.
 */
public record Lane(long id, String origin, String destination) implements Comparable<Lane> {

    public Lane {
        if (id <= 0) {
            throw new IllegalArgumentException("Lane ID must be positive, got: " + id);
        }
    }

    public Lane(long id) {
        this(id, null, null);
    }

    @Override
    public int compareTo(Lane other) {
        return Long.compare(this.id, other.id);
    }
}
