package com.freightfox.optimizer.model;

/**
 * Pure domain model representing a transporter.
 * Framework-independent and immutable.
 */
public record Transporter(long id, String name) implements Comparable<Transporter> {

    public Transporter {
        if (id <= 0) {
            throw new IllegalArgumentException("Transporter ID must be positive, got: " + id);
        }
    }

    public Transporter(long id) {
        this(id, null);
    }

    @Override
    public int compareTo(Transporter other) {
        return Long.compare(this.id, other.id);
    }
}
