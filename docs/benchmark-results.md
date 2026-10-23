# Benchmark results

Written by `./gradlew benchmark`; see [improvement-plan.md](improvement-plan.md). Use `git diff` to see what
a change did, and only compare runs made on the same machine.

Java 21.0.12.1, 2 processors, 5 seeds, 5000 ms or 5000000 evaluations per run

| Problem | Strategy | Evals/sec | Bytes/eval | Median fitness | Best fitness | Median evals | Stopped by |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| tsp-standard | SIMULATED_ANNEALING | 18,028 | 689 | 0.0000 | 0.0000 | 6 | finished |
| tsp-standard | GLOBAL_SIMULATED_ANNEALING | 12,368 | 1,195 | 0.0000 | 0.0000 | 39 | finished |
| tsp-standard | HILL_CLIMBING | 4,827 | 3,432 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | GLOBAL_HILL_CLIMBING | 3,952 | 4,906 | 0.0000 | 0.0000 | 15 | finished |
| tsp-standard | GENETIC_SEARCH | 168 | 5,411,465 | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | CONCURRENT_GENETIC_SEARCH | 494 | n/a | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | ITERATED_LOCAL_SEARCH | 65,922 | 732 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | VARIABLE_NEIGHBORHOOD_SEARCH | 81,934 | 699 | 0.0000 | 0.0000 | 3 | finished |
| tsp-us-capitals | SIMULATED_ANNEALING | 1,219,181 | 584 | 36754.3413 | 30154.8212 | 13,441 | finished |
| tsp-us-capitals | GLOBAL_SIMULATED_ANNEALING | 1,015,121 | 532 | 28760.7948 | 27052.8957 | 68,205 | finished |
| tsp-us-capitals | HILL_CLIMBING | 105,879 | 6,648 | 98617.7492 | 88206.8986 | 1,151 | finished |
| tsp-us-capitals | GLOBAL_HILL_CLIMBING | 152,843 | 5,619 | 92899.6330 | 84267.9722 | 6,313 | finished |
| tsp-us-capitals | GENETIC_SEARCH | 370,555 | 1,323 | 25512.1398 | 22972.8115 | 655,686 | finished |
| tsp-us-capitals | CONCURRENT_GENETIC_SEARCH | 428,174 | n/a | 25512.1398 | 22972.8115 | 655,686 | finished |
| tsp-us-capitals | ITERATED_LOCAL_SEARCH | 330,455 | 1,217 | 46366.9413 | 44689.3909 | 94,881 | finished |
| tsp-us-capitals | VARIABLE_NEIGHBORHOOD_SEARCH | 303,895 | 1,217 | 46368.4842 | 41995.5052 | 87,291 | finished |
| tour-50 | SIMULATED_ANNEALING | 1,085,876 | 592 | 8.3669 | 7.6767 | 14,001 | finished |
| tour-50 | GLOBAL_SIMULATED_ANNEALING | 1,110,810 | 538 | 8.0496 | 7.0996 | 71,005 | finished |
| tour-50 | HILL_CLIMBING | 189,089 | 6,764 | 19.9245 | 19.1931 | 1,129 | finished |
| tour-50 | GLOBAL_HILL_CLIMBING | 135,340 | 5,686 | 18.3689 | 16.6756 | 6,426 | finished |
| tour-50 | GENETIC_SEARCH | 607,103 | 1,328 | 6.5324 | 5.9456 | 614,507 | finished |
| tour-50 | CONCURRENT_GENETIC_SEARCH | 604,188 | n/a | 6.5324 | 5.9456 | 614,507 | finished |
| tour-50 | ITERATED_LOCAL_SEARCH | 343,950 | 1,241 | 11.2871 | 9.8050 | 89,620 | finished |
| tour-50 | VARIABLE_NEIGHBORHOOD_SEARCH | 421,022 | 1,242 | 11.0278 | 10.1265 | 81,518 | finished |
| tour-200 | SIMULATED_ANNEALING | 556,680 | 1,123 | 26.5353 | 24.4351 | 56,001 | finished |
| tour-200 | GLOBAL_SIMULATED_ANNEALING | 399,867 | 1,135 | 24.7823 | 23.1749 | 281,005 | finished |
| tour-200 | HILL_CLIMBING | 25,827 | 25,853 | 91.0628 | 88.1335 | 1,107 | finished |
| tour-200 | GLOBAL_HILL_CLIMBING | 22,914 | 21,949 | 89.2896 | 87.0760 | 6,691 | finished |
| tour-200 | GENETIC_SEARCH | 181,947 | 4,556 | 33.8232 | 32.9201 | 911,409 | mixed |
| tour-200 | CONCURRENT_GENETIC_SEARCH | 136,818 | n/a | 39.0495 | 38.2571 | 684,104 | time |
| tour-200 | ITERATED_LOCAL_SEARCH | 171,915 | 4,720 | 76.8015 | 74.7946 | 73,416 | finished |
| tour-200 | VARIABLE_NEIGHBORHOOD_SEARCH | 198,039 | 4,720 | 76.1377 | 73.6846 | 68,763 | finished |
| subset-sum | SIMULATED_ANNEALING | 17,010 | 7,512 | 0.0000 | 0.0000 | 362 | finished |
| subset-sum | GLOBAL_SIMULATED_ANNEALING | 32,791 | 6,505 | 0.0000 | 0.0000 | 1,721 | finished |
| subset-sum | HILL_CLIMBING | 47,721 | 10,058 | 0.0000 | 0.0000 | 87 | finished |
| subset-sum | GLOBAL_HILL_CLIMBING | 26,921 | 17,326 | 0.0000 | 0.0000 | 2,287 | finished |
| subset-sum | GENETIC_SEARCH | 68,649 | 6,123 | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | CONCURRENT_GENETIC_SEARCH | 78,288 | n/a | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | ITERATED_LOCAL_SEARCH | 63,925 | 8,155 | 0.0000 | 0.0000 | 77 | finished |
| subset-sum | VARIABLE_NEIGHBORHOOD_SEARCH | 62,247 | 10,631 | 0.0000 | 0.0000 | 618 | finished |
| dominating-set | SIMULATED_ANNEALING | 42,009 | 11,715 | 0.6000 | 0.0000 | 1,731 | finished |
| dominating-set | GLOBAL_SIMULATED_ANNEALING | 70,194 | 11,596 | 0.2000 | 0.0000 | 9,445 | finished |
| dominating-set | HILL_CLIMBING | 47,431 | 24,601 | 1.2000 | 1.0000 | 903 | finished |
| dominating-set | GLOBAL_HILL_CLIMBING | 44,803 | 22,095 | 0.6000 | 0.6000 | 5,511 | finished |
| dominating-set | GENETIC_SEARCH | 58,180 | 13,118 | 0.0000 | 0.0000 | 39,494 | finished |
| dominating-set | CONCURRENT_GENETIC_SEARCH | 73,385 | n/a | 0.0000 | 0.0000 | 40,166 | finished |
| dominating-set | ITERATED_LOCAL_SEARCH | 78,292 | 13,427 | 0.0000 | 0.0000 | 8,420 | finished |
| dominating-set | VARIABLE_NEIGHBORHOOD_SEARCH | 70,287 | 13,531 | 0.0000 | 0.0000 | 9,895 | finished |
