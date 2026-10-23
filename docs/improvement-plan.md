# Plan: faster, more reliable local search in bb4-optimization

Status: Phases 0 (baseline and benchmark) and 1 (fewer allocations) are done. Later phases are not started.

Decisions: new APIs are added in 2.1-SNAPSHOT (additive, defaults keep today's behavior). The first user of the
changes is the Red puzzle in bb4-puzzles, then Tantrix.

## Why

The strategies in this library only see a fitness function, so a puzzle with exactly one right answer gives them
little to go on. The custom solvers written for bb4-puzzles (Tantrix, Red puzzle, sliding puzzle) were much faster
for two reasons. One is constant factors (primitive arrays, no allocation, incremental updates), which a library can
share. The other is using the structure of the problem (pruning, propagation), which a library cannot, except through
hooks that let the problem say something about its own structure. This plan covers the first and a limited form of
the second. It does not make the optimizers a replacement for an exact solver.

What the code does today:

- **Evaluation is always from scratch.** `Optimizee` only has `evaluateFitness(params)` and `compareFitness(p1, p2)`.
  Simulated annealing, iterated local search, hill climbing and genetic mutation each call `getRandomNeighbor` and then
  score the whole solution again. For a swap that changes two cells, that is O(n) work for an O(1) change.
- **`AbsoluteOptimizee.compareFitness` costs two full evaluations** (it evaluates both arrays). Strategies that go
  through it pay double.
- **Allocation in the hot loop.** `PermutedParameterArray.getRandomNeighbor` copies the params, rebuilds two boxed
  `Parameter` objects per swap, and wraps the result in a new immutable array. `candidateIdentity` builds a boxed
  `Seq[Double]` on every call, and the duplicate checks use it. Order crossover builds sets and filtered sequences.
- **Crossover is closed.** `ParameterCrossover.cross` is a class pattern match. The only extension point is
  `rebuildAfterOrderCrossover`.

## Phase 0: baseline and safety net (done)

- `OptimizationBenchmark` (in `scala-test/.../benchmark`) measures evaluations per second, bytes allocated per
  evaluation (single threaded strategies only) and the fitness reached in a fixed time, as the median over several
  seeds. Run it with `./gradlew benchmark` (5 seconds per search, about 10 minutes in all). A full run rewrites
  [benchmark-results.md](benchmark-results.md), which is committed, so `git diff` shows what a change did to the
  numbers. Commit it with each phase, from the same machine. [benchmarks-baseline.md](benchmarks-baseline.md) has
  the numbers before any of the changes below.
- Fixed a bug found while building it: `getSamplePopulationSize` overflowed for 16 or more integer parameters, so
  genetic search could not run on a permutation of that length at all.
- Policy on seeds: neighbor generation will change in later phases, so a given seed may follow a different path.
  Treat results as statistics over several seeds, not exact values.

## Phase 1: fewer allocations (done, no API change)

What the profile showed, and what was done. The numbers are from [benchmark-results.md](benchmark-results.md) against
[benchmarks-baseline.md](benchmarks-baseline.md) (the baseline used 1000 ms per run, the results 5000 ms).

- **Hill climbing spent its time on huge neighbors.** It widens the radius by 1% on every failed try, so after a few
  hundred tries a neighbor was tens of thousands of random swaps, each allocating two parameters (3 MB per
  evaluation). Past 4 swaps per parameter the result is as good as random, so `getRandomNeighbor` now shuffles
  instead. A few swaps copy the parameter references once. Many swaps work on a `double[]` and replace each changed
  parameter once. The swap count is clamped before it becomes an Int, since a radius that keeps growing otherwise
  overflows it. Parameters are not swapped by reference, because each position has its own name and range.
- **Iterated local search and state space search keyed visited states on `toCSVString`**, which formats every number
  through a synchronized global formatter (75% of allocation on `tour-200`), loses precision, and ignores
  `candidateIdentity` overrides. They now use the candidate identity. Its default is a `ValuesKey`, one `double[]`
  with array hash and equals, instead of a boxed `Seq[Double]`. This also makes the genetic search duplicate checks
  cheaper. A small behavior change: for continuous parameters, states that differed only beyond the printed digits
  used to count as the same state, and no longer do.
- **The hill climbing cache** is a `CandidateSet`, a `mutable.Set[ParameterArray]` keyed by candidate identity,
  instead of a `HashSet` that hashed every parameter object.
- **Order crossover** is its own class, `OrderCrossover`, with a boolean array for the slice membership when the values
  are whole numbers below n, and the old set for anything else. For the same random numbers it gives the same children.
- **`compareFitness` audit: no change needed.** `AbsoluteOptimizee.compareFitness` evaluates twice, but every strategy
  calls it only when `evaluateByComparison` is true, which is false for an `AbsoluteOptimizee`. Comparison optimizees
  need both the delta against the current point and the fitness against the baseline, so discrete hill climbing keeps
  its two calls. Noted for later: the sign convention in the `Optimizee.compareFitness` doc ("amount params1 are better")
  is the opposite of how the strategies read it (negative means the candidate is better). It matters only to someone
  who writes an optimizee that evaluates by comparison.

Results on the 2-core VM, medians of 5 seeds (5000 ms limit; baseline 1000 ms):

| tour-200 | Evals/sec before | after | Bytes/eval before | after | Median fitness before | after |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| HILL_CLIMBING | 920 | 25,827 | 2,812,167 | 25,853 | 91.1 | 91.1 |
| ITERATED_LOCAL_SEARCH | 9,654 | 171,915 | 49,316 | 4,720 | 77.7 | 76.8 |
| GENETIC_SEARCH | 60,488 | 181,947 | 16,094 | 4,556 | 79.4 | 33.8 |

Hill climbing and iterated local search now stop on their own in well under a second, with the same fitness as
before. Genetic search is limited by time, so it gets much further in the same time. Simulated annealing is
unchanged in speed and uses less memory. What is left is mostly the cost of building a new immutable array and new
parameter objects for every neighbor, which Phase 2 removes.

## Phase 2: delta evaluation (additive API)

- A small `Move` type. A permutation move is `Swap(i, j)` (segment reverse and insert later), a numeric move changes
  one coordinate, a subset move flips a membership bit.
- `ParameterArray.proposeMove(radius)` and `applyMove(move)`.
- `IncrementalOptimizee` with `evaluateMove(current, currentFitness, move): Double`. The default implementation applies
  the move and calls `evaluateFitness`, so every existing optimizee keeps working.
- Simulated annealing, iterated local search, hill climbing and genetic mutation hold the current fitness, ask for the
  delta, and build the new array only when the move is accepted. That also removes most neighbor allocations.
- Start with stateless deltas (safe for the concurrent genetic search). Consider an optional stateful `accept(move)`
  protocol later.
- `BudgetedOptimizee` counts move evaluations too, and documents how.
- A debug mode checks every delta against a full recompute, and is used in property tests.

## Phase 3: structure hooks (limited, additive)

- Replace the closed crossover match with `ParameterArray.crossWith(other, rnd)`. The default keeps today's behavior.
  A subclass can override it for type-aware crossover, as discussed for Tantrix.
- `repair(candidate)`, called after crossover and mutation, so a problem can restore feasibility (unique pieces,
  tile-type counts).
- Problem-supplied moves: an optimizee or parameter array can offer guided moves (for example swaps that involve cells
  with mismatched edges). Strategies mix them with random moves with a configurable probability.
- Scope: this makes the metaheuristic smarter. It is not constraint propagation or backtracking, which stay in the
  exact solvers.

## Phase 4: adopt in bb4-puzzles (after a release)

- Red puzzle: a swap or rotation touches at most 8 edges, so its fitness delta is a handful of comparisons. Add repair
  for piece uniqueness and guided moves toward unmatched edges.
- Tantrix: type-aware crossover and delta fitness for path changes.
- Re-run the benchmark against the Phase 0 numbers. The custom exact solvers stay as the reference.

## Suggested PR sequence

1. Phase 0: benchmark harness, baseline numbers, this plan (done).
2. Phase 1: neighbor, identity and cache, crossover, `compareFitness` audit (done, one PR).
3. Phase 2: Move API and the permutation swap; then simulated annealing; then iterated local search and hill climbing;
   then genetic mutation.
4. Phase 3: crossover hook; then repair; then guided moves.
5. Release, then the bb4-puzzles adoption PRs.

## Risks

- Seeded results change, which can break tests that pin exact values.
- A wrong delta silently corrupts the search, so the delta-check mode must be on in tests.
- Stateful deltas conflict with the concurrent genetic search.
- The API grows. Keep everything additive with defaults.

## What to expect

A hypothesis to confirm against the Phase 0 numbers: 10 to 100 times more evaluations per second on permutation
problems (phases 1 and 2), and better success rates from the hooks. It will not match a purpose built exact solver
on puzzles with a single right answer.
