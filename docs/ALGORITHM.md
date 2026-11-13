# Optimization algorithm

## Objective

Given lanes, transporter quotes and an upper bound K, find a complete lane assignment that:

1. maximizes the number of distinct transporters used, capped at K;
2. minimizes total quote cost among assignments using that maximum feasible count;
3. assigns every lane exactly once; and
4. uses only submitted quotes.

This is a constrained combinatorial optimization problem. Choosing the cheapest quote independently per lane is not sufficient because it may use fewer distinct transporters than the primary objective requires.

## Exact method for the supplied small dataset

Use transporter-subset enumeration with bitmask dynamic programming.

For a candidate subset S of K transporters:

- Process lanes in a deterministic order.
- Maintain a DP state indexed by the bitmask of selected transporters already used.
- A state stores the minimum cost after processing the current prefix of lanes.
- For each lane, transition through every eligible transporter in S that quoted for that lane.
- Set that transporter's bit in the next mask.
- At the end, accept only the full mask (all selected transporters used).

Enumerate subsets from min(K, transporterCount) down to 1. At the first subset size with any feasible solution, retain the minimum-cost feasible assignment across all subsets of that size.

## Complexity

For T transporters, L lanes, and candidate subset size k, the straightforward upper bound is:

O(C(T,k) * L * k * 2^k)

per fixed k, with the cost of enumerating all subset sizes considered separately. Rolling DP costs O(2^k) memory for objective values. Assignment reconstruction requires predecessor information or a compact backpointer strategy.

The method is exact but exponential in transporter count. It is appropriate only while input constraints remain small. Do not claim it scales to hundreds or thousands of transporters without a different formulation and evidence.

## Correctness outline

For a fixed transporter subset, induction over processed lanes shows that each DP state stores the least possible cost for its used-transporter mask among all assignments of the processed lane prefix. Each transition considers every eligible quote for the next lane, so no valid assignment is omitted. At the final lane, the full mask represents exactly those assignments using every transporter in the subset. Enumerating all subsets of a fixed cardinality therefore finds the cheapest feasible assignment with that cardinality. Searching cardinalities in descending order enforces the primary objective before cost minimization.

## Engineering requirements

- Use integer minor units or BigDecimal for monetary values; never double.
- Define deterministic ordering for lanes, transporter IDs and tie resolution.
- Validate that every lane has at least one eligible quote before solving.
- Avoid mutable global state.
- Keep the optimizer independent of Spring, JPA, HTTP and PostgreSQL.
- Test the solver against brute force on many small randomly generated cases.
- Benchmark before deciding whether a more scalable solver is necessary.
