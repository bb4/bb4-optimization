# Solve rate results

Written by `./gradlew benchmark -Pargs="--solve-rate=true"`; see [new-strategies-plan.md](new-strategies-plan.md).
Each run stops when it solves the problem, so the time is the time to a solution. The unsolved fitness is the
median number of constraints still broken in the runs that did not. Only compare runs made on the same machine.

Java 21.0.12.1, 2 processors, 20 seeds, 2000 ms or 5000000 evaluations per run

| Problem | Strategy | Solved | Median ms (solved) | Median evals (solved) | Median broken (unsolved) |
| --- | --- | ---: | ---: | ---: | ---: |
| queens-100 | SIMULATED_ANNEALING | 20/20 | 60 | 17,517 | - |
| queens-100 | GLOBAL_SIMULATED_ANNEALING | 20/20 | 62 | 18,800 | - |
| queens-100 | HILL_CLIMBING | 0/20 | - | - | 39.0 |
| queens-100 | GLOBAL_HILL_CLIMBING | 0/20 | - | - | 36.0 |
| queens-100 | GENETIC_SEARCH | 0/20 | - | - | 2.0 |
| queens-100 | CONCURRENT_GENETIC_SEARCH | 0/20 | - | - | 2.0 |
| queens-100 | ITERATED_LOCAL_SEARCH | 0/20 | - | - | 20.0 |
| queens-100 | VARIABLE_NEIGHBORHOOD_SEARCH | 0/20 | - | - | 20.0 |
| queens-400 | SIMULATED_ANNEALING | 20/20 | 818 | 97,017 | - |
| queens-400 | GLOBAL_SIMULATED_ANNEALING | 20/20 | 993 | 94,068 | - |
| queens-400 | HILL_CLIMBING | 0/20 | - | - | 206.0 |
| queens-400 | GLOBAL_HILL_CLIMBING | 0/20 | - | - | 199.0 |
| queens-400 | GENETIC_SEARCH | 0/20 | - | - | 155.5 |
| queens-400 | CONCURRENT_GENETIC_SEARCH | 0/20 | - | - | 155.0 |
| queens-400 | ITERATED_LOCAL_SEARCH | 0/20 | - | - | 155.0 |
| queens-400 | VARIABLE_NEIGHBORHOOD_SEARCH | 0/20 | - | - | 153.0 |
| coloring-200 | SIMULATED_ANNEALING | 0/20 | - | - | 141.0 |
| coloring-200 | HILL_CLIMBING | 0/20 | - | - | 107.5 |
| coloring-200 | GENETIC_SEARCH | 0/20 | - | - | 116.0 |
| coloring-200 | CONCURRENT_GENETIC_SEARCH | 0/20 | - | - | 115.0 |
| coloring-200 | ITERATED_LOCAL_SEARCH | 0/20 | - | - | 131.0 |
| coloring-200 | VARIABLE_NEIGHBORHOOD_SEARCH | 0/20 | - | - | 132.0 |
| all-interval-14 | SIMULATED_ANNEALING | 1/20 | 2 | 1,771 | 1.0 |
| all-interval-14 | GLOBAL_SIMULATED_ANNEALING | 1/20 | 9 | 9,764 | 1.0 |
| all-interval-14 | HILL_CLIMBING | 0/20 | - | - | 2.0 |
| all-interval-14 | GLOBAL_HILL_CLIMBING | 0/20 | - | - | 1.0 |
| all-interval-14 | GENETIC_SEARCH | 10/20 | 205 | 58,450 | 1.0 |
| all-interval-14 | CONCURRENT_GENETIC_SEARCH | 10/20 | 219 | 58,451 | 1.0 |
| all-interval-14 | ITERATED_LOCAL_SEARCH | 5/20 | 3 | 2,436 | 1.0 |
| all-interval-14 | VARIABLE_NEIGHBORHOOD_SEARCH | 4/20 | 17 | 9,609 | 1.0 |
