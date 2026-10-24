# Plan: strategies for constraint problems (tabu search, min-conflicts, adaptive restarts)

Status: proposal, nothing built yet.

Decisions already made: new APIs are additive in 2.1-SNAPSHOT, and defaults keep today's behavior. A new strategy is
judged on the benchmark, with solve rate first and time second (see "Does it earn its place?").

## Why

Puzzles such as the Red puzzle, Tantrix and sliding puzzles have one right answer: fitness 0, or nothing. For those the
question is not how good a result we reach in a fixed time but how often we reach 0 at all. The measurements from the
earlier phases show where we stand. On the 9 piece Red puzzle, genetic search found the solution in 5 of 12 runs and
annealing in 0 of 12 before guided moves were added (PR #66 in bb4-puzzles), and Tantrix genetic search solved 4 of 6
runs of 9 tiles in about 8 seconds. Genetic search keeps a population and recombines it, which pays off when good
partial solutions can be combined. Constraint problems mostly do not work that way: a solution is a single assignment
in which a few constraints are violated, and the useful step is to repair one violation without breaking others.
Local search methods built for that are tabu search, min-conflicts and restarts.

What exists today:

- Hill climbing, global hill climbing, simulated annealing, iterated local search (ILS) and variable neighborhood search
  (VNS). ILS and VNS already restart from a random sample after `restartAfterStagnantIterations`. Global hill climbing
  and global annealing start a fixed number of searches from the best of a set of samples. None of them adapts when to
  give up on a search.
- Moves (`Move`, `Swap`, `SwapSequence`), `Optimizee.evaluateMove` for delta evaluation, `proposeGuidedMove`, `repair`,
  `BudgetedOptimizee` (which stops a search after a number of evaluations by throwing
  `EvaluationBudgetExceededException`). These are the building blocks for everything below.
- Moves exist only for permutations (`PermutedParameterArray`). Arrays of integers or booleans, which is what coloring,
  scheduling and SAT style problems use, have `getRandomNeighbor` but no moves.
- Nothing in the benchmark is a constraint satisfaction problem with a known solution. `tsp-*` and `tour-*` are
  optimization problems, `subset-sum` and `dominating-set` are small.

## The three techniques, and what each needs

**Adaptive restarts.** Run a search for a budget of evaluations, and if it has not reached the goal, start a new one
from a fresh random sample (or from a perturbed best), with budgets that grow, for example the Luby sequence 1, 1, 2,
1, 1, 2, 4, ... times a base. Heavy tailed run times are the reason: a search that is going to succeed usually does so
early, and a long run is more often stuck than close. The "adaptive" part is that the base budget and the choice
between a random and a perturbed start follow what was seen (how soon earlier runs reached their best, whether the best
keeps improving between runs). It is a wrapper around any existing strategy, so it is cheap to build and helps all of
them. It needs no new hooks, because `BudgetedOptimizee` already ends a run.

**Tabu search.** From the current solution look at a set of moves (all of them for a small neighborhood, a random
sample for a large one), take the best move that is not tabu even if it is worse than where we are, and forbid undoing it
for a few iterations. An aspiration rule allows a tabu move that beats the best solution so far. This walks out of
local optima without randomness, and works well on permutation problems. It needs:

- `Move.attributes`, a small description of what a move changes that the tabu list stores (for `Swap(i, j)` the two
  positions with the values they held). The default is the move itself.
- A way to get a set of candidate moves. `proposeMove` gives random ones. For permutations a full enumeration of the
  swaps is also worth having when n is small, so a `ParameterArray.enumerateMoves(limit)` with a default of "not
  supported" (the strategy then samples).
- Delta evaluation (`evaluateMove`) so that looking at many moves is cheap. Without it tabu search is slow, since it
  evaluates a whole neighborhood every step.

**Min-conflicts.** Pick a variable that violates a constraint, and change it to the value that violates the fewest, with
a small chance of a random move so it does not cycle. It is the standard method for N-queens and coloring and works well
whenever the problem can say which variables are in conflict. It needs:

- `Optimizee.conflictedPositions(current): Seq[Int]`, default empty, meaning "unknown". Strategies fall back to a
  random position. This is the same kind of hook as `proposeGuidedMove` and `GuidedSwap` in the Red puzzle, which is
  min-conflicts by another name, and the Red result (9 of 20 solved to 15 of 20) is the best evidence that it pays off.
- A move that sets one position to one value, for arrays that are not permutations: `SetValue(index, value)`. For
  permutations the equivalent is a swap of the conflicted position with the best partner, found by scoring the
  swaps with `evaluateMove`.
- Which values to try at a position. For integer and boolean parameters that is the parameter's range; for
  permutations it is the other positions.

## Does it earn its place? (decision rule)

Build the benchmark first, then add a strategy to `OptimizationStrategyType` only if, on the constraint problems, it
solves more runs than the best existing strategy on at least one of them, or solves as many in clearly less time, and it
does not get worse on the others by more than the noise. A strategy that fails the rule stays as a class, off the enum
and off the default benchmark list, and the plan says so. This is the same discipline as the guided moves for
Tantrix, which did not pass and were left off.

## Phases

### Phase A: a way to measure (first, because everything else is judged by it)

- Constraint problems in `BenchmarkProblems`, each with a known solution and `getOptimalFitness` of 0:
  - N-queens as a permutation of 100 and 400 queens (conflicts counted on the diagonals; with an allocation free swap
    delta). The classic case for min-conflicts and tabu.
  - Graph coloring: a random graph that is built around a hidden coloring, so a solution exists, as integer parameters.
    Uses the `SetValue` move from phase B, so a version without delta evaluation comes first.
  - A hard permutation problem such as all-interval series, to keep the benchmark from being tuned to N-queens.
  - The Red puzzle lives in bb4-puzzles, so it is measured there (as in the Phase 4 work), not here.
- The benchmark reports, per problem and strategy, the number of runs that reached fitness 0, and the median time
  and evaluations of those that did. The current "fitness reached in a fixed time" column stays for optimization
  problems. Runs are repeated over more seeds (20), since solve rates need more samples than medians.
- Baseline of today's strategies on these problems, committed in `docs/benchmark-results.md` like the others.

### Phase B: shared pieces (additive, no behavior change)

- `Move.attributes` and a `SetValue(index, value)` move.
- Moves for `NumericParameterArray` when every parameter is an integer (and for boolean parameters), with
  `proposeMove`, `applyMove` and `supportsMoves`, consistent with `getRandomNeighbor`: applying the proposed move
  gives the same array as `getRandomNeighbor` for the same random numbers (the rule used for permutations).
- `Optimizee.conflictedPositions` with a default of empty, forwarded by the wrappers (`BudgetedOptimizee`,
  `DeltaCheckingOptimizee`, `LimitedOptimizee`) as `repair` and `proposeGuidedMove` are.
- `ParameterArray.enumerateMoves(limit)` for permutations, with a default of "unsupported".
- `DeltaCheckingOptimizee` already checks any move against a full evaluation, so the new move types are covered by it.

### Phase C: adaptive restarts

- `RestartingStrategy(innerType, config)`: runs the inner strategy through `BudgetedOptimizee` for a budget taken from
  a Luby sequence times `baseBudget`, keeps the best result, and stops at fitness 0, at the overall evaluation budget, or
  when the iteration limit is reached. A restart starts from `getRandomSample`, or from a perturbed copy of the best (a
  `getRandomNeighbor` with a large radius) with a probability that rises when fresh starts have not beaten the best.
- The base budget is adapted from the first runs (a fraction of the evaluations that the first run took to reach its
  best), unless it is given.
- Enum entries for the useful wrapped combinations are decided by the rule above, probably restarting hill climbing
  and restarting annealing, not every pair. A `RestartingStrategy` can be built for any type by code.
- Tests: the Luby sequence, restarts stop at the goal, the budget is shared and never exceeded, the best result of
  all runs is returned, and a seeded run repeats.

### Phase D: tabu search

- `TabuSearchStrategy` with `TabuSearchConfig`: `maxIterations`, `candidateMoves` (how many moves to look at per
  step; all of them when the neighborhood is smaller), `tenureMin`/`tenureMax` (random per move, which avoids cycles
  of a fixed length), `aspiration` on or off, `restartAfterStagnantIterations`.
- Uses moves and `evaluateMove`; for an array without moves it builds neighbors and scores them, like the other
  strategies, but is documented as slow there.
- Tests: a move that is tabu is not taken, the aspiration rule lets a tabu move through when it beats the best,
  the tenure expires, a seeded run repeats, budget exhaustion returns the best so far, and a solved problem ends early.

### Phase E: min-conflicts

- `MinConflictsStrategy` with `MinConflictsConfig`: `maxSteps`, `noiseProbability` (a random move instead of the best
  one), `tieBreaking` (random among equally good values), and `restartAfterStagnantSteps`.
- Each step: take `optimizee.conflictedPositions(current)`, pick one at random, and look at the values or swap partners
  for that position through `evaluateMove`, taking the best, or a random one with the noise probability. If the
  optimizee does not report conflicts the step picks a random position, so the strategy still runs but is only a
  randomized greedy search. That case is documented, not hidden.
- Tests: with a problem that reports conflicts it only touches conflicted positions, the noise probability of 0 is
  deterministic given the seed, it solves small N-queens and a small coloring, and the fallback runs.

### Phase F: register, document, adopt

- `OptimizationStrategyType` entries, descriptions and `appliesTo` (min-conflicts and tabu only for arrays with moves;
  min-conflicts is useful only when the optimizee reports conflicts, so `appliesTo` stays permissive and the
  description says so). Update the benchmark default list, the README strategy table and the results file.
- In bb4-puzzles: give the Red puzzle `conflictedPositions` (its loose edges, which `GuidedSwap` already finds) and try
  tabu and min-conflicts there, as in Phase 4. Tantrix and the sliding puzzle only if the benchmark and Red results
  say they are worth it. The puzzle results go in that repo's `docs/benchmarks.md`.

## Pull requests

1. Phase A: constraint problems, solve rate reporting, baseline results. (Docs and test code only.)
2. Phase B: moves for integer arrays, `attributes`, `conflictedPositions`, `enumerateMoves`.
3. Phase C: adaptive restarts.
4. Phase D: tabu search.
5. Phase E: min-conflicts.
6. Phase F: registration and docs, then the bb4-puzzles PRs.

Phases C, D and E depend on A and B but not on each other, so they can be done in any order. I would do C first: it is
the smallest, it can use the existing strategies, and it tells us early whether solve rate responds to restarts on
these problems.

## Risks

- **Tuning.** Tabu tenure and the noise probability matter a lot. Defaults come from the benchmark problems and could be
  wrong for a puzzle. Mitigation: the configs are public, and the results in the doc are from named problems and seeds.
- **Min-conflicts without conflict information is just greedy search.** It only earns its place when a problem reports
  conflicts, which is extra work for the problem author. The fallback keeps it correct but not useful.
- **Solve rates need many runs.** Twenty seeds per cell makes the full benchmark longer. A `--quick` option or a
  smaller list for the solve rate section may be needed.
- **Evaluation counting.** `BudgetedOptimizee` counts `evaluateMove` as an evaluation, so a tabu step that looks at 50 moves
  uses 50 of the budget. That is the honest cost and the restart budgets are in the same units, but strategies must not
  be compared with a count of steps.
- **Overlap with ILS/VNS.** Restarting ILS may add little, since ILS already restarts. The decision rule applies there too.
- **Determinism.** Restarts and tabu tenure use the strategy's random generator, so a seeded run repeats. Any threaded
  variant (parallel restarts) is out of scope for now, since it would not.

## Not in this plan

- Constraint propagation or backtracking (the exact solvers in bb4-puzzles keep that role).
- Parallel restarts, or a portfolio that runs several strategies at once.
- New parameter array types. Boolean and integer arrays get moves, nothing else changes.
- Changing the defaults of existing strategies.

## Open questions

These have defaults, so the plan can proceed without answers:

1. Should tabu search and min-conflicts be separate enum entries, as proposed, or options of an existing strategy? Default: separate
   entries, which matches how ILS and VNS were added.
2. Is N-queens plus coloring plus all-interval series the right benchmark set? Default: yes, and add a problem from
   bb4-puzzles later if one is needed.
3. Is a 20 seed solve rate section acceptable for the length of a full benchmark run (about 10 minutes now)? Default:
   yes, with the section as its own `--solve-rate` run if it makes the full run too long.
