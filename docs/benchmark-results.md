# Benchmark results

Written by `./gradlew benchmark`; see [improvement-plan.md](improvement-plan.md). Use `git diff` to see what
a change did, and only compare runs made on the same machine.

Java 21.0.12.1, 2 processors, 5 seeds, 5000 ms or 5000000 evaluations per run

| Problem | Strategy | Evals/sec | Bytes/eval | Median fitness | Best fitness | Median evals | Stopped by |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| tsp-standard | SIMULATED_ANNEALING | 12,990 | 643 | 0.0000 | 0.0000 | 6 | finished |
| tsp-standard | GLOBAL_SIMULATED_ANNEALING | 12,212 | 1,149 | 0.0000 | 0.0000 | 39 | finished |
| tsp-standard | HILL_CLIMBING | 4,953 | 3,165 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | GLOBAL_HILL_CLIMBING | 3,650 | 4,582 | 0.0000 | 0.0000 | 15 | finished |
| tsp-standard | GENETIC_SEARCH | 221 | 7,745,651 | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | CONCURRENT_GENETIC_SEARCH | 264 | n/a | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | ITERATED_LOCAL_SEARCH | 14,307 | 1,640 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | VARIABLE_NEIGHBORHOOD_SEARCH | 12,811 | 1,640 | 0.0000 | 0.0000 | 3 | finished |
| tsp-us-capitals | SIMULATED_ANNEALING | 879,103 | 791 | 36754.3413 | 30154.8212 | 13,441 | finished |
| tsp-us-capitals | GLOBAL_SIMULATED_ANNEALING | 755,097 | 738 | 28760.7948 | 27052.8957 | 68,205 | finished |
| tsp-us-capitals | HILL_CLIMBING | 916 | 3,361,937 | 100031.5328 | 88206.8986 | 1,099 | finished |
| tsp-us-capitals | GLOBAL_HILL_CLIMBING | 1,054 | 2,483,990 | 92899.6330 | 84267.9722 | 5,276 | time |
| tsp-us-capitals | GENETIC_SEARCH | 247,872 | 3,537 | 25512.1398 | 22972.8115 | 655,686 | finished |
| tsp-us-capitals | CONCURRENT_GENETIC_SEARCH | 271,752 | n/a | 25512.1398 | 22972.8115 | 655,686 | finished |
| tsp-us-capitals | ITERATED_LOCAL_SEARCH | 39,241 | 11,019 | 46366.9413 | 44689.3909 | 94,881 | finished |
| tsp-us-capitals | VARIABLE_NEIGHBORHOOD_SEARCH | 40,182 | 11,018 | 46368.4842 | 41995.5052 | 87,291 | finished |
| tour-50 | SIMULATED_ANNEALING | 965,625 | 759 | 8.3669 | 7.6767 | 14,001 | finished |
| tour-50 | GLOBAL_SIMULATED_ANNEALING | 1,092,751 | 751 | 8.0496 | 7.0996 | 71,005 | finished |
| tour-50 | HILL_CLIMBING | 670 | 4,197,278 | 19.6587 | 19.1931 | 1,134 | mixed |
| tour-50 | GLOBAL_HILL_CLIMBING | 1,228 | 2,365,357 | 18.3689 | 16.6756 | 6,141 | mixed |
| tour-50 | GENETIC_SEARCH | 285,203 | 3,601 | 6.5324 | 5.9456 | 614,507 | finished |
| tour-50 | CONCURRENT_GENETIC_SEARCH | 286,157 | n/a | 6.5324 | 5.9456 | 614,507 | finished |
| tour-50 | ITERATED_LOCAL_SEARCH | 41,369 | 12,019 | 11.2871 | 9.8050 | 89,620 | finished |
| tour-50 | VARIABLE_NEIGHBORHOOD_SEARCH | 39,904 | 12,020 | 11.0278 | 10.1265 | 81,518 | finished |
| tour-200 | SIMULATED_ANNEALING | 437,359 | 1,939 | 26.5353 | 24.4351 | 56,001 | finished |
| tour-200 | GLOBAL_SIMULATED_ANNEALING | 363,226 | 1,948 | 24.7823 | 23.1749 | 281,005 | finished |
| tour-200 | HILL_CLIMBING | 225 | 11,906,145 | 91.0628 | 88.1335 | 1,081 | mixed |
| tour-200 | GLOBAL_HILL_CLIMBING | 412 | 6,194,342 | 92.0451 | 91.8622 | 2,063 | time |
| tour-200 | GENETIC_SEARCH | 73,581 | 13,921 | 53.6410 | 50.3077 | 367,913 | time |
| tour-200 | CONCURRENT_GENETIC_SEARCH | 69,066 | n/a | 53.6524 | 52.5545 | 347,170 | time |
| tour-200 | ITERATED_LOCAL_SEARCH | 9,397 | 49,313 | 76.8015 | 74.7946 | 46,990 | time |
| tour-200 | VARIABLE_NEIGHBORHOOD_SEARCH | 9,879 | 49,322 | 76.1377 | 75.2947 | 49,398 | time |
| subset-sum | SIMULATED_ANNEALING | 33,744 | 7,442 | 0.0000 | 0.0000 | 362 | finished |
| subset-sum | GLOBAL_SIMULATED_ANNEALING | 44,387 | 6,473 | 0.0000 | 0.0000 | 1,721 | finished |
| subset-sum | HILL_CLIMBING | 38,852 | 10,766 | 0.0000 | 0.0000 | 86 | finished |
| subset-sum | GLOBAL_HILL_CLIMBING | 20,695 | 28,813 | 0.0000 | 0.0000 | 1,429 | finished |
| subset-sum | GENETIC_SEARCH | 104,644 | 6,495 | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | CONCURRENT_GENETIC_SEARCH | 104,972 | n/a | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | ITERATED_LOCAL_SEARCH | 91,483 | 9,329 | 0.0000 | 0.0000 | 77 | finished |
| subset-sum | VARIABLE_NEIGHBORHOOD_SEARCH | 52,389 | 13,852 | 0.0000 | 0.0000 | 618 | finished |
| dominating-set | SIMULATED_ANNEALING | 50,923 | 11,807 | 0.6000 | 0.0000 | 1,731 | finished |
| dominating-set | GLOBAL_SIMULATED_ANNEALING | 78,698 | 11,623 | 0.2000 | 0.0000 | 9,445 | finished |
| dominating-set | HILL_CLIMBING | 21,237 | 48,001 | 1.2000 | 1.0000 | 422 | finished |
| dominating-set | GLOBAL_HILL_CLIMBING | 25,248 | 37,233 | 0.6000 | 0.6000 | 3,047 | finished |
| dominating-set | GENETIC_SEARCH | 58,698 | 13,929 | 0.0000 | 0.0000 | 29,929 | finished |
| dominating-set | CONCURRENT_GENETIC_SEARCH | 64,994 | n/a | 0.0000 | 0.0000 | 29,864 | finished |
| dominating-set | ITERATED_LOCAL_SEARCH | 51,741 | 15,341 | 0.0000 | 0.0000 | 3,323 | finished |
| dominating-set | VARIABLE_NEIGHBORHOOD_SEARCH | 49,604 | 15,340 | 0.0000 | 0.0000 | 6,201 | finished |
