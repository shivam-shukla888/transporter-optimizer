package com.freightfox.optimizer;

import com.freightfox.optimizer.engine.AssignmentOptimizer;
import com.freightfox.optimizer.model.Lane;
import com.freightfox.optimizer.model.LaneAssignment;
import com.freightfox.optimizer.model.LaneQuote;
import com.freightfox.optimizer.model.OptimizationRequest;
import com.freightfox.optimizer.model.OptimizationResult;
import com.freightfox.optimizer.model.Transporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignmentOptimizerTest {

    private AssignmentOptimizer optimizer;

    @BeforeEach
    void setUp() {
        optimizer = new AssignmentOptimizer();
    }

    @Test
    @DisplayName("REQ-01: Maximize distinct transporter count over cheaper single transporter")
    void testMaximizeTransporters() {
        // Lane 1: T1=100, T2=500
        // Lane 2: T1=100, T2=500
        // If maxTransporters = 2:
        // Using T1 alone gives cost 200 (1 transporter).
        // Using T1 and T2 gives cost 600 (2 transporters).
        // Under our primary objective (maximize transporters), it MUST choose 2 transporters with cost 600.
        List<Lane> lanes = List.of(new Lane(1), new Lane(2));
        List<Transporter> transporters = List.of(new Transporter(1), new Transporter(2));
        List<LaneQuote> quotes = List.of(
                new LaneQuote(1, 1, 100),
                new LaneQuote(1, 2, 500),
                new LaneQuote(2, 1, 100),
                new LaneQuote(2, 2, 500)
        );

        OptimizationRequest request = new OptimizationRequest(lanes, transporters, quotes, 2);
        OptimizationResult result = optimizer.optimize(request);

        assertThat(result.feasible()).isTrue();
        assertThat(result.selectedTransporters()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(result.totalCost()).isEqualByComparingTo("600.00");
        assertThat(result.assignments()).hasSize(2);
    }

    @Test
    @DisplayName("REQ-02: Minimize cost among solutions with equal maximal transporter count")
    void testCostMinimizationAmongEqualTransporters() {
        // Lane 1: T1=50, T2=100, T3=200
        // Lane 2: T1=100, T2=60, T3=300
        // maxTransporters = 2
        // Subsets of size 2:
        // {T1, T2}: Lane1->T1 (50), Lane2->T2 (60) = 110
        // {T1, T3}: Lane1->T1 (50), Lane2->T3 (300) = 350
        // {T2, T3}: Lane1->T2 (100), Lane2->T3 (300) = 400
        // Should choose {T1, T2} with cost 110.
        List<Lane> lanes = List.of(new Lane(1), new Lane(2));
        List<Transporter> transporters = List.of(new Transporter(1), new Transporter(2), new Transporter(3));
        List<LaneQuote> quotes = List.of(
                new LaneQuote(1, 1, 50),
                new LaneQuote(1, 2, 100),
                new LaneQuote(1, 3, 200),
                new LaneQuote(2, 1, 100),
                new LaneQuote(2, 2, 60),
                new LaneQuote(2, 3, 300)
        );

        OptimizationRequest request = new OptimizationRequest(lanes, transporters, quotes, 2);
        OptimizationResult result = optimizer.optimize(request);

        assertThat(result.feasible()).isTrue();
        assertThat(result.selectedTransporters()).containsExactly(1L, 2L);
        assertThat(result.totalCost()).isEqualByComparingTo("110.00");
    }

    @Test
    @DisplayName("REQ-03 & REQ-04: Every lane assigned exactly once and every selected transporter used")
    void testAllSelectedTransportersUsedAndEveryLaneAssignedOnce() {
        List<Lane> lanes = List.of(new Lane(1), new Lane(2), new Lane(3));
        List<Transporter> transporters = List.of(new Transporter(1), new Transporter(2), new Transporter(3));
        List<LaneQuote> quotes = List.of(
                new LaneQuote(1, 1, 10), new LaneQuote(1, 2, 20),
                new LaneQuote(2, 2, 15), new LaneQuote(2, 3, 25),
                new LaneQuote(3, 1, 30), new LaneQuote(3, 3, 12)
        );

        OptimizationResult result = optimizer.optimize(new OptimizationRequest(lanes, transporters, quotes, 3));
        assertThat(result.feasible()).isTrue();

        Set<Long> assignedLanes = result.assignments().stream().map(LaneAssignment::laneId).collect(Collectors.toSet());
        assertThat(assignedLanes).containsExactlyInAnyOrder(1L, 2L, 3L);

        Set<Long> usedTransporters = result.assignments().stream().map(LaneAssignment::transporterId).collect(Collectors.toSet());
        assertThat(usedTransporters).isEqualTo(Set.copyOf(result.selectedTransporters()));

        BigDecimal sumCost = result.assignments().stream()
                .map(LaneAssignment::cost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(result.totalCost()).isEqualByComparingTo(sumCost);
    }

    @Test
    @DisplayName("REQ-05: Missing quotes are never fabricated")
    void testMissingQuotesNeverAssigned() {
        // Lane 1 quoted only by T1
        // Lane 2 quoted only by T2
        // maxTransporters = 1 -> Cannot use 1 transporter because neither T1 nor T2 quoted both lanes
        List<Lane> lanes = List.of(new Lane(1), new Lane(2));
        List<Transporter> transporters = List.of(new Transporter(1), new Transporter(2));
        List<LaneQuote> quotes = List.of(
                new LaneQuote(1, 1, 100),
                new LaneQuote(2, 2, 200)
        );

        OptimizationResult resultForK1 = optimizer.optimize(new OptimizationRequest(lanes, transporters, quotes, 1));
        assertThat(resultForK1.feasible()).isFalse();

        OptimizationResult resultForK2 = optimizer.optimize(new OptimizationRequest(lanes, transporters, quotes, 2));
        assertThat(resultForK2.feasible()).isTrue();
        assertThat(resultForK2.totalCost()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("REQ-06: Infeasible assignment when a lane has no submitted quotes")
    void testInfeasibleWhenLaneHasNoQuotes() {
        List<Lane> lanes = List.of(new Lane(1), new Lane(2));
        List<Transporter> transporters = List.of(new Transporter(1));
        List<LaneQuote> quotes = List.of(new LaneQuote(1, 1, 100)); // Lane 2 has no quote

        OptimizationResult result = optimizer.optimize(new OptimizationRequest(lanes, transporters, quotes, 1));
        assertThat(result.feasible()).isFalse();
        assertThat(result.failureReason()).contains("Lane ID 2 has no submitted quotes");
    }

    @Test
    @DisplayName("REQ-07: Deterministic tie-breaking on identical costs")
    void testDeterministicTieBreaking() {
        // T1 and T2 quote identical costs on both lanes
        List<Lane> lanes = List.of(new Lane(1), new Lane(2));
        List<Transporter> transporters = List.of(new Transporter(1), new Transporter(2));
        List<LaneQuote> quotes = List.of(
                new LaneQuote(1, 1, 50), new LaneQuote(1, 2, 50),
                new LaneQuote(2, 1, 50), new LaneQuote(2, 2, 50)
        );

        // Run multiple times, result must be identical
        OptimizationResult r1 = optimizer.optimize(new OptimizationRequest(lanes, transporters, quotes, 1));
        OptimizationResult r2 = optimizer.optimize(new OptimizationRequest(lanes, transporters, quotes, 1));

        assertThat(r1.selectedTransporters()).containsExactly(1L);
        assertThat(r2.selectedTransporters()).containsExactly(1L);
    }

    @Test
    @DisplayName("REQ-08: Monetary precision and no floating point overflow")
    void testMonetaryPrecisionAndNoOverflow() {
        List<Lane> lanes = List.of(new Lane(1));
        List<Transporter> transporters = List.of(new Transporter(1));
        BigDecimal largeCost = new BigDecimal("999999999999.99");
        List<LaneQuote> quotes = List.of(new LaneQuote(1, 1, largeCost));

        OptimizationResult result = optimizer.optimize(new OptimizationRequest(lanes, transporters, quotes, 1));
        assertThat(result.feasible()).isTrue();
        assertThat(result.totalCost()).isEqualByComparingTo("999999999999.99");
    }

    @Test
    @DisplayName("REQ-11: Verified 9-Lane Matrix Benchmark Fixture (maxTransporters = 3 -> 134876, T1, T4, T6)")
    void testNineLaneMatrixBenchmarkFixture() {
        List<Lane> lanes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            lanes.add(new Lane(i));
        }

        List<Transporter> transporters = new ArrayList<>();
        for (int t = 1; t <= 7; t++) {
            transporters.add(new Transporter(t, "Transporter T" + t));
        }

        long[][] matrix = {
                { 20835, 48844, 39020, 14400, 11601, 35095, 26070 }, // Lane 1
                { 10512, 31326, 20648, 44514, 19760, 12494, 41098 }, // Lane 2
                { 22105, 18640, 31438, 14316, 40870, 17808, 20932 }, // Lane 3
                { 42481, 45828, 36447, 10678, 20635, 36210, 16897 }, // Lane 4
                { 19862, 18297, 12789, 13032, 26421, 39444, 27938 }, // Lane 5
                { 13567, 45810, 49985, 46024, 28809, 29948, 43517 }, // Lane 6
                { 10015, 49573, 35285, 42342, 27815, 17320, 25881 }, // Lane 7
                { 17886, 45122, 10281, 35742, 17024, 10492, 46136 }, // Lane 8
                { 43996, 31856, 40092, 48921, 44691, 37864, 31286 }  // Lane 9
        };

        List<LaneQuote> quotes = new ArrayList<>();
        for (int l = 0; l < 9; l++) {
            for (int t = 0; t < 7; t++) {
                quotes.add(new LaneQuote(l + 1, t + 1, matrix[l][t]));
            }
        }

        OptimizationRequest request = new OptimizationRequest(lanes, transporters, quotes, 3);
        OptimizationResult result = optimizer.optimize(request);

        assertThat(result.feasible()).isTrue();
        assertThat(result.totalCost()).isEqualByComparingTo("134876.00");
        assertThat(result.selectedTransporters()).containsExactly(1L, 4L, 6L);

        // Verify exact lane assignments:
        // Lane 1 -> T4, Lane 2 -> T1, Lane 3 -> T4, Lane 4 -> T4, Lane 5 -> T4,
        // Lane 6 -> T1, Lane 7 -> T1, Lane 8 -> T6, Lane 9 -> T6
        Map<Long, Long> assignmentMap = result.assignments().stream()
                .collect(Collectors.toMap(LaneAssignment::laneId, LaneAssignment::transporterId));

        assertThat(assignmentMap).containsEntry(1L, 4L)
                .containsEntry(2L, 1L)
                .containsEntry(3L, 4L)
                .containsEntry(4L, 4L)
                .containsEntry(5L, 4L)
                .containsEntry(6L, 1L)
                .containsEntry(7L, 1L)
                .containsEntry(8L, 6L)
                .containsEntry(9L, 6L);
    }

    @Test
    @DisplayName("Official PDF 5-Lane Sample Fixture: Independent Mathematical Optimum = 58139 with T1, T4, T5")
    void testPdfFiveLaneDiscrepancyFixture() {
        // PDF 5 lanes and transporters fixture:
        // Transporters: T1(1), T2(2), T3(3), T4(4), T5(5), T6(6)
        // With maxTransporters = 3
        List<Lane> lanes = List.of(new Lane(1), new Lane(2), new Lane(3), new Lane(4), new Lane(5));
        List<Transporter> transporters = List.of(
                new Transporter(1), new Transporter(2), new Transporter(3),
                new Transporter(4), new Transporter(5), new Transporter(6)
        );

        // Quotes matrix for PDF 5-lane sample
        List<LaneQuote> quotes = List.of(
                new LaneQuote(1, 1, 12000), new LaneQuote(1, 4, 11000), new LaneQuote(1, 5, 14000),
                new LaneQuote(2, 1, 10000), new LaneQuote(2, 4, 15000), new LaneQuote(2, 5, 12000),
                new LaneQuote(3, 1, 18000), new LaneQuote(3, 4, 13000), new LaneQuote(3, 5, 20000),
                new LaneQuote(4, 1, 14000), new LaneQuote(4, 4, 16000), new LaneQuote(4, 5, 11139),
                new LaneQuote(5, 1, 13000), new LaneQuote(5, 4, 17000), new LaneQuote(5, 5, 15000)
        );

        OptimizationRequest request = new OptimizationRequest(lanes, transporters, quotes, 3);
        OptimizationResult result = optimizer.optimize(request);
        OptimizationResult oracleResult = new BruteForceReferenceSolver().solve(request);

        assertThat(result.feasible()).isTrue();
        assertThat(result.selectedTransporters()).containsExactly(1L, 4L, 5L);
        assertThat(result.totalCost()).isEqualByComparingTo(oracleResult.totalCost());
        assertThat(result.totalCost()).isEqualByComparingTo("58139.00");
    }

    @Test
    @DisplayName("More transporters than lanes: bounded by lane count")
    void testMoreTransportersThanLanes() {
        List<Lane> lanes = List.of(new Lane(1), new Lane(2));
        List<Transporter> transporters = List.of(
                new Transporter(1), new Transporter(2), new Transporter(3),
                new Transporter(4), new Transporter(5)
        );
        List<LaneQuote> quotes = new ArrayList<>();
        for (int l = 1; l <= 2; l++) {
            for (int t = 1; t <= 5; t++) {
                quotes.add(new LaneQuote(l, t, 100 + t));
            }
        }

        // Even if maxTransporters = 5, we only have 2 lanes, so max distinct used is 2
        OptimizationResult result = optimizer.optimize(new OptimizationRequest(lanes, transporters, quotes, 5));
        assertThat(result.feasible()).isTrue();
        assertThat(result.selectedTransporters()).hasSize(2);
    }
}
