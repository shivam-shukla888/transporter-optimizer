package com.freightfox.optimizer;

import com.freightfox.optimizer.engine.AssignmentOptimizer;
import com.freightfox.optimizer.model.Lane;
import com.freightfox.optimizer.model.LaneQuote;
import com.freightfox.optimizer.model.OptimizationRequest;
import com.freightfox.optimizer.model.OptimizationResult;
import com.freightfox.optimizer.model.Transporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentOptimizerBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(AssignmentOptimizerBenchmarkTest.class);
    private final AssignmentOptimizer optimizer = new AssignmentOptimizer();

    @Test
    @DisplayName("REQ-20: Measure optimizer execution performance across standard and large problem instances")
    void testBenchmarkOptimizer() {
        runBenchmarkScenario("Official 9-Lane Matrix", 9, 7, 3, 1.0);
        runBenchmarkScenario("Medium Instance 15 Lanes / 8 Transporters", 15, 8, 4, 0.85);
        runBenchmarkScenario("Large Instance 20 Lanes / 10 Transporters", 20, 10, 5, 0.80);
        runBenchmarkScenario("High Density 25 Lanes / 12 Transporters", 25, 12, 4, 0.75);
    }

    private void runBenchmarkScenario(String name, int numLanes, int numTransporters, int maxTransporters, double quoteDensity) {
        Random random = new Random(numLanes * 31L + numTransporters);

        List<Lane> lanes = new ArrayList<>(numLanes);
        for (int l = 1; l <= numLanes; l++) {
            lanes.add(new Lane(l));
        }

        List<Transporter> transporters = new ArrayList<>(numTransporters);
        for (int t = 1; t <= numTransporters; t++) {
            transporters.add(new Transporter(t, "T" + t));
        }

        List<LaneQuote> quotes = new ArrayList<>();
        for (int l = 1; l <= numLanes; l++) {
            int guaranteedTransporter = 1 + random.nextInt(numTransporters);
            quotes.add(new LaneQuote(l, guaranteedTransporter, 1000 + random.nextInt(20000)));

            for (int t = 1; t <= numTransporters; t++) {
                if (t != guaranteedTransporter && random.nextDouble() < quoteDensity) {
                    quotes.add(new LaneQuote(l, t, 1000 + random.nextInt(20000)));
                }
            }
        }

        OptimizationRequest request = new OptimizationRequest(lanes, transporters, quotes, maxTransporters);

        // Warm up JIT
        optimizer.optimize(request);

        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startNanos = System.nanoTime();

        OptimizationResult result = optimizer.optimize(request);

        long durationNanos = System.nanoTime() - startNanos;
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double durationMs = durationNanos / 1_000_000.0;
        long memoryDeltaBytes = Math.max(0, memAfter - memBefore);

        assertThat(result.feasible()).isTrue();
        assertThat(result.assignments()).hasSize(numLanes);
        assertThat(result.selectedTransporters().size()).isLessThanOrEqualTo(maxTransporters);

        log.info("[BENCHMARK] Scenario: '{}' | Lanes: {} | Transporters: {} | Quotes: {} | maxK: {} | Time: {} ms | Used Transporters: {} | Total Cost: {} | Mem: {} KB",
                name, numLanes, numTransporters, quotes.size(), maxTransporters,
                String.format("%.2f", durationMs), result.selectedTransporters().size(), result.totalCost(), memoryDeltaBytes / 1024);
    }
}
