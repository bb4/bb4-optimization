# Plan: faster, more reliable local search in bb4-optimization

Status: Phases 0 (baseline and benchmark), 1 (fewer allocations), 2 (delta evaluation, for permutations) and 3 (structure hooks) are done. Phase 4 is not started.

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

## Phase 2: delta evaluation (done for permutations, simulated annealing and genetic greedy mutation)

What was built, all additive:

- **`Move`** (`parameter/Move.scala`) is a trait, with `Swap(first, second)` and `SwapSequence(swaps)` for permutations.
  A `ParameterArray` has `supportsMoves` (false by default), `proposeMove(radius)` and `applyMove(move)`.
  `PermutedParameterArray.proposeMove` draws exactly the random numbers `getRandomNeighbor` draws, so
  `applyMove(proposeMove(r))` is the neighbor `getRandomNeighbor(r)` would have been, at every radius, including the
  shuffle used for huge radii. A test checks this. That is what lets a strategy switch to moves without changing its
  search path.
- **`Optimizee.evaluateMove(current, currentFitness, move)`** defaults to applying the move and evaluating in full, so
  every existing optimizee keeps working. An optimizee overrides it to work out the new fitness from the move alone.
  This is on `Optimizee` itself, not on a separate `IncrementalOptimizee` trait as first sketched, because strategies
  must be able to call it on any optimizee. Delta evaluation is for absolute fitness only; comparison mode is unchanged.
- **`BudgetedOptimizee`** (and the benchmark's `LimitedOptimizee`) count a move evaluation as one evaluation, share the
  budget with full evaluations, and pass the call to the delegate so its own delta is used.
- **`DeltaCheckingOptimizee`** is the debug wrapper from the plan. It checks every `evaluateMove` against a full
  evaluation of the moved array, and checks that the fitness the strategy passed for the current array is the true one.
  Use it in tests of any new delta.
- **Simulated annealing** proposes a move, asks `evaluateMove`, and builds the neighbor only if it keeps it
  (`SimulatedAnnealingConfig.useMoves`, default true; off when logging to a file or evaluating by comparison).
  It follows the same path as before: the same tours come out (compared as cycles), tested on 8, 25 and 60 cities with
  every delta checked.
- **Genetic greedy mutation** tries up to `maxNeighborRetries + 1` moves and builds only the mutant it returns
  (`GeneticSearchConfig.useMoves`, default true). The mutants are identical to the old ones for the same random numbers.
- **The random tour benchmark problem** has a swap delta that changes four legs and allocates nothing.

Choices worth knowing about:

- **Only a plain `PermutedParameterArray` supports moves.** `TantrixPath` and the Red puzzle's `PieceParameterArray`
  extend it, override `getRandomNeighbor` and carry more than their parameters. If they inherited moves, a strategy
  would bypass their neighbors and build plain arrays. A subclass opts in by overriding `supportsMoves` and
  `applyMove`. Tests check that annealing and genetic mutation keep calling a subclass's own `getRandomNeighbor`.
- **Fitness carried through many moves picks up rounding error** (about 1e-16 per move). Two tours that are rotations of
  each other tie in exact arithmetic and can break differently, so a run can end on a different rotation of the same
  cycle. The fitness agrees to well under 1e-9.
- **A multi-swap delta did not pay.** An overlay that works out the legs touched by 2 to 8 swaps was slower than a full
  evaluation at 50 and 200 cities (it made genetic search about half as fast at 50), so it was removed. A
  `SwapSequence` falls back to the default. A problem with an expensive full evaluation may well want one.

Not done, and why:

- **Iterated local search and hill climbing.** Iterated local search promises to evaluate every distinct solution at
  most once, which needs the neighbor's key, and building the key costs as much as building the neighbor. Hill climbing
  has the same kind of cache, and most of its time is spent on huge-radius neighbors, which are shuffles rather than
  swaps. Taking this on means choosing between that promise and speed (an opt-in setting that allows re-evaluation, or an
  incrementally updated hash of the permutation). Left for a decision.
- **Non-greedy genetic mutation and crossover.** These need the child array to remove duplicates, and crossover has no
  small move.
- **Numeric and subset moves.** The `Move` trait allows them; only permutations are implemented.

Results on the 2-core VM, medians of 5 seeds, 5000 ms, from [benchmark-results.md](benchmark-results.md) (the "before"
column is the Phase 1 run). The fitness reached is the same:

| Problem | Strategy | Evals/sec before | after | Bytes/eval before | after |
| --- | --- | ---: | ---: | ---: | ---: |
| tour-200 | SIMULATED_ANNEALING | 556,680 | 1,365,343 | 1,123 | 654 |
| tour-200 | GLOBAL_SIMULATED_ANNEALING | 399,867 | 797,559 | 1,135 | 666 |
| tour-200 | GENETIC_SEARCH | 181,947 | 218,529 | 4,556 | 4,529 |
| tour-50 | SIMULATED_ANNEALING | 1,085,876 | 1,155,608 | 592 | 343 |

Isolated runs with nothing else on the machine measured simulated annealing on `tour-200` at about 2.7 million evals/sec,
so the full run understates it. The gain grows with the tour length, since the full evaluation is O(n) and the delta
is O(1). A tour of 50 is short enough that other costs dominate. Genetic search gains little because most of its
evaluations are crossover children, which have no delta.

## Phase 3: structure hooks (done, additive)

- Replace the closed crossover match with `ParameterArray.crossWith(other, rnd)`. The default keeps today's behavior.
  A subclass can override it for type-aware crossover, as discussed for Tantrix.
- `repair(candidate)`, called after crossover and mutation, so a problem can restore feasibility (unique pieces,
  tile-type counts).
- Problem-supplied moves: an optimizee or parameter array can offer guided moves (for example swaps that involve cells
  with mismatched edges). Strategies mix them with random moves with a configurable probability.
- Scope: this makes the metaheuristic smarter. It is not constraint propagation or backtracking, which stay in the
  exact solvers.

### What Phase 3 added

All three hooks default to today's behavior, so a problem that overrides nothing runs exactly as before.

- `ParameterArray.crossWith(other, rnd)`. The default calls the old crossover. Genetic search calls it for every pair.
- `Optimizee.repair(candidate)`. The default returns the same instance, which is the contract for "nothing to fix".
  Genetic search repairs crossover children, non-greedy mutants, greedy mutants (built or from a move) and the
  initial population. A greedy mutant found by a move has a fitness for the array as built, so when `repair` returns
  a different instance the repaired array is scored again, in full. The wrappers (`BudgetedOptimizee`,
  `DeltaCheckingOptimizee`, the benchmark's `LimitedOptimizee`) forward it and do not count it as an evaluation.
- `Optimizee.proposeGuidedMove(current, radius, rnd)`, returning an `Option[Move]`, and
  `guidedMoveProbability` (default 0, validated to [0, 1]) in `SimulatedAnnealingConfig` and `GeneticSearchConfig`.
  `MoveChoice.propose` asks the problem with that probability and uses a random move when it has nothing to
  suggest. With a probability of 0 it draws no random number, so seeded runs are unchanged. It applies only on the
  move paths, so it needs `supportsMoves` and absolute fitness.

### Does guiding help? A first look

`RandomTourProblem` suggests a long-leg move: sample 3 legs, take the longest, and swap the city at its end with a
random position. Median best tour length over 7 seeds, 4 s or 3M evaluations per run, lower is better, 2-core VM
(a scratch run, not part of the committed benchmark):

| Strategy, cities | p = 0 | 0.1 | 0.3 | 0.5 | 0.8 |
|---|---|---|---|---|---|
| Annealing, 100 | 12.9 | 13.3 | 13.4 | 13.6 | 13.3 |
| Annealing, 200 | 26.5 | 25.8 | 24.3 | 24.3 | 23.4 |
| Genetic, 100 | 12.5 | 11.8 | 12.0 | 12.1 | 12.0 |
| Genetic, 200 | 35.4 | 33.0 | 34.4 | 32.5 | 32.6 |

The effect is real but modest and depends on the problem: about 5 to 12 percent better for annealing on 200 cities,
3 to 8 percent for genetic search, and no gain (within the noise) for annealing on 100 cities, where the run already
finishes its schedule. The hook is a way to put a problem's knowledge to use. The numbers say that a guide is worth
writing when it targets real defects (like the long leg here), and that it should be tried with a probability around
0.5 rather than assumed to help.

### Not done

- Iterated local search, hill climbing and the variable neighborhood search do not use guided moves or repair. They
  would need the same plumbing as `useMoves` (see Phase 2), and are best added with the real puzzles in Phase 4.
- Repair is not applied by simulated annealing, which has no crossover and moves from a feasible state by small steps.
  A problem whose moves can leave the feasible set should keep them feasible in `getRandomNeighbor`.

## Phase 4: adopt in bb4-puzzles (after a release)

- Red puzzle: a swap or rotation touches at most 8 edges, so its fitness delta is a handful of comparisons. Add repair
  for piece uniqueness and guided moves toward unmatched edges.
- Tantrix: type-aware crossover and delta fitness for path changes.
- Re-run the benchmark against the Phase 0 numbers. The custom exact solvers stay as the reference.

## Suggested PR sequence

1. Phase 0: benchmark harness, baseline numbers, this plan (done).
2. Phase 1: neighbor, identity and cache, crossover, `compareFitness` audit (done, one PR).
3. Phase 2: Move API and the permutation swap; simulated annealing; genetic greedy mutation (done, one PR).
   Iterated local search and hill climbing are waiting on a decision, see Phase 2.
4. Phase 3: crossover hook; then repair; then guided moves (done, one PR, three commits).
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
