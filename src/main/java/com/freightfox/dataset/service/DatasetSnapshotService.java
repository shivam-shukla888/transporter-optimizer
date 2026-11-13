package com.freightfox.dataset.service;

import com.freightfox.common.exception.InvalidDatasetException;
import com.freightfox.common.exception.NoActiveDatasetException;
import com.freightfox.dataset.dto.DatasetInputRequest;
import com.freightfox.dataset.dto.DatasetInputResponse;
import com.freightfox.dataset.dto.LaneInputDto;
import com.freightfox.dataset.dto.LaneQuoteInputDto;
import com.freightfox.dataset.dto.TransporterInputDto;
import com.freightfox.dataset.entity.DatasetSnapshotEntity;
import com.freightfox.dataset.entity.LaneEntity;
import com.freightfox.dataset.entity.LaneQuoteEntity;
import com.freightfox.dataset.entity.TransporterEntity;
import com.freightfox.dataset.repository.DatasetSnapshotRepository;
import com.freightfox.dataset.repository.LaneQuoteRepository;
import com.freightfox.dataset.repository.LaneRepository;
import com.freightfox.dataset.repository.TransporterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service managing dataset snapshot ingestion, cross-entity validation, and atomic snapshot activation.
 */
@Service
public class DatasetSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(DatasetSnapshotService.class);

    private final DatasetSnapshotRepository snapshotRepository;
    private final LaneRepository laneRepository;
    private final TransporterRepository transporterRepository;
    private final LaneQuoteRepository quoteRepository;

    public DatasetSnapshotService(
            DatasetSnapshotRepository snapshotRepository,
            LaneRepository laneRepository,
            TransporterRepository transporterRepository,
            LaneQuoteRepository quoteRepository
    ) {
        this.snapshotRepository = snapshotRepository;
        this.laneRepository = laneRepository;
        this.transporterRepository = transporterRepository;
        this.quoteRepository = quoteRepository;
    }

    @Transactional
    public DatasetInputResponse saveAndActivateDataset(DatasetInputRequest request) {
        validateDatasetPayload(request);

        // Deactivate currently active snapshot atomically within transaction
        snapshotRepository.deactivateAll();

        int totalLanes = request.lanes().size();
        int totalTransporters = request.transporters().size();
        int totalQuotes = request.transporters().stream()
                .mapToInt(t -> t.laneQuotes() != null ? t.laneQuotes().size() : 0)
                .sum();

        DatasetSnapshotEntity snapshot = new DatasetSnapshotEntity(true, totalLanes, totalTransporters, totalQuotes);
        snapshot = snapshotRepository.save(snapshot);

        // Persist lanes
        List<LaneEntity> laneEntities = new ArrayList<>(totalLanes);
        for (LaneInputDto l : request.lanes()) {
            laneEntities.add(new LaneEntity(snapshot, l.id(), l.origin(), l.destination()));
        }
        laneRepository.saveAll(laneEntities);

        // Persist transporters and quotes
        List<TransporterEntity> transporterEntities = new ArrayList<>(totalTransporters);
        List<LaneQuoteEntity> quoteEntities = new ArrayList<>(totalQuotes);

        for (TransporterInputDto t : request.transporters()) {
            transporterEntities.add(new TransporterEntity(snapshot, t.id(), t.name()));

            if (t.laneQuotes() != null) {
                for (LaneQuoteInputDto q : t.laneQuotes()) {
                    quoteEntities.add(new LaneQuoteEntity(snapshot, q.laneId(), t.id(), q.quote()));
                }
            }
        }

        transporterRepository.saveAll(transporterEntities);
        quoteRepository.saveAll(quoteEntities);

        log.info("Persisted and activated new dataset snapshot ID: {} ({} lanes, {} transporters, {} quotes)",
                snapshot.getId(), totalLanes, totalTransporters, totalQuotes);

        return new DatasetInputResponse("success", "Input data saved successfully.");
    }

    @Transactional(readOnly = true)
    public DatasetSnapshotEntity getActiveSnapshot() {
        return snapshotRepository.findByActiveTrue()
                .orElseThrow(() -> new NoActiveDatasetException("No active dataset snapshot found. Please submit dataset input first."));
    }

    private void validateDatasetPayload(DatasetInputRequest request) {
        if (request.lanes() == null || request.lanes().isEmpty()) {
            throw new InvalidDatasetException("Lanes list cannot be empty");
        }
        if (request.transporters() == null || request.transporters().isEmpty()) {
            throw new InvalidDatasetException("Transporters list cannot be empty");
        }

        Set<Long> laneIds = new HashSet<>();
        for (LaneInputDto lane : request.lanes()) {
            if (lane.id() == null) {
                throw new InvalidDatasetException("Lane ID cannot be null");
            }
            if (!laneIds.add(lane.id())) {
                throw new InvalidDatasetException("Duplicate lane ID encountered: " + lane.id());
            }
        }

        Set<Long> transporterIds = new HashSet<>();
        Set<String> quotePairs = new HashSet<>();

        for (TransporterInputDto transporter : request.transporters()) {
            if (transporter.id() == null) {
                throw new InvalidDatasetException("Transporter ID cannot be null");
            }
            if (!transporterIds.add(transporter.id())) {
                throw new InvalidDatasetException("Duplicate transporter ID encountered: " + transporter.id());
            }

            if (transporter.laneQuotes() != null) {
                for (LaneQuoteInputDto quote : transporter.laneQuotes()) {
                    if (quote.laneId() == null) {
                        throw new InvalidDatasetException("Quote lane ID cannot be null for transporter " + transporter.id());
                    }
                    if (!laneIds.contains(quote.laneId())) {
                        throw new InvalidDatasetException("Transporter " + transporter.id() +
                                " submitted quote for non-existent lane ID: " + quote.laneId());
                    }

                    String pairKey = quote.laneId() + ":" + transporter.id();
                    if (!quotePairs.add(pairKey)) {
                        throw new InvalidDatasetException("Duplicate quote for lane " + quote.laneId() +
                                " by transporter " + transporter.id());
                    }

                    if (quote.quote() == null) {
                        throw new InvalidDatasetException("Quote amount cannot be null");
                    }
                    if (quote.quote().compareTo(BigDecimal.ZERO) < 0) {
                        throw new InvalidDatasetException("Quote amount cannot be negative, got: " + quote.quote());
                    }
                }
            }
        }
    }
}
