package com.freightfox.dataset;

import com.freightfox.dataset.entity.DatasetSnapshotEntity;
import com.freightfox.dataset.entity.LaneEntity;
import com.freightfox.dataset.entity.LaneQuoteEntity;
import com.freightfox.dataset.entity.TransporterEntity;
import com.freightfox.dataset.repository.DatasetSnapshotRepository;
import com.freightfox.dataset.repository.LaneQuoteRepository;
import com.freightfox.dataset.repository.LaneRepository;
import com.freightfox.dataset.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class DatasetRepositoryIntegrationTest {

    @Autowired
    private DatasetSnapshotRepository snapshotRepository;

    @Autowired
    private LaneRepository laneRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    @Autowired
    private LaneQuoteRepository quoteRepository;

    @BeforeEach
    void setUp() {
        quoteRepository.deleteAll();
        laneRepository.deleteAll();
        transporterRepository.deleteAll();
        snapshotRepository.deleteAll();
    }

    @Test
    @DisplayName("Persist a complete dataset snapshot and query active snapshot")
    void testPersistAndQueryActiveSnapshot() {
        DatasetSnapshotEntity snapshot = new DatasetSnapshotEntity(true, 2, 2, 4);
        snapshot = snapshotRepository.saveAndFlush(snapshot);

        LaneEntity lane1 = new LaneEntity(snapshot, 1L, "Mumbai", "Delhi");
        LaneEntity lane2 = new LaneEntity(snapshot, 2L, "Chennai", "Kolkata");
        laneRepository.saveAllAndFlush(List.of(lane1, lane2));

        TransporterEntity t1 = new TransporterEntity(snapshot, 1L, "Transporter T1");
        TransporterEntity t2 = new TransporterEntity(snapshot, 2L, "Transporter T2");
        transporterRepository.saveAllAndFlush(List.of(t1, t2));

        LaneQuoteEntity q1 = new LaneQuoteEntity(snapshot, 1L, 1L, new BigDecimal("5000.00"));
        LaneQuoteEntity q2 = new LaneQuoteEntity(snapshot, 1L, 2L, new BigDecimal("6000.00"));
        LaneQuoteEntity q3 = new LaneQuoteEntity(snapshot, 2L, 1L, new BigDecimal("7500.00"));
        LaneQuoteEntity q4 = new LaneQuoteEntity(snapshot, 2L, 2L, new BigDecimal("7000.00"));
        quoteRepository.saveAllAndFlush(List.of(q1, q2, q3, q4));

        Optional<DatasetSnapshotEntity> activeOpt = snapshotRepository.findByActiveTrue();
        assertThat(activeOpt).isPresent();
        assertThat(activeOpt.get().getId()).isEqualTo(snapshot.getId());

        List<LaneEntity> lanes = laneRepository.findBySnapshotIdOrderByLaneIdAsc(snapshot.getId());
        assertThat(lanes).hasSize(2);
        assertThat(lanes.get(0).getLaneId()).isEqualTo(1L);

        List<TransporterEntity> transporters = transporterRepository.findBySnapshotIdOrderByTransporterIdAsc(snapshot.getId());
        assertThat(transporters).hasSize(2);

        List<LaneQuoteEntity> quotes = quoteRepository.findBySnapshotId(snapshot.getId());
        assertThat(quotes).hasSize(4);
    }

    @Test
    @DisplayName("Enforce unique constraint on duplicate lane in same snapshot")
    void testDuplicateLaneInSameSnapshotRejected() {
        DatasetSnapshotEntity snapshot = snapshotRepository.saveAndFlush(new DatasetSnapshotEntity(false, 1, 1, 1));
        laneRepository.saveAndFlush(new LaneEntity(snapshot, 1L, "A", "B"));

        assertThatThrownBy(() -> {
            laneRepository.saveAndFlush(new LaneEntity(snapshot, 1L, "A", "C"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Enforce check constraint on negative quote amounts")
    void testNegativeQuoteRejected() {
        DatasetSnapshotEntity snapshot = snapshotRepository.saveAndFlush(new DatasetSnapshotEntity(false, 1, 1, 1));

        assertThatThrownBy(() -> {
            quoteRepository.saveAndFlush(new LaneQuoteEntity(snapshot, 1L, 1L, new BigDecimal("-10.00")));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
