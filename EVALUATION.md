# Evaluation of bb4-optimization strategies

Fitness is minimized. Every viewer problem used here has a known optimum of 0. A run counts as converged when the best fitness is at most 0.01 above that optimum. Random strategies were repeated with seeds 1, 42, and 99. The number in each cell is the **best fitness across those seeds**, followed by how many seeds converged. Full per-seed fitness, evaluation counts, unique candidates, runtime, and status are in the TSV artifacts.

Headless runs went through `Optimizer.doOptimization`. The same problems were also driven through `OptimizerEvalApp` on a 1920×1200 display (`DISPLAY=:1`). Screenshots are one PNG per strategy per problem (104), plus a short after-fix set.

## Tests

| Run | Result |
|---|---|
| `./gradlew test` on the tree **before** these fixes | Did not finish. `DominatingSetProblemSuite` stayed inside genetic-search duplicate checks (`toCSVString` → `FormatUtil.formatNumber`, a global lock) for more than 90s and was killed. Filtered suites under `strategy`, `parameter`, `optimizee`, and `viewer` were 242 tests, 0 failures. `ParabolaMinFunctionProblemSuite`, `SevenElevenProblemSuite`, `SubsetSumProblemSuite`, and `TravelingSalesmanProblemSuite` passed when run on their own. |
| `./gradlew test --tests SlowIntegrationSuite` | Fails immediately with "No tests found". `build.gradle` excludes `*SlowIntegrationSuite*` from the `test` task. The suite is `./gradlew slowTest` (one brute-force pass over a standard TSP, two subset-sum variants, and `TYPICAL_DS`). That task was still running when a 90s cap expired, so it is not part of the 288. |
| `./gradlew test` **after** the fixes on this branch | **288 tests, 0 failures, 0 errors**, about 6s. `DominatingSetProblemSuite` finished in 3.9s (genetic search 1.3s, concurrent genetic search 2.1s). |
| `./gradlew test` after the scoreboard and the viewer applicability change | **299 tests, 0 failures, 0 errors**, about 8s. That includes `StrategyScoreboardSuite`, `OptimizationStrategyTypeSuite` (6), and `OptimizerEvalStatusSuite` (4). |

## Results before fixes

Optimum is 0 for every column. `throw` means the run aborted with an exception. `hang` means the 1D brute-force grid is 2,147,483,647 points and was not executed (the viewer left it on "Running…" until a 40s timeout).

| Strategy | demo-2d | wavy | grid | line | ackley | rastrigin | rosenbrock | sphere |
|---|---|---|---|---|---|---|---|---|
| GLOBAL_SAMPLING | 0.005 (3/3) | 0.003 (3/3) | 0 (3/3) | 0 (3/3) | 4.4e-16 (3/3) | 0 (3/3) | 0.260 (0/3) | 0 (3/3) |
| GLOBAL_HILL_CLIMBING | 3.7e-10 (3/3) | 0.001 (3/3) | throw | throw | 4.4e-16 (3/3) | 0 (3/3) | 6.2e-08 (3/3) | 0 (3/3) |
| GLOBAL_SIMULATED_ANNEALING | 9.2e-08 (3/3) | 0.001 (3/3) | throw | 0 (3/3) | 4.4e-16 (3/3) | 0 (3/3) | 5.4e-07 (3/3) | 0 (3/3) |
| HILL_CLIMBING | 5.9e-10 (3/3) | 0.001 (3/3) | 0 (3/3) | 0 (3/3) | 19.18 (0/3) | 31.84 (0/3) | 4.9e-07 (3/3) | 4.2e-19 (3/3) |
| SIMULATED_ANNEALING | 2.3e-07 (3/3) | 0.002 (2/3) | 0 (3/3) | 0 (3/3) | 1.1e-05 (3/3) | 1.99 (0/3) | 0.117 (0/3) | 3.8e-12 (3/3) |
| GENETIC_SEARCH | 3.0e-04 (3/3) | 0.001 (3/3) | 0 (3/3) | 0 (3/3) | 0.262 (0/3) | 4.5e-05 (2/3) | 0.002 (3/3) | 7.7e-06 (3/3) |
| CONCURRENT_GENETIC_SEARCH | 3.0e-04 (3/3) | 0.001 (3/3) | 0 (3/3) | 0 (3/3) | 0.262 (0/3) | 4.5e-05 (2/3) | 0.002 (3/3) | 7.7e-06 (3/3) |
| STATE_SPACE_SEARCH | throw | throw | 0 (3/3) | 0 (3/3) | throw | throw | throw | throw |
| BRUTE_FORCE | 0.566 (0/3) | 0.609 (0/3) | 0.566 (0/3) | hang | 21.57 (0/3) | 57.85 (0/3) | 90036 (0/3) | 52.43 (0/3) |
| DIFFERENTIAL_EVOLUTION | 0.141 (0/3) | 0.023 (0/3) | 0.141 (0/3) | 1.00 (0/3) | 17.02 (0/3) | 22.63 (0/3) | 9.11 (0/3) | 4.61 (0/3) |
| PARTICLE_SWARM | 5.6e-17 (3/3) | 0.001 (3/3) | 0 (3/3) | 0 (3/3) | 2.9e-14 (3/3) | 0 (3/3) | 2.3e-10 (3/3) | 3.3e-19 (3/3) |
| ITERATED_LOCAL_SEARCH | 0.005 (3/3) | 0.001 (3/3) | 0 (3/3) | 0 (3/3) | 1.63 (0/3) | 0.086 (0/3) | 7.5e-04 (1/3) | 6.4e-04 (3/3) |
| VARIABLE_NEIGHBORHOOD_SEARCH | 0.005 (3/3) | 0.001 (3/3) | 0 (3/3) | 0 (3/3) | 0.593 (0/3) | 0.125 (0/3) | 0.024 (0/3) | 3.0e-04 (3/3) |

Evaluation cost on the three classic continuous problems (median of three seeds, before fixes):

| Strategy | Sphere evals / ms | Ackley evals / ms | Rosenbrock evals / ms |
|---|---|---|---|
| HILL_CLIMBING | 237 / <1 | 171 / <1 | 31,676 / 63 |
| SIMULATED_ANNEALING | 561 / 1 | 561 / 1 | 561 / 1 |
| GENETIC_SEARCH | 7,032 / 277 | 6,328 / 231 | 5,897 / 275 |
| DIFFERENTIAL_EVOLUTION | 40,041 / 40 (22 unique) | 40,041 / 44 (24 unique) | 40,041 / 37 (28 unique) |
| PARTICLE_SWARM | 4,830 / 10 | 8,490 / 25 | 7,470 / 11 |
| BRUTE_FORCE | 1 / <1 | 1 / <1 | 1 / <1 |
| GLOBAL_HILL_CLIMBING | 1,596 / 4 | 1,844 / 4 | 201,517 / 329 |

Concurrent genetic search matched sequential genetic search fitness on every problem and seed. It spent a similar amount of time. Particle swarm was the strongest default on these continuous functions. Hill climbing is deterministic across seeds (it does not use the supplied `Random`).

Global sampling "solves" sphere, Ackley, and Rastrigin because an odd number of samples on a symmetric domain lands on the origin. Rosenbrock's optimum is `(1, 1)`, off that grid point, so the same sampler stops at 0.260. That is grid resolution, not a broken sampler. The wavy polynomial and the 2D demo bottom out near 0.001 and 0.0046 for the same reason: the sample or the basin does not contain the exact target.

## What is working

- **Particle swarm** reaches the optimum on every problem here (sphere ~1e-18, Ackley ~1e-14, Rastrigin 0, Rosenbrock ~1e-9, both discrete demos exact).
- **Hill climbing** and **global hill climbing** solve smooth unimodal surfaces (sphere, Rosenbrock, the 2D demo). Hill climbing on Ackley (fitness 19.18) and Rastrigin (31.84) stops in a local basin. The path on Ackley never enters the central hole. That is the algorithm, and the viewer makes it obvious.
- **Simulated annealing** solves sphere, Ackley, and the discrete demos inside a short schedule (~561 evaluations). It misses the 0.01 band on Rastrigin (best 1.99) and Rosenbrock (best 0.117, spread up to 4.38).
- **Genetic search** is reliable on sphere, Rosenbrock, the demo, and the discrete problems. Ackley stalls around 0.26–0.90. Rastrigin converges on 2 of 3 seeds.
- **Iterated local search and variable neighborhood search** are exact on the integer line and grid, which is the domain their comments describe. On continuous Ackley, Rastrigin, and Rosenbrock they improve on the start and still miss 0.01 most of the time. Viewer problems embed `new Random(1)`, and the default acceptance mode is `BetterOrEqual`, so the strategy `Random` is unused and demo/wavy results are identical across seeds.
- **State-space search** is exact on the line and the grid (5 and 12 evaluations). On every other problem it throws `IllegalArgumentException: STATE_SPACE_SEARCH requires an Optimizee that mixes in DiscreteStateSpace`. The viewer shows "did not complete".

## Screenshots (before fixes)

All 104 frames: `/opt/cursor/artifacts/screenshots/{problem}__{strategy}.png`. Status lines: `/opt/cursor/artifacts/gui_status.tsv`.

Representative frames:

- Sphere hill climbing reaches the origin: `screenshots/sphere__hill_climbing.png` (fitness 4.19e-19).
- Sphere brute force returns one corner, fitness 52.43, worse than the start at 18: `screenshots/sphere__brute_force.png`.
- Sphere differential evolution stops on its initial cloud, fitness 4.82: `screenshots/sphere__differential_evolution.png`.
- Ackley hill climbing sits in an outer basin, fitness 19.18: `screenshots/ackley__hill_climbing.png`.
- 1D global hill climbing dies with "assertion failed": `screenshots/line__global_hill_climbing.png`.
- Sphere genetic search draws a real population, then the status bar says "Cancelled.": `screenshots/sphere__genetic_search.png`.

The viewer also timed out (40s, status still "Cancelled." or "Running…") on grid genetic search, grid concurrent genetic search, line concurrent genetic search, and line brute force.

## Bugs

Line numbers are the versions on `master` before this branch.

### 1. Differential evolution never builds a trial vector

`Parameter.setValue` returns a new instance. `createTrialVector` aliased the target and discarded that instance:

```121:140:scala-source/com/barrybecker4/optimization/strategy/DifferentialEvolutionStrategy.scala
val trial = target
...
trial.get(j).setValue(clampedVal)
...
trial
```

(The citation above is the pre-fix source; this branch replaces the method.) Every "trial" was the target, so selection never changed the population. Initialization used `getRandomNeighbor(1.0)` (a Gaussian around the guess) rather than a uniform draw (`DifferentialEvolutionStrategy.scala` line 96). `selectThreeDistinct` (lines 146–155) loops until it has three donors and does not terminate when `populationSize < 4`. `OptimizationStrategyType.getStrategy` constructed `new DifferentialEvolutionStrategy(optimizee)` and dropped the caller's `Random` (line 93).

Repro, seeds 1/42/99, default population 40 and 1000 generations: 40,041 evaluations, about 22 unique candidates, sphere best fitness 4.61 (median 10.55, which is exactly the old unit-test ceiling). The 2D demo fitness stayed at the initial 0.141. `populationSize = 3` did not return within 40s.

The unit tests asserted `fitness <= 10.55` on the sphere and `<= 20.9` on Ackley, so they passed while the search was a no-op.

### 2. Brute force searches one wrapped sample, or billions

`BruteForceStrategy.doOptimization` (line 22) asks for `findGlobalSamples(Long.MaxValue)`. `NumericGlobalSampler.determineSamplingRate` (line 44) does `pow(requested, 1/dims).toInt`. `MultiDimensionalIndexer` then multiplies those per-axis counts in a 32-bit int.

- Two dimensions: the product wraps and the iterator has a single sample. Sphere fitness 52.43 (start 18), Rosenbrock 90,036 (start 24.2), demo and grid 0.566 (start 0.141). One evaluation, and the result is worse than the start whenever that corner is worse.
- One dimension: the rate is `Int.MaxValue` (2,147,483,647). The line demo does not finish. The viewer was still "Running…" at 40s.

### 3. Integer samples exclude both endpoints, then assert

`NumericParameterArray.getRandomSample` (lines 128–130) drew `min + nextDouble * range` and asserted `min < value < max`. For an integer parameter that truncates, so `[0, 5]` never yields 5, and a draw that truncates to 0 throws `AssertionError: newPar 0.0 not between 0.0 and 20.0`. A 4,000-draw probe on `[0, 5]` threw 781 times, saw a maximum of 4, and never saw 5.

That is why **global hill climbing** and **global simulated annealing** throw on the 2D integer grid, and why global simulated annealing throws on the line in the viewer. The numeric bench threw on the grid for all three seeds. The viewer's own `Random(1)` happened to finish the grid at fitness 0, so the failure is seed-dependent.

### 4. A gradient step that crosses a bound leaves the range or sticks

`DoubleParameter.getIncrementForDirection` (lines 56–60): if `value + increment > maxValue` it returns `maxValue` itself (an absolute coordinate, not a delta). `incrementByEps` then adds that to `value` and `getValue` asserts. Repro: `DoubleParameter(5.119, -5.12, 5.12, "x").incrementByEps(FORWARD)` → `AssertionError: Value 10.239 outside range`. A backward step from `-5.119` returns 0 and the parameter does not move.

`IntegerParameter.incrementByEps` (lines 44–45) and `AbstractIntParameter.incrementByEps` (lines 37–38) do `(getValue + direction).toInt` with no clamp. `IntegerParameter(5, 0, 5, "i")` steps to 6. `getValue` on integers does not validate, so this is silent. `BooleanParameter(true)` is the same path: the suite expected a forward step of `2.0` on a parameter whose range is `[0, 1]`.

### 5. Hill-climbing cache retries grow the radius until an assert

`ImprovementStep.findNextCandidateParams` (lines 60–63) retries a cached integer neighbor and multiplies `gaussRadius` by 1.5 with no cap. `AbstractParameter.tweakNumericValue` (line 36) asserts `|r| <= 1.5`. Global hill climbing on the 1D line hits that assert (`screenshots/line__global_hill_climbing.png`, status "assertion failed").

### 6. A cancelled viewer run overwrites a finished one

`OptimizerEvalPanel.startOptimizationRun` (lines 108–117) always applies `onComplete`. Changing the combo box cancels the in-flight worker; that worker's `done()` runs later and sets the status to "Cancelled." even when the replacement run succeeded. `screenshots/sphere__genetic_search.png` shows a finished population and the status "Cancelled."

`OptimizationSwingWorker.doInBackground` (lines 257–258) catches `IllegalArgumentException`, prints it, and returns `None` with no error attached. The panel then says "Optimization did not complete" and hides the message. State-space search on a continuous problem takes this path. So does brute force on a continuous space after the fix below. This follow-up stops catching `IllegalArgumentException`. The status line is `Failed (STRATEGY):` plus `getMessage`. `AbstractMethodError` still becomes an empty result.

### 7. `Optimizer` drops the caller's `Random` for several strategies

`OptimizationStrategyType.getStrategy` (lines 93–99) ignored `rnd` for differential evolution, particle swarm, iterated local search, and variable neighborhood search. Particle swarm therefore used `MathUtil.RANDOM`. `IteratedLocalSearchStrategy.strategyFor` constructed `new Random(1)`. Reseeding only the `Optimizer` argument did not reseed those searches.

### 8. Genetic duplicate checks serialize on a formatter, and one notification is the parent

`GeneticSearchStrategy.sameSolution` (line 20) compared `toCSVString`, which formats every coordinate under `FormatUtil`'s lock. `DominatingSetProblemSuite` (population cap 4,000; see `AbstractParameterArray.getSamplePopulationSize`, `POPULATION_MAX = 4000`) did not finish in 90s. In the greedy mutation branch, `notifyOfChange(p)` (line 364) reported the keeper, not the child `nbr`.

### Other defects that are real and smaller

- `Optimizee.getOptimalFitness` is documented as "the best (largest) expected fitness" (`Optimizee.scala` line 39). Every strategy minimizes, and 0 is best.
- The README strategy list stops at brute force and does not mention differential evolution, particle swarm, iterated local search, or variable neighborhood search. The `valuesForUi` comment that said the catalog excluded state-space search now says the dropdown filters that catalog with `appliesTo`.
- `IntegerParameter.setValue` still does not clamp. Only the epsilon step was corrected. A caller can still store an integer outside `[min, max]`.

## Fixes on this branch, with before / after

Low-risk corrections only. The table is best fitness and converged seeds, same protocol as above.

| Case | Before | After |
|---|---|---|
| Differential evolution, sphere | 4.61 (0/3), 40,041 evals, ~22 unique | ~1e-158 (3/3), 40,040 evals, ~40,000 unique |
| Differential evolution, Ackley | 17.02 (0/3) | 4.4e-16 (3/3) |
| Differential evolution, Rastrigin | 22.63 (0/3) | 0 (3/3) |
| Differential evolution, Rosenbrock | 9.11 (0/3) | 0 (3/3) |
| Differential evolution, demo / grid / line | 0.141 / 0.141 / 1.00 (0/3) | 0 / 0 / 0 (3/3) |
| Brute force, 2D continuous (sphere, Ackley, Rosenbrock, demo, wavy) | 1 eval, often worse than the start (sphere 52.43, Rosenbrock 90,036) | `IllegalArgumentException`: continuous space is not enumerable |
| Brute force, integer grid | 0.566 (0/3), 1 eval | 0 (3/3), 177 evals |
| Brute force, integer line | hang (2.1e9 sampler points) | 0 at x=7, walk of 0..7 (8 evals), 106 ms |
| Global hill climbing, grid and line | throw | 0 (3/3) |
| Global simulated annealing, grid | throw | 0 (3/3) |
| `DominatingSetProblemSuite` | did not finish (>90s) | 8 tests, 3.9s |
| Viewer status after a replaced run | "Cancelled." on a finished sphere genetic search | "Completed (GENETIC_SEARCH) … fitness=4.03e-5" |

After-fix frames are under `/opt/cursor/artifacts/screenshots-after/`:

- `sphere__differential_evolution.png` — fitness 1.06e-158, marker on the origin.
- `ackley__differential_evolution.png` and `rosenbrock__differential_evolution.png` — fitness ~0.
- `line__brute_force.png` and `grid__brute_force.png` — fitness 0.
- `line__global_hill_climbing.png` and `line__global_simulated_annealing.png` — fitness 0.
- `sphere__brute_force.png` — "did not complete" (the illegal-argument message is swallowed; see bug 6).
- `sphere__genetic_search.png` — status now says Completed.

One side effect: global simulated annealing on Rosenbrock moved from 3/3 converged (worst 0.0044) to 2/3 (seed 99 fitness 0.026, still far below the start at 24.2). The bound-step change alters gradient steps near the edge of the domain, so that local search is not the same trajectory. Sphere, Ackley, Rastrigin, and the discrete problems for that strategy still converge.

What the patch does:

- Differential evolution builds a new `NumericParameterArray` from `setValue` results, samples the initial population uniformly with the strategy `Random`, requires `populationSize >= 4`, notifies each generation, and returns the best so far if the evaluation budget is exceeded.
- Brute force enumerates an inclusive integer lattice (cap 1,000,000 candidates, stop at the known optimum) and refuses a continuous `NumericParameterArray`. Permutations still use `findGlobalSamples`.
- `NumericGlobalSampler` rejects a non-positive request and a grid whose per-axis count would overflow a 32-bit product.
- Integer `getRandomSample` draws inclusive `[min, max]`. The range assert is inclusive.
- Double steps that would cross a bound land on the bound. Integer and boolean epsilon steps clamp to `[min, max]`.
- The hill-climbing cache retry stops after 24 attempts and never passes a radius above 1.5.
- Genetic `sameSolution` compares numeric values. The greedy path notifies the child. `getStrategy` forwards `rnd` to differential evolution, particle swarm, and both local-search presets.
- A stale SwingWorker can no longer overwrite the status of the run that replaced it.

## Suggestions, in order

1. **Headless scoreboard in CI.** Done in `StrategyScoreboardSuite`. `./gradlew build` (the CI workflow) runs every strategy on sphere, Ackley, Rastrigin, Rosenbrock, the integer line, and the integer grid, for seeds 1, 42, and 99. Each run must finish at least as good as its start. Differential evolution and particle swarm must reach 1e-2 everywhere in that set. Brute force and state-space search must reach 0 on the lattices and refuse a continuous space. A 300,000-evaluation budget and a 90-second test limit stop a non-terminating search.
2. **Make illegal strategy/problem pairs visible.** Done. `OptimizationStrategyType.appliesTo(optimizee, params)` is what the viewer dropdown uses. State-space search applies when the optimizee is a `DiscreteStateSpace`. Brute force applies to integer lattices and non-numeric arrays, and refuses a `NumericParameterArray` that has a continuous parameter. Differential evolution and particle swarm apply to absolute-fitness numeric arrays. Hill climbing and global hill climbing apply to numeric, permuted, and variable-length parameters. Everything else in `valuesForUi` stays offered. Changing the problem rebuilds the combo and keeps the current strategy when it still applies. A thrown `IllegalArgumentException` reaches the status bar as `Failed (STRATEGY): <message>`. Screenshots: `/opt/cursor/artifacts/screenshots-applicability/before/` (sphere list still contains `STATE_SPACE_SEARCH` and `BRUTE_FORCE`; status "Optimization did not complete") and `.../after/` (those two are gone on the sphere, present on the 1D line, and a state-space run on the sphere shows `Failed (STATE_SPACE_SEARCH): STATE_SPACE_SEARCH requires an Optimizee that mixes in DiscreteStateSpace`).
3. **Document the strategies that already exist.** The README stops at brute force. Differential evolution, particle swarm, iterated local search, and variable neighborhood search need a sentence each on the problem shape they fit. Correct `getOptimalFitness` to say the fitness is minimized.
4. **Give genetic search a cheaper identity and a smaller default population.** `getSamplePopulationSize` is `min(4000, 4^n)` for integer parameters. Duplicate checks were linear scans through a formatter. A hash of the parameter values, and a population that grows with dimension instead of jumping to the cap, is what made the dominating-set case usable after the equality change; the cap itself is still blunt.
5. **Clamp in `IntegerParameter.setValue`**, the same way `incrementByEps` now clamps. Redistribution functions need a test so the clamp does not change their inverse.
6. **Algorithms still missing for the surfaces these demos use.** Nelder–Mead or another direct simplex search would solve Rosenbrock without 30,000 gradient evaluations. A covariance method (CMA-ES) is the usual next step after differential evolution for rotated valleys. Iterated local search's default acceptance ignores its `Random` on `BetterOrEqual`, so passing `rnd` through `strategyFor` does not diversify the viewer problems that carry their own `Random(1)`; a Metropolis preset should be the one the enum selects if seeds are supposed to matter.
7. **`Parameter.setValue` returning a new instance is an easy foot-gun.** Bug 1 is that pattern. A test that a trial vector differs from its target, or a builder that mutates and copies once, would lock the invariant in. The same pattern is easy to reintroduce in any new strategy.
8. **Point `test --tests SlowIntegrationSuite` at `slowTest`.** The exclude filter makes the obvious command report that the suite does not exist.
