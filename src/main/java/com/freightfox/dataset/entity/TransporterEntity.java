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
 * Represents a transporter entity within a dataset snapshot.
 */
@Entity
@Table(name = "transporter")
public class TransporterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private DatasetSnapshotEntity snapshot;

    @Column(name = "transporter_id", nullable = false)
    private Long transporterId;

    @Column(name = "name")
    private String name;

    public TransporterEntity() {
    }

    public TransporterEntity(DatasetSnapshotEntity snapshot, Long transporterId, String name) {
        this.snapshot = snapshot;
        this.transporterId = transporterId;
        this.name = name;
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

    public Long getTransporterId() {
        return transporterId;
    }

    public void setTransporterId(Long transporterId) {
        this.transporterId = transporterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
