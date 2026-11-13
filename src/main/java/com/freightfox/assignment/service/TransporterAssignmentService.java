package com.freightfox.assignment.service;

import com.freightfox.assignment.dto.AssignmentResponseDto;
import com.freightfox.assignment.dto.LaneAssignmentDto;
import com.freightfox.common.exception.InfeasibleAssignmentException;
import com.freightfox.dataset.entity.DatasetSnapshotEntity;
import com.freightfox.dataset.entity.LaneEntity;
import com.freightfox.dataset.entity.LaneQuoteEntity;
import com.freightfox.dataset.entity.TransporterEntity;
import com.freightfox.dataset.repository.LaneQuoteRepository;
import com.freightfox.dataset.repository.LaneRepository;
import com.freightfox.dataset.repository.TransporterRepository;
import com.freightfox.dataset.service.DatasetSnapshotService;
import com.freightfox.optimizer.engine.AssignmentOptimizer;
import com.freightfox.optimizer.model.Lane;
import com.freightfox.optimizer.model.LaneQuote;
import com.freightfox.optimizer.model.OptimizationRequest;
import com.freightfox.optimizer.model.OptimizationResult;
import com.freightfox.optimizer.model.Transporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrator service for reading the active dataset snapshot and running the pure Java
 * optimization engine to produce the optimal transporter-to-lane assignment.
 */
@Service
public class TransporterAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TransporterAssignmentService.class);

    private final DatasetSnapshotService datasetSnapshotService;
    private final LaneRepository laneRepository;
    private final TransporterRepository transporterRepository;
    private final LaneQuoteRepository quoteRepository;
    private final AssignmentOptimizer optimizer;

    public TransporterAssignmentService(
            DatasetSnapshotService datasetSnapshotService,
            LaneRepository laneRepository,
            TransporterRepository transporterRepository,
            LaneQuoteRepository quoteRepository
    ) {
        this.datasetSnapshotService = datasetSnapshotService;
        this.laneRepository = laneRepository;
        this.transporterRepository = transporterRepository;
        this.quoteRepository = quoteRepository;
        this.optimizer = new AssignmentOptimizer();
    }

    @Transactional(readOnly = true)
    public AssignmentResponseDto optimizeAssignment(int maxTransporters) {
        DatasetSnapshotEntity snapshot = datasetSnapshotService.getActiveSnapshot();
        Long snapshotId = snapshot.getId();

        log.info("Running optimization for active snapshot ID: {} with maxTransporters: {}", snapshotId, maxTransporters);

        List<LaneEntity> laneEntities = laneRepository.findBySnapshotIdOrderByLaneIdAsc(snapshotId);
        List<TransporterEntity> transporterEntities = transporterRepository.findBySnapshotIdOrderByTransporterIdAsc(snapshotId);
        List<LaneQuoteEntity> quoteEntities = quoteRepository.findBySnapshotIdOrderByLaneIdAscTransporterIdAsc(snapshotId);

        List<Lane> lanes = laneEntities.stream()
                .map(l -> new Lane(l.getLaneId(), l.getOrigin(), l.getDestination()))
                .toList();

        List<Transporter> transporters = transporterEntities.stream()
                .map(t -> new Transporter(t.getTransporterId(), t.getName()))
                .toList();

        List<LaneQuote> quotes = quoteEntities.stream()
                .map(q -> new LaneQuote(q.getLaneId(), q.getTransporterId(), q.getQuoteAmount()))
                .toList();

        OptimizationRequest request = new OptimizationRequest(lanes, transporters, quotes, maxTransporters);
        OptimizationResult result = optimizer.optimize(request);

        if (!result.feasible()) {
            log.warn("Optimization infeasible for snapshot {}: {}", snapshotId, result.failureReason());
            throw new InfeasibleAssignmentException(result.failureReason());
        }

        List<LaneAssignmentDto> assignmentDtos = result.assignments().stream()
                .map(a -> new LaneAssignmentDto(a.laneId(), a.transporterId()))
                .toList();

        log.info("Optimization succeeded for snapshot {}. Total cost: {}, selected transporters: {}",
                snapshotId, result.totalCost(), result.selectedTransporters());

        return new AssignmentResponseDto("success", result.totalCost(), assignmentDtos, result.selectedTransporters());
    }
}
