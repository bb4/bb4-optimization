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

- **`Optimizer`** — facade that runs a chosen strategy against an `Optimizee` (optional logging, listeners, evaluation budget)
- **`Optimizee` / `AbsoluteOptimizee`** — interface (and absolute-fitness adapter) for the thing being optimized; fitness is minimized (0 is best)
- **`BudgetedOptimizee`** — wraps an optimizee with a hard evaluation limit
- **`DiscreteStateSpace`** — marker for discrete problems used with state-space search
- **`OptimizationStrategyType`** — hill climbing, global sampling / global hill climbing, simulated annealing, genetic search (including concurrent), state-space search, and brute force
- **`parameter`** — typed parameters (`DoubleParameter`, `IntegerParameter`, `BooleanParameter`, …), arrays (`NumericParameterArray`, `PermutedParameterArray`, `VariableLengthIntSet`), sampling, distance metrics, and optional redistribution functions
- **`viewer` / `OptimizerEvalApp`** — Swing UI that visualizes strategies on demo problems (`./gradlew run`)

## Building from source

See the [Building bb4 Projects wiki](https://github.com/bb4/bb4-common/wiki/Building-bb4-Projects).

## License

MIT — see [LICENSE](LICENSE).
