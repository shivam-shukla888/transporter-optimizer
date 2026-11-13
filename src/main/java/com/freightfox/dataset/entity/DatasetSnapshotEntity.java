package com.freightfox.dataset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Represents an immutable snapshot of an ingested dataset (lanes, transporters, quotes).
 * Only one snapshot may be active at any given time.
 */
@Entity
@Table(name = "dataset_snapshot")
public class DatasetSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "active", nullable = false)
    private boolean active = false;

    @Column(name = "total_lanes", nullable = false)
    private int totalLanes;

    @Column(name = "total_transporters", nullable = false)
    private int totalTransporters;

    @Column(name = "total_quotes", nullable = false)
    private int totalQuotes;

    public DatasetSnapshotEntity() {
    }

    public DatasetSnapshotEntity(boolean active, int totalLanes, int totalTransporters, int totalQuotes) {
        this.createdAt = Instant.now();
        this.active = active;
        this.totalLanes = totalLanes;
        this.totalTransporters = totalTransporters;
        this.totalQuotes = totalQuotes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getTotalLanes() {
        return totalLanes;
    }

    public void setTotalLanes(int totalLanes) {
        this.totalLanes = totalLanes;
    }

    public int getTotalTransporters() {
        return totalTransporters;
    }

    public void setTotalTransporters(int totalTransporters) {
        this.totalTransporters = totalTransporters;
    }

    public int getTotalQuotes() {
        return totalQuotes;
    }

    public void setTotalQuotes(int totalQuotes) {
        this.totalQuotes = totalQuotes;
    }
}
