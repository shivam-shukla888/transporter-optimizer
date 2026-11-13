-- V1__init_schema.sql: Initial schema for FreightFox Transporter Assignment Optimizer

CREATE TABLE dataset_snapshot (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    total_lanes INT NOT NULL DEFAULT 0,
    total_transporters INT NOT NULL DEFAULT 0,
    total_quotes INT NOT NULL DEFAULT 0
);

CREATE TABLE lane (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES dataset_snapshot(id) ON DELETE CASCADE,
    lane_id BIGINT NOT NULL,
    origin VARCHAR(255),
    destination VARCHAR(255),
    CONSTRAINT uq_lane_snapshot UNIQUE (snapshot_id, lane_id)
);

CREATE TABLE transporter (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES dataset_snapshot(id) ON DELETE CASCADE,
    transporter_id BIGINT NOT NULL,
    name VARCHAR(255),
    CONSTRAINT uq_transporter_snapshot UNIQUE (snapshot_id, transporter_id)
);

CREATE TABLE lane_quote (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES dataset_snapshot(id) ON DELETE CASCADE,
    lane_id BIGINT NOT NULL,
    transporter_id BIGINT NOT NULL,
    quote_amount NUMERIC(15, 2) NOT NULL CHECK (quote_amount >= 0),
    CONSTRAINT uq_quote_lane_transporter UNIQUE (snapshot_id, lane_id, transporter_id)
);

CREATE INDEX idx_snapshot_active ON dataset_snapshot(active);
CREATE INDEX idx_lane_snapshot ON lane(snapshot_id);
CREATE INDEX idx_transporter_snapshot ON transporter(snapshot_id);
CREATE INDEX idx_quote_snapshot_lane ON lane_quote(snapshot_id, lane_id);
CREATE INDEX idx_quote_snapshot_transporter ON lane_quote(snapshot_id, transporter_id);
