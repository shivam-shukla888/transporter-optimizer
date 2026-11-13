package com.freightfox.optimizer;

import com.freightfox.optimizer.engine.AssignmentOptimizer;
import com.freightfox.optimizer.model.Lane;
import com.freightfox.optimizer.model.LaneAssignment;
import com.freightfox.optimizer.model.LaneQuote;
import com.freightfox.optimizer.model.OptimizationRequest;
import com.freightfox.optimizer.model.OptimizationResult;
import com.freightfox.optimizer.model.Transporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentOptimizerOracleComparisonTest {

    private final AssignmentOptimizer productionOptimizer = new AssignmentOptimizer();
    private final BruteForceReferenceSolver oracleSolver = new BruteForceReferenceSolver();

    @Test
    @DisplayName("REQ-10: Compare production AssignmentOptimizer against independent BruteForceReferenceSolver across 50 randomized instances")
    void testCompareAgainstBruteForceOracleOnRandomInstances() {
        Random random = new Random(42); // Deterministic seed for reproducible tests

        int testCount = 50;
        for (int iteration = 0; iteration < testCount; iteration++) {
            int numLanes = 2 + random.nextInt(4); // 2 to 5 lanes
            int numTransporters = 2 + random.nextInt(3); // 2 to 4 transporters
            int maxTransporters = 1 + random.nextInt(numTransporters);

            List<Lane> lanes = new ArrayList<>();
            for (int l = 1; l <= numLanes; l++) {
                lanes.add(new Lane(l));
            }

            List<Transporter> transporters = new ArrayList<>();
            for (int t = 1; t <= numTransporters; t++) {
                transporters.add(new Transporter(t, "T" + t));
            }

            List<LaneQuote> quotes = new ArrayList<>();
            for (int l = 1; l <= numLanes; l++) {
                // Ensure at least one quote per lane
                int guaranteedTransporter = 1 + random.nextInt(numTransporters);
                quotes.add(new LaneQuote(l, guaranteedTransporter, 10 + random.nextInt(500)));

                // Add random additional quotes
                for (int t = 1; t <= numTransporters; t++) {
                    if (t != guaranteedTransporter && random.nextDouble() < 0.70) {
                        quotes.add(new LaneQuote(l, t, 10 + random.nextInt(500)));
                    }
                }
            }

            OptimizationRequest request = new OptimizationRequest(lanes, transporters, quotes, maxTransporters);

            OptimizationResult productionResult = productionOptimizer.optimize(request);
            OptimizationResult oracleResult = oracleSolver.solve(request);

            // 1. Feasibility agreement
            assertThat(productionResult.feasible())
                    .as("Feasibility must match on iteration %d", iteration)
                    .isEqualTo(oracleResult.feasible());

            if (productionResult.feasible()) {
                // 2. Maximal distinct transporter count agreement (Priority 1)
                int prodDistinctCount = productionResult.selectedTransporters().size();
                int oracleDistinctCount = oracleResult.selectedTransporters().size();
                assertThat(prodDistinctCount)
                        .as("Transporter count must match on iteration %d (maxTransporters=%d)", iteration, maxTransporters)
                        .isEqualTo(oracleDistinctCount);

                // 3. Minimal total cost agreement (Priority 2)
                assertThat(productionResult.totalCost())
                        .as("Total cost must match on iteration %d", iteration)
                        .isEqualByComparingTo(oracleResult.totalCost());

                // 4. Invariant: Every lane assigned exactly once
                Set<Long> assignedLanes = productionResult.assignments().stream()
                        .map(LaneAssignment::laneId)
                        .collect(Collectors.toSet());
                assertThat(assignedLanes).hasSize(numLanes);

                // 5. Invariant: Every selected transporter is used at least once
                Set<Long> usedTransporters = productionResult.assignments().stream()
                        .map(LaneAssignment::transporterId)
                        .collect(Collectors.toSet());
                assertThat(usedTransporters).isEqualTo(Set.copyOf(productionResult.selectedTransporters()));

                // 6. Invariant: Sum of assignments equals totalCost
                BigDecimal calculatedSum = productionResult.assignments().stream()
                        .map(LaneAssignment::cost)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                assertThat(productionResult.totalCost()).isEqualByComparingTo(calculatedSum);
            }
        }
    }
}
