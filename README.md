# bb4-optimization

[![CI](https://github.com/bb4/bb4-optimization/actions/workflows/ci.yml/badge.svg)](https://github.com/bb4/bb4-optimization/actions/workflows/ci.yml)

📊 [Build status for all bb4 projects](https://github.com/bb4/.github)

Heuristic search and optimization algorithms for continuous and discrete parameter spaces. Implement an `Optimizee`, pick a strategy (hill climbing, simulated annealing, genetic search, and others), and let `Optimizer` find a good solution — used by [bb4-puzzles](https://github.com/bb4/bb4-puzzles), [bb4-games](https://github.com/bb4/bb4-games), and [bb4-simulations](https://github.com/bb4/bb4-simulations). Algorithms largely follow Michalewicz and Fogel's [*How to Solve It: Modern Heuristics*](https://www.amazon.com/How-Solve-It-Modern-Heuristics/dp/3540224947).

## Using it

```groovy
implementation 'com.barrybecker4:bb4-optimization:2.0.0'
```

See the [releases page](https://github.com/bb4/bb4-optimization/releases) or [Maven Central](https://central.sonatype.com/artifact/com.barrybecker4/bb4-optimization) for newer versions.

## What's inside

- **`Optimizer`** — facade that runs a chosen strategy against an `Optimizee`; 
  returns **`OptimizationResult`** (`best`, `status`, optional `evaluationsUsed`) with optional logging, listeners, and evaluation budget.
- **`OptimizationResult` / `OptimizationStatus`** — when `maxEvaluations` is set, exceeding the budget yields `BudgetExhausted` and **`best` is best-so-far** (no exception to the caller)
- **`Optimizee` / `AbsoluteOptimizee`** — interface (and absolute-fitness adapter) for the thing being optimized; fitness is minimized (0 is best)
- **`BudgetedOptimizee`** — wraps an optimizee with a hard evaluation limit (preserves `DiscreteStateSpace` when the delegate has it)
- **`DiscreteStateSpace`** — mix-in for discrete graph problems (`successors`, optional `heuristic`); keep branching factor small
- **`StateSpaceSearchConfig` / `StateSpaceSearchMode`** — best-first search over `DiscreteStateSpace` with canonical visited-state keys, budget-safe best-so-far, optional **A\*** (`fitness + heuristic`); 
  prefer vs hill climbing when you can enumerate neighbors, vs brute force when the graph is large but structured.
- **`OptimizationStrategyType`** — 
  hill climbing, global sampling / **global hill climbing** (diverse multistart seeds via `GlobalHillClimbingConfig`), 
  **global simulated annealing** (same multistart pattern with SA locals via `GlobalSimulatedAnnealingConfig`), 
  simulated annealing (`SimulatedAnnealingConfig`: adaptive acceptance by default, budget-safe best-so-far), 
  **genetic / concurrent genetic** (`GeneticSearchConfig`: elitism, cull/crossover, neighbor modes, budget-safe evolve), 
  **state-space search**, and brute force
- **`SimulatedAnnealingConfig` / `SimulatedAnnealingStepMode`** — cooling schedule and proposal radius; 
  default `AdaptiveAcceptance` scales steps from Metropolis acceptance rate on continuous problems (discrete spaces automatically use Fixed radius); proposal radius is clamped to 1.5
- **`GlobalSimulatedAnnealingConfig`** — global sample budget + multistart SA from diverse / top-fitness seeds (prefer over single-chain SA on multimodal discrete problems; 
  prefer `GLOBAL_HILL_CLIMBING` when local optima are shallow)
- **`GeneticSearchConfig` / `NeighborSelection`** — tunables for genetic search; 
  concurrent genetic parallelizes offspring evaluation (optimizee must be thread-safe for `evaluateFitness` / `compareFitness`)
- **`parameter`** — typed parameters (`DoubleParameter`, `IntegerParameter`, `BooleanParameter`, …), 
  arrays (`NumericParameterArray`, `PermutedParameterArray`, `VariableLengthIntSet`), 
  sampling, distance metrics, and optional redistribution functions
- **`viewer` / `OptimizerEvalApp`** — Swing UI that visualizes strategies on demo problems (`./gradlew run`)

## Building from source

See the [Building bb4 Projects wiki](https://github.com/bb4/bb4-common/wiki/Building-bb4-Projects).

## License

MIT — see [LICENSE](LICENSE).
