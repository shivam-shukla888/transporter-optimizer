package com.freightfox.dataset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a shipping lane within a dataset snapshot.
 */
@Entity
@Table(name = "lane")
public class LaneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private DatasetSnapshotEntity snapshot;

    @Column(name = "lane_id", nullable = false)
    private Long laneId;

    @Column(name = "origin")
    private String origin;

    @Column(name = "destination")
    private String destination;

    public LaneEntity() {
    }

    public LaneEntity(DatasetSnapshotEntity snapshot, Long laneId, String origin, String destination) {
        this.snapshot = snapshot;
        this.laneId = laneId;
        this.origin = origin;
        this.destination = destination;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DatasetSnapshotEntity getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(DatasetSnapshotEntity snapshot) {
        this.snapshot = snapshot;
    }

    public Long getLaneId() {
        return laneId;
    }

    public void setLaneId(Long laneId) {
        this.laneId = laneId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
