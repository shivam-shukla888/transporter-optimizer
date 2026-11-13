# Assignment assumptions

This document records the interpretation used by the implementation. The assignment brief and sample data contain ambiguities; these assumptions must remain visible and must not be silently treated as confirmed business requirements.

## Optimization priority

1. Maximize the number of distinct transporters actually assigned, subject to the requested upper bound.
2. Among solutions with that transporter count, minimize total quoted cost.
3. Assign each lane exactly once.
4. A transporter is eligible for a lane only when a quote exists for that lane-transporter pair.
5. Every transporter reported as selected must receive at least one lane.

If the requested maximum cannot be reached, the optimizer will search lower transporter counts until it finds a feasible solution. If no complete assignment exists, it returns an explicit infeasible result/error according to the API contract.

## Input interpretation

- A submitted input represents a complete dataset snapshot.
- A new successful input submission becomes the active snapshot atomically.
- An assignment request uses one snapshot consistently; it must not mix rows from two submissions.
- Lane IDs, transporter IDs and lane-transporter quote pairs must be unique within a snapshot.
- Quotes must be non-negative monetary values. Monetary arithmetic must not use binary floating point.

## Tie-breaking

When multiple assignments have the same objective value, use a documented deterministic tie-break (stable ascending transporter ID, then stable lane order). This affects reproducibility, not the primary optimization objective.

## Sample discrepancies to disclose

The provided PDF's first sample output appears internally inconsistent: a lane is assigned to a transporter not listed among the selected transporters, and the stated total does not equal the sum of its displayed lane costs. The implementation should follow the explicit constraints above and explain any resulting difference from that sample.

The supplied nine-lane CSV and examples in the PDF should be treated as distinct fixtures unless the brief explicitly says otherwise.

## Scope boundaries

The assignment optimizer is cost-only. Reliability, capacity, emissions, transit time, transporter preferences and multi-tenant isolation are possible future constraints, but must not be added to the objective without an explicit requirement.
