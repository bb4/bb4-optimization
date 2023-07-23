# bb4-optimization

A collection of heuristic optimization algorithms.
The algorithms are implemented mostly as described by Michalewicz and Fogel in ["How to Solve It: Modern Heuristics"](http://www.amazon.com/How-Solve-It-Modern-Heuristics/dp/3540224947)

An Optimizer uses a specified optimization strategy (see OptimizationStrategyType) to optimize an Optimizee.
Optimization is nearly the same thing as search. In [bb4-puzzles](https://github.com/bb4/bb4-puzzles), [bb4-games](https://github.com/bb4/bb4-games), and [bb4-simulations](https://github.com/bb4/bb4-simulations) implementations, various types of optimization strategies from this library are used to search for solutions.

### How to Build
Type 'gradlew build' at the root (or ./gradlew if running in Cygwin). This is mainly a library project, but there are several interesting test cases that can be viewed visually.
If you want to open the source in Intellij, then first run 'gradlew idea'.
There is a simple visualization of a trivial optimization problem that can be viewed by running 'gradlew run'.

When there is a new release, versioned artifacts will be published by Barry Becker to [Sonatype](https://oss.sonatype.org).

### License
All source (unless otherwise specified in individual file) is provided under the [MIT License](http://www.opensource.org/licenses/MIT)





