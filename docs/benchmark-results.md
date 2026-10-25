# Benchmark results

Written by `./gradlew benchmark`; see [improvement-plan.md](improvement-plan.md). Use `git diff` to see what
a change did, and only compare runs made on the same machine.

Java 21.0.10, 14 processors, 5 seeds, 5000 ms or 5000000 evaluations per run

| Problem | Strategy | Evals/sec | Bytes/eval | Median fitness | Best fitness | Median evals | Stopped by |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| tsp-standard | SIMULATED_ANNEALING | 25,171 | 1,251 | 0.0000 | 0.0000 | 6 | finished |
| tsp-standard | GLOBAL_SIMULATED_ANNEALING | 36,334 | 1,726 | 0.0000 | 0.0000 | 39 | finished |
| tsp-standard | HILL_CLIMBING | 15,852 | 3,048 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | GLOBAL_HILL_CLIMBING | 15,984 | 4,532 | 0.0000 | 0.0000 | 15 | finished |
| tsp-standard | GENETIC_SEARCH | 1,192 | 4,570,096 | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | CONCURRENT_GENETIC_SEARCH | 1,095 | n/a | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | ITERATED_LOCAL_SEARCH | 187,494 | 741 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | VARIABLE_NEIGHBORHOOD_SEARCH | 211,439 | 741 | 0.0000 | 0.0000 | 3 | finished |
| tsp-us-capitals | SIMULATED_ANNEALING | 1,613,808 | 888 | 36754.3413 | 30154.8212 | 13,441 | finished |
| tsp-us-capitals | GLOBAL_SIMULATED_ANNEALING | 1,905,951 | 904 | 28760.7948 | 27052.8957 | 68,205 | finished |
| tsp-us-capitals | HILL_CLIMBING | 322,471 | 6,603 | 98617.7492 | 88206.8986 | 1,151 | finished |
| tsp-us-capitals | GLOBAL_HILL_CLIMBING | 447,237 | 5,504 | 92899.6330 | 84267.9722 | 6,313 | finished |
| tsp-us-capitals | GENETIC_SEARCH | 1,654,449 | 1,618 | 25512.1398 | 22972.8115 | 655,686 | finished |
| tsp-us-capitals | CONCURRENT_GENETIC_SEARCH | 651,006 | n/a | 25512.1398 | 22972.8115 | 655,686 | finished |
| tsp-us-capitals | ITERATED_LOCAL_SEARCH | 1,287,574 | 1,216 | 46366.9413 | 44689.3909 | 94,881 | finished |
| tsp-us-capitals | VARIABLE_NEIGHBORHOOD_SEARCH | 1,289,615 | 1,216 | 46368.4842 | 41995.5052 | 87,291 | finished |
| tour-50 | SIMULATED_ANNEALING | 1,735,867 | 305 | 8.3669 | 7.6767 | 14,001 | finished |
| tour-50 | GLOBAL_SIMULATED_ANNEALING | 4,134,769 | 328 | 8.0496 | 7.0996 | 71,005 | finished |
| tour-50 | HILL_CLIMBING | 607,329 | 6,647 | 19.9245 | 19.1931 | 1,129 | finished |
| tour-50 | GLOBAL_HILL_CLIMBING | 506,781 | 5,651 | 18.3689 | 16.6756 | 6,426 | finished |
| tour-50 | GENETIC_SEARCH | 1,483,152 | 1,383 | 6.5351 | 6.2003 | 626,178 | finished |
| tour-50 | CONCURRENT_GENETIC_SEARCH | 771,677 | n/a | 6.5351 | 6.2003 | 626,178 | finished |
| tour-50 | ITERATED_LOCAL_SEARCH | 1,304,276 | 1,240 | 11.2871 | 9.8050 | 89,620 | finished |
| tour-50 | VARIABLE_NEIGHBORHOOD_SEARCH | 1,325,034 | 1,242 | 11.0278 | 10.1265 | 81,518 | finished |
| tour-200 | SIMULATED_ANNEALING | 4,524,892 | 606 | 26.5353 | 24.4351 | 56,001 | finished |
| tour-200 | GLOBAL_SIMULATED_ANNEALING | 3,049,539 | 622 | 24.7823 | 23.1749 | 281,005 | finished |
| tour-200 | HILL_CLIMBING | 126,312 | 25,610 | 91.0628 | 88.1335 | 1,107 | finished |
| tour-200 | GLOBAL_HILL_CLIMBING | 103,614 | 22,044 | 89.2896 | 87.0760 | 6,691 | finished |
| tour-200 | GENETIC_SEARCH | 416,552 | 4,915 | 34.4515 | 33.6188 | 982,267 | finished |
| tour-200 | CONCURRENT_GENETIC_SEARCH | 262,298 | n/a | 34.4515 | 33.6188 | 982,267 | finished |
| tour-200 | ITERATED_LOCAL_SEARCH | 420,945 | 4,719 | 76.8015 | 74.7946 | 73,416 | finished |
| tour-200 | VARIABLE_NEIGHBORHOOD_SEARCH | 415,373 | 4,720 | 76.1377 | 73.6846 | 68,763 | finished |
| subset-sum | SIMULATED_ANNEALING | 105,801 | 7,595 | 0.0000 | 0.0000 | 362 | finished |
| subset-sum | GLOBAL_SIMULATED_ANNEALING | 183,663 | 6,101 | 0.0000 | 0.0000 | 1,721 | finished |
| subset-sum | HILL_CLIMBING | 169,563 | 9,227 | 0.0000 | 0.0000 | 87 | finished |
| subset-sum | GLOBAL_HILL_CLIMBING | 124,638 | 17,219 | 0.0000 | 0.0000 | 2,287 | finished |
| subset-sum | GENETIC_SEARCH | 318,641 | 6,036 | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | CONCURRENT_GENETIC_SEARCH | 242,311 | n/a | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | ITERATED_LOCAL_SEARCH | 368,275 | 8,057 | 0.0000 | 0.0000 | 77 | finished |
| subset-sum | VARIABLE_NEIGHBORHOOD_SEARCH | 257,335 | 10,565 | 0.0000 | 0.0000 | 618 | finished |
| dominating-set | SIMULATED_ANNEALING | 182,185 | 11,848 | 0.6000 | 0.0000 | 1,731 | finished |
| dominating-set | GLOBAL_SIMULATED_ANNEALING | 203,730 | 11,699 | 0.2000 | 0.0000 | 9,445 | finished |
| dominating-set | HILL_CLIMBING | 129,905 | 24,740 | 1.2000 | 1.0000 | 903 | finished |
| dominating-set | GLOBAL_HILL_CLIMBING | 120,893 | 22,214 | 0.6000 | 0.6000 | 5,511 | finished |
| dominating-set | GENETIC_SEARCH | 213,868 | 13,240 | 0.0000 | 0.0000 | 39,877 | finished |
| dominating-set | CONCURRENT_GENETIC_SEARCH | 177,390 | n/a | 0.0000 | 0.0000 | 29,599 | finished |
| dominating-set | ITERATED_LOCAL_SEARCH | 213,469 | 13,640 | 0.0000 | 0.0000 | 8,933 | finished |
| dominating-set | VARIABLE_NEIGHBORHOOD_SEARCH | 217,019 | 13,662 | 0.0000 | 0.0000 | 851 | finished |
