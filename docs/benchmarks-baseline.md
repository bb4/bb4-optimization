# Benchmark baseline (before the improvements in the plan)

These are the numbers to beat, recorded at 1000 ms per run before the default became 5000 ms. The current numbers, which the benchmark rewrites
on every full run, are in [benchmark-results.md](benchmark-results.md). See [improvement-plan.md](improvement-plan.md) for what is planned and why.

Reproduce with `./gradlew benchmark -Pargs="--time-ms=1000 --seeds=5 --report=none"`, on the version of the library you want to measure.
Each run is stopped after 1000 ms or 5,000,000 evaluations, whichever comes first. Every row is the median of seeds 1 to 5,
after one unmeasured warm-up run. Numbers depend on the machine, so only compare runs made on the same one.

- **Machine:** Java 21.0.12, 2 processors (a cloud VM), nothing else running.
- **Version:** bb4-optimization 2.1-SNAPSHOT at master (8db9cda) plus the `getSamplePopulationSize` overflow fix. Without that
  fix, genetic search fails on `tour-50` and `tour-200`.

## How to read the table

- **Evals/sec** is evaluations of the objective per second. **Bytes/eval** is bytes allocated per evaluation by the thread
  running the search (`n/a` for the concurrent genetic search, which uses several threads).
- **Median fitness** and **Best fitness** are tour lengths or error counts. Lower is better, 0 is optimal for `tsp-standard`, `subset-sum`
  and `dominating-set`.
- **Stopped by** says why the runs ended: `time` or `evals` (the limit stopped them), `finished` (the strategy decided to stop)
  or `mixed`. A strategy that finishes on its own has a fixed amount of work, so for those the fitness is the interesting
  number and evals/sec is the speed.
- `tsp-standard`, `subset-sum` and `dominating-set` are small and finish in a few milliseconds, so their evals/sec are noisy.
  Use `tour-50`, `tour-200` and `tsp-us-capitals` to measure speed.

## Results

| Problem | Strategy | Evals/sec | Bytes/eval | Median fitness | Best fitness | Median evals | Stopped by |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| tsp-standard | SIMULATED_ANNEALING | 16,470 | 643 | 0.0000 | 0.0000 | 6 | finished |
| tsp-standard | GLOBAL_SIMULATED_ANNEALING | 18,874 | 1,149 | 0.0000 | 0.0000 | 39 | finished |
| tsp-standard | HILL_CLIMBING | 6,728 | 3,165 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | GLOBAL_HILL_CLIMBING | 5,779 | 4,586 | 0.0000 | 0.0000 | 15 | finished |
| tsp-standard | GENETIC_SEARCH | 208 | 7,745,651 | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | CONCURRENT_GENETIC_SEARCH | 342 | n/a | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | ITERATED_LOCAL_SEARCH | 16,303 | 1,640 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | VARIABLE_NEIGHBORHOOD_SEARCH | 17,310 | 1,640 | 0.0000 | 0.0000 | 3 | finished |
| tsp-us-capitals | SIMULATED_ANNEALING | 1,411,671 | 791 | 36754.3413 | 30154.8212 | 13,441 | finished |
| tsp-us-capitals | GLOBAL_SIMULATED_ANNEALING | 1,282,981 | 737 | 28760.7948 | 27052.8957 | 68,205 | finished |
| tsp-us-capitals | HILL_CLIMBING | 1,097 | 2,781,244 | 100031.5328 | 88206.8986 | 1,078 | mixed |
| tsp-us-capitals | GLOBAL_HILL_CLIMBING | 2,064 | 1,292,629 | 98645.3512 | 84267.9722 | 2,073 | time |
| tsp-us-capitals | GENETIC_SEARCH | 272,125 | 4,071 | 33308.8983 | 31965.1347 | 272,154 | time |
| tsp-us-capitals | CONCURRENT_GENETIC_SEARCH | 280,640 | n/a | 32497.0565 | 30241.0403 | 281,022 | time |
| tsp-us-capitals | ITERATED_LOCAL_SEARCH | 46,312 | 11,018 | 48771.6619 | 46715.2753 | 46,318 | time |
| tsp-us-capitals | VARIABLE_NEIGHBORHOOD_SEARCH | 45,945 | 11,017 | 47880.0668 | 44498.9828 | 45,951 | time |
| tour-50 | SIMULATED_ANNEALING | 1,415,879 | 736 | 8.3669 | 7.6767 | 14,001 | finished |
| tour-50 | GLOBAL_SIMULATED_ANNEALING | 1,354,499 | 751 | 8.0496 | 7.0996 | 71,005 | finished |
| tour-50 | HILL_CLIMBING | 1,092 | 3,057,982 | 19.6587 | 19.1931 | 1,094 | time |
| tour-50 | GLOBAL_HILL_CLIMBING | 2,905 | 1,166,474 | 18.7724 | 17.6285 | 2,906 | time |
| tour-50 | GENETIC_SEARCH | 279,825 | 4,082 | 8.7961 | 7.3979 | 281,101 | time |
| tour-50 | CONCURRENT_GENETIC_SEARCH | 233,309 | n/a | 9.1408 | 8.4664 | 233,350 | time |
| tour-50 | ITERATED_LOCAL_SEARCH | 42,057 | 12,020 | 11.3912 | 10.4938 | 42,061 | time |
| tour-50 | VARIABLE_NEIGHBORHOOD_SEARCH | 44,974 | 12,019 | 11.4890 | 10.1265 | 44,977 | time |
| tour-200 | SIMULATED_ANNEALING | 477,112 | 1,939 | 26.5353 | 24.4351 | 56,001 | finished |
| tour-200 | GLOBAL_SIMULATED_ANNEALING | 391,986 | 1,948 | 24.7823 | 23.1749 | 281,005 | finished |
| tour-200 | HILL_CLIMBING | 920 | 2,812,167 | 91.0628 | 88.1335 | 923 | time |
| tour-200 | GLOBAL_HILL_CLIMBING | 1,882 | 1,270,036 | 92.0451 | 91.8622 | 1,894 | time |
| tour-200 | GENETIC_SEARCH | 60,488 | 16,094 | 79.3701 | 77.1636 | 60,891 | time |
| tour-200 | CONCURRENT_GENETIC_SEARCH | 59,883 | n/a | 79.3701 | 78.1082 | 61,502 | time |
| tour-200 | ITERATED_LOCAL_SEARCH | 9,654 | 49,316 | 77.6651 | 74.7946 | 9,655 | time |
| tour-200 | VARIABLE_NEIGHBORHOOD_SEARCH | 10,240 | 49,314 | 76.1377 | 75.2947 | 10,242 | time |
| subset-sum | SIMULATED_ANNEALING | 33,101 | 7,484 | 0.0000 | 0.0000 | 362 | finished |
| subset-sum | GLOBAL_SIMULATED_ANNEALING | 25,472 | 6,454 | 0.0000 | 0.0000 | 1,721 | finished |
| subset-sum | HILL_CLIMBING | 36,758 | 10,562 | 0.0000 | 0.0000 | 86 | finished |
| subset-sum | GLOBAL_HILL_CLIMBING | 20,724 | 28,813 | 0.0000 | 0.0000 | 1,429 | finished |
| subset-sum | GENETIC_SEARCH | 83,493 | 6,530 | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | CONCURRENT_GENETIC_SEARCH | 116,922 | n/a | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | ITERATED_LOCAL_SEARCH | 78,385 | 9,382 | 0.0000 | 0.0000 | 77 | finished |
| subset-sum | VARIABLE_NEIGHBORHOOD_SEARCH | 51,050 | 13,903 | 0.0000 | 0.0000 | 618 | finished |
| dominating-set | SIMULATED_ANNEALING | 47,229 | 11,880 | 0.6000 | 0.0000 | 1,731 | finished |
| dominating-set | GLOBAL_SIMULATED_ANNEALING | 77,081 | 11,543 | 0.2000 | 0.0000 | 9,445 | finished |
| dominating-set | HILL_CLIMBING | 27,405 | 47,901 | 1.2000 | 1.0000 | 422 | finished |
| dominating-set | GLOBAL_HILL_CLIMBING | 31,294 | 37,141 | 0.6000 | 0.6000 | 3,047 | finished |
| dominating-set | GENETIC_SEARCH | 80,910 | 13,426 | 0.0000 | 0.0000 | 40,443 | finished |
| dominating-set | CONCURRENT_GENETIC_SEARCH | 78,581 | n/a | 0.0000 | 0.0000 | 50,704 | finished |
| dominating-set | ITERATED_LOCAL_SEARCH | 49,765 | 15,319 | 0.0000 | 0.0000 | 11,415 | finished |
| dominating-set | VARIABLE_NEIGHBORHOOD_SEARCH | 67,953 | 15,339 | 0.0000 | 0.0000 | 2,865 | finished |

## What stands out

- **Hill climbing on permutations is extremely slow and allocates heavily:** about 1,000 evaluations per second and
  2.8 to 3 MB allocated per evaluation on `tour-50`, `tour-200` and `tsp-us-capitals`. That is 1,000 times slower than simulated annealing on
  the same problems. It looks like it builds and checks a large set of neighbors on each step. This is the biggest single target for Phase 1.
- **Genetic search allocates 4 to 16 KB per evaluation and 7.7 MB per evaluation on `tsp-standard`**, where it spends its few
  evaluations building a large initial population.
- **Iterated local search and variable neighborhood search allocate 11 to 49 KB per evaluation** and run at about 10 to 45 thousand
  evaluations per second, 10 to 30 times slower than simulated annealing, which allocates under 2 KB.
- **Simulated annealing is the fastest** (0.4 to 1.4 million evaluations per second) and the best on the tours, but its schedule is
  fixed, so it finishes after a fixed number of evaluations. On `tour-200` that is 56,000 evaluations, about 0.12 s, and it reaches about 26.5,
  where genetic search and iterated local search reach 77 to 79 in a full second.
- **Quality on the 200 city tour is poor for everything except annealing.** That is the kind of problem delta evaluation
  helps most, because an evaluation reads all 200 cities and a swap changes two of them.
