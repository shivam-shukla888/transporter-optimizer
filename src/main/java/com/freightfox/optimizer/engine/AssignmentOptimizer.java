package com.freightfox.optimizer.engine;

import com.freightfox.optimizer.model.Lane;
import com.freightfox.optimizer.model.LaneAssignment;
import com.freightfox.optimizer.model.LaneQuote;
import com.freightfox.optimizer.model.OptimizationRequest;
import com.freightfox.optimizer.model.OptimizationResult;
import com.freightfox.optimizer.model.Transporter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pure Java, framework-independent optimization engine for assigning transporters to shipping lanes.
 * <p>
 * Optimization Priority:
 * 1. Lexicographic Primary: Maximize the number of distinct transporters actually assigned, capped at maxTransporters.
 * 2. Lexicographic Secondary: Among solutions with that maximal distinct transporter count, minimize total quote cost.
 * <p>
 * Constraints Enforced:
 * - Every lane is assigned to exactly one transporter.
 * - Every selected transporter is used for at least one lane.
 * - Only submitted quotes are eligible. Never fabricates missing quotes.
 * - Total cost is calculated directly from the sum of assigned quotes.
 * - Deterministic tie-breaking is applied on equal-cost solutions.
 */
public class AssignmentOptimizer {

    private static final int MAX_BITMASK_TRANSPORTERS = 20;

    private record QuoteKey(long laneId, long transporterId) {}

    public OptimizationResult optimize(OptimizationRequest request) {
        Objects.requireNonNull(request, "Optimization request cannot be null");

        List<Lane> lanes = request.lanes();
        if (lanes.isEmpty()) {
            return OptimizationResult.success(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), List.of(), List.of());
        }

        List<Lane> sortedLanes = lanes.stream().sorted().toList();
        int numLanes = sortedLanes.size();

        // Index quotes and find transporters that submitted at least one valid quote
        Map<QuoteKey, BigDecimal> quoteMap = new HashMap<>(request.quotes().size());
        Set<Long> transportersWithQuotes = new HashSet<>();
        for (LaneQuote quote : request.quotes()) {
            quoteMap.put(new QuoteKey(quote.laneId(), quote.transporterId()), quote.quoteAmount());
            transportersWithQuotes.add(quote.transporterId());
        }

        // Filter eligible transporters
        List<Transporter> eligibleTransporters = request.transporters().stream()
                .filter(t -> transportersWithQuotes.contains(t.id()))
                .distinct()
                .sorted()
                .toList();

        if (eligibleTransporters.isEmpty()) {
            return OptimizationResult.infeasible("No eligible transporters have submitted quotes");
        }

        // Verify every lane has at least one submitted quote
        for (Lane lane : sortedLanes) {
            boolean hasQuote = eligibleTransporters.stream()
                    .anyMatch(t -> quoteMap.containsKey(new QuoteKey(lane.id(), t.id())));
            if (!hasQuote) {
                return OptimizationResult.infeasible("Lane ID " + lane.id() + " has no submitted quotes from eligible transporters");
            }
        }

        int maxPossibleK = Math.min(request.maxTransporters(), Math.min(eligibleTransporters.size(), numLanes));
        if (maxPossibleK > MAX_BITMASK_TRANSPORTERS) {
            throw new IllegalArgumentException(
                    "Requested transporter subset size (" + maxPossibleK + ") exceeds bitmask DP safe limit of " + MAX_BITMASK_TRANSPORTERS
            );
        }

        // Search descending cardinality k to strictly maximize distinct transporter utilization
        for (int k = maxPossibleK; k >= 1; k--) {
            List<List<Transporter>> combinations = generateCombinations(eligibleTransporters, k);

            CandidateSolution bestCandidateForK = null;

            for (List<Transporter> subset : combinations) {
                CandidateSolution solution = solveForSubset(sortedLanes, subset, quoteMap);
                if (solution != null) {
                    if (bestCandidateForK == null || solution.isBetterThan(bestCandidateForK)) {
                        bestCandidateForK = solution;
                    }
                }
            }

            // At the first (highest) cardinality with any feasible solution, return the minimum cost result
            if (bestCandidateForK != null) {
                return OptimizationResult.success(
                        bestCandidateForK.totalCost,
                        bestCandidateForK.selectedTransporterIds,
                        bestCandidateForK.assignments
                );
            }
        }

        return OptimizationResult.infeasible("No feasible complete assignment can be formed using submitted quotes");
    }

    private CandidateSolution solveForSubset(
            List<Lane> lanes,
            List<Transporter> subset,
            Map<QuoteKey, BigDecimal> quoteMap
    ) {
        int k = subset.size();
        int numStates = 1 << k;
        int numLanes = lanes.size();

        BigDecimal[] dp = new BigDecimal[numStates];
        dp[0] = BigDecimal.ZERO;

        int[][] parentMask = new int[numLanes][numStates];
        int[][] chosenTransporterIdx = new int[numLanes][numStates];

        for (int i = 0; i < numLanes; i++) {
            Lane lane = lanes.get(i);
            BigDecimal[] nextDp = new BigDecimal[numStates];

            for (int j = 0; j < k; j++) {
                long transporterId = subset.get(j).id();
                BigDecimal quote = quoteMap.get(new QuoteKey(lane.id(), transporterId));
                if (quote == null) {
                    continue;
                }

                int transporterBit = 1 << j;

                for (int mask = 0; mask < numStates; mask++) {
                    if (dp[mask] == null) {
                        continue;
                    }

                    int nextMask = mask | transporterBit;
                    BigDecimal candidateCost = dp[mask].add(quote);

                    if (nextDp[nextMask] == null || candidateCost.compareTo(nextDp[nextMask]) < 0) {
                        nextDp[nextMask] = candidateCost;
                        parentMask[i][nextMask] = mask;
                        chosenTransporterIdx[i][nextMask] = j;
                    }
                }
            }

            dp = nextDp;
        }

        int fullMask = (1 << k) - 1;
        if (dp[fullMask] == null) {
            return null; // Not all k transporters could be utilized across all lanes
        }

        // Reconstruct assignments from backpointers
        List<LaneAssignment> assignments = new ArrayList<>(numLanes);
        int currentMask = fullMask;

        for (int i = numLanes - 1; i >= 0; i--) {
            int j = chosenTransporterIdx[i][currentMask];
            long chosenTransporterId = subset.get(j).id();
            BigDecimal cost = quoteMap.get(new QuoteKey(lanes.get(i).id(), chosenTransporterId));

            assignments.add(new LaneAssignment(lanes.get(i).id(), chosenTransporterId, cost));
            currentMask = parentMask[i][currentMask];
        }

        Collections.reverse(assignments);

        // Calculate verified total cost from actual assignments
        BigDecimal totalCost = BigDecimal.ZERO;
        for (LaneAssignment a : assignments) {
            totalCost = totalCost.add(a.cost());
        }

        List<Long> selectedIds = subset.stream().map(Transporter::id).sorted().toList();
        return new CandidateSolution(totalCost, selectedIds, assignments);
    }

    private List<List<Transporter>> generateCombinations(List<Transporter> items, int k) {
        List<List<Transporter>> result = new ArrayList<>();
        combinationsHelper(items, k, 0, new ArrayList<>(k), result);
        return result;
    }

    private void combinationsHelper(
            List<Transporter> items,
            int k,
            int start,
            List<Transporter> current,
            List<List<Transporter>> result
    ) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            current.add(items.get(i));
            combinationsHelper(items, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private static class CandidateSolution {
        final BigDecimal totalCost;
        final List<Long> selectedTransporterIds;
        final List<LaneAssignment> assignments;

        CandidateSolution(BigDecimal totalCost, List<Long> selectedTransporterIds, List<LaneAssignment> assignments) {
            this.totalCost = totalCost;
            this.selectedTransporterIds = selectedTransporterIds;
            this.assignments = assignments;
        }

        boolean isBetterThan(CandidateSolution other) {
            int costComparison = this.totalCost.compareTo(other.totalCost);
            if (costComparison != 0) {
                return costComparison < 0;
            }

            // Deterministic tie-breaker 1: Compare sorted selected transporter IDs
            int size = Math.min(this.selectedTransporterIds.size(), other.selectedTransporterIds.size());
            for (int i = 0; i < size; i++) {
                int cmp = Long.compare(this.selectedTransporterIds.get(i), other.selectedTransporterIds.get(i));
                if (cmp != 0) {
                    return cmp < 0;
                }
            }

            // Deterministic tie-breaker 2: Compare lane assignments
            int assignSize = Math.min(this.assignments.size(), other.assignments.size());
            for (int i = 0; i < assignSize; i++) {
                int cmp = this.assignments.get(i).compareTo(other.assignments.get(i));
                if (cmp != 0) {
                    return cmp < 0;
                }
            }

            return false;
        }
    }
}
