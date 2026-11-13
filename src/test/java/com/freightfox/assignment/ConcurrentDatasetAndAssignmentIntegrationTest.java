package com.freightfox.assignment;

import com.freightfox.assignment.dto.AssignmentResponseDto;
import com.freightfox.assignment.service.TransporterAssignmentService;
import com.freightfox.dataset.dto.DatasetInputRequest;
import com.freightfox.dataset.dto.LaneInputDto;
import com.freightfox.dataset.dto.LaneQuoteInputDto;
import com.freightfox.dataset.dto.TransporterInputDto;
import com.freightfox.dataset.repository.DatasetSnapshotRepository;
import com.freightfox.dataset.repository.LaneQuoteRepository;
import com.freightfox.dataset.repository.LaneRepository;
import com.freightfox.dataset.repository.TransporterRepository;
import com.freightfox.dataset.service.DatasetSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class ConcurrentDatasetAndAssignmentIntegrationTest {

    @Autowired
    private DatasetSnapshotService datasetSnapshotService;

    @Autowired
    private TransporterAssignmentService assignmentService;

    @Autowired
    private DatasetSnapshotRepository snapshotRepository;

    @Autowired
    private LaneRepository laneRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    @Autowired
    private LaneQuoteRepository quoteRepository;

    @BeforeEach
    void cleanUp() {
        quoteRepository.deleteAll();
        laneRepository.deleteAll();
        transporterRepository.deleteAll();
        snapshotRepository.deleteAll();
    }

    @Test
    @DisplayName("REQ-12 & REQ-13: Concurrent assignment reads while activating new snapshots never observe corrupted/mixed states")
    void testConcurrentReadAndSnapshotActivation() throws Exception {
        // Initial dataset A (Cost: 100)
        DatasetInputRequest datasetA = new DatasetInputRequest(
                List.of(new LaneInputDto(1L, "A1", "A2")),
                List.of(new TransporterInputDto(1L, "T1", List.of(new LaneQuoteInputDto(1L, new BigDecimal("100.00")))))
        );
        datasetSnapshotService.saveAndActivateDataset(datasetA);

        int numThreads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<Callable<BigDecimal>> readTasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            readTasks.add(() -> {
                AssignmentResponseDto response = assignmentService.optimizeAssignment(1);
                return response.totalCost();
            });
        }

        // Concurrently submit dataset B (Cost: 500)
        Callable<Void> writeTask = () -> {
            DatasetInputRequest datasetB = new DatasetInputRequest(
                    List.of(new LaneInputDto(2L, "B1", "B2")),
                    List.of(new TransporterInputDto(2L, "T2", List.of(new LaneQuoteInputDto(2L, new BigDecimal("500.00")))))
            );
            datasetSnapshotService.saveAndActivateDataset(datasetB);
            return null;
        };

        Future<Void> writeFuture = executor.submit(writeTask);
        List<Future<BigDecimal>> readFutures = executor.invokeAll(readTasks);

        writeFuture.get();

        // Every read task must have observed EITHER exactly 100.00 (Dataset A) OR exactly 500.00 (Dataset B).
        // Never null, never mixed, never corrupted!
        for (Future<BigDecimal> future : readFutures) {
            BigDecimal cost = future.get();
            assertThat(cost)
                    .satisfiesAnyOf(
                            c -> assertThat(c).isEqualByComparingTo("100.00"),
                            c -> assertThat(c).isEqualByComparingTo("500.00")
                    );
        }

        executor.shutdown();
    }
}
