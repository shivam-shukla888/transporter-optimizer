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
import java.math.BigDecimal;

/**
 * Represents a submitted quote from a transporter for a specific lane in a snapshot.
 */
@Entity
@Table(name = "lane_quote")
public class LaneQuoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private DatasetSnapshotEntity snapshot;

    @Column(name = "lane_id", nullable = false)
    private Long laneId;

    @Column(name = "transporter_id", nullable = false)
    private Long transporterId;

    @Column(name = "quote_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal quoteAmount;

    public LaneQuoteEntity() {
    }

    public LaneQuoteEntity(DatasetSnapshotEntity snapshot, Long laneId, Long transporterId, BigDecimal quoteAmount) {
        this.snapshot = snapshot;
        this.laneId = laneId;
        this.transporterId = transporterId;
        this.quoteAmount = quoteAmount;
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

    public Long getTransporterId() {
        return transporterId;
    }

    public void setTransporterId(Long transporterId) {
        this.transporterId = transporterId;
    }

    public BigDecimal getQuoteAmount() {
        return quoteAmount;
    }

    public void setQuoteAmount(BigDecimal quoteAmount) {
        this.quoteAmount = quoteAmount;
    }
}
