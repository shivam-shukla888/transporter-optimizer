package com.freightfox.optimizer;

import com.freightfox.optimizer.model.Lane;
import com.freightfox.optimizer.model.LaneAssignment;
import com.freightfox.optimizer.model.LaneQuote;
import com.freightfox.optimizer.model.OptimizationRequest;
import com.freightfox.optimizer.model.OptimizationResult;
import com.freightfox.optimizer.model.Transporter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Independent brute-force oracle solver used to verify the mathematical correctness of
 * the production AssignmentOptimizer on small problem instances.
 * <p>
 * Does NOT use bitmask dynamic programming or subset enumeration. Recursively explores all
 * valid lane assignments to find the exact global optimum under the lexicographical objective:
 * 1. Maximize distinct transporters used <= maxTransporters
 * 2. Minimize total cost
 */
public class BruteForceReferenceSolver {

    private record QuoteKey(long laneId, long transporterId) {}

    public OptimizationResult solve(OptimizationRequest request) {
        List<Lane> lanes = request.lanes().stream().sorted().toList();
        if (lanes.isEmpty()) {
            return OptimizationResult.success(BigDecimal.ZERO, List.of(), List.of());
        }

        Map<QuoteKey, BigDecimal> quotes = new HashMap<>();
        for (LaneQuote q : request.quotes()) {
            quotes.put(new QuoteKey(q.laneId(), q.transporterId()), q.quoteAmount());
        }

        List<Transporter> transporters = request.transporters().stream().distinct().sorted().toList();

        // Check each lane has at least one quote
        for (Lane lane : lanes) {
            boolean hasQuote = transporters.stream()
                    .anyMatch(t -> quotes.containsKey(new QuoteKey(lane.id(), t.id())));
            if (!hasQuote) {
                return OptimizationResult.infeasible("Lane " + lane.id() + " has no eligible quotes");
            }
        }

        int maxK = request.maxTransporters();
        BruteForceState best = new BruteForceState();

        int[] currentAssignment = new int[lanes.size()];
        explore(0, lanes, transporters, quotes, maxK, currentAssignment, best);

        if (best.assignments == null) {
            return OptimizationResult.infeasible("No feasible assignment found");
        }

        List<Long> selected = best.selectedTransporters.stream().sorted().toList();
        return OptimizationResult.success(best.totalCost, selected, best.assignments);
    }

    private void explore(
            int laneIdx,
            List<Lane> lanes,
            List<Transporter> transporters,
            Map<QuoteKey, BigDecimal> quotes,
            int maxK,
            int[] currentAssignment,
            BruteForceState best
    ) {
        if (laneIdx == lanes.size()) {
            evaluateAssignment(lanes, transporters, quotes, maxK, currentAssignment, best);
            return;
        }

        long laneId = lanes.get(laneIdx).id();
        for (int tIdx = 0; tIdx < transporters.size(); tIdx++) {
            long transporterId = transporters.get(tIdx).id();
            if (quotes.containsKey(new QuoteKey(laneId, transporterId))) {
                currentAssignment[laneIdx] = tIdx;
                explore(laneIdx + 1, lanes, transporters, quotes, maxK, currentAssignment, best);
            }
        }
    }

    private void evaluateAssignment(
            List<Lane> lanes,
            List<Transporter> transporters,
            Map<QuoteKey, BigDecimal> quotes,
            int maxK,
            int[] currentAssignment,
            BruteForceState best
    ) {
        Set<Long> usedTransporters = new HashSet<>();
        BigDecimal cost = BigDecimal.ZERO;
        List<LaneAssignment> assignments = new ArrayList<>(lanes.size());

        for (int i = 0; i < lanes.size(); i++) {
            long laneId = lanes.get(i).id();
            long tId = transporters.get(currentAssignment[i]).id();
            BigDecimal quoteCost = quotes.get(new QuoteKey(laneId, tId));

            usedTransporters.add(tId);
            cost = cost.add(quoteCost);
            assignments.add(new LaneAssignment(laneId, tId, quoteCost));
        }

        int distinctCount = usedTransporters.size();
        if (distinctCount > maxK) {
            return; // Exceeds upper bound
        }

        if (distinctCount > best.distinctCount) {
            // Priority 1: Greater distinct transporters
            best.distinctCount = distinctCount;
            best.totalCost = cost;
            best.selectedTransporters = new ArrayList<>(usedTransporters);
            best.assignments = assignments;
        } else if (distinctCount == best.distinctCount) {
            // Priority 2: Lower cost
            if (best.assignments == null || cost.compareTo(best.totalCost) < 0) {
                best.totalCost = cost;
                best.selectedTransporters = new ArrayList<>(usedTransporters);
                best.assignments = assignments;
            }
        }
    }

    private static class BruteForceState {
        int distinctCount = 0;
        BigDecimal totalCost = null;
        List<Long> selectedTransporters = null;
        List<LaneAssignment> assignments = null;
    }
}
