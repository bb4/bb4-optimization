# Benchmark results

Written by `./gradlew benchmark`; see [improvement-plan.md](improvement-plan.md). Use `git diff` to see what
a change did, and only compare runs made on the same machine.

Java 21.0.12.1, 2 processors, 5 seeds, 5000 ms or 5000000 evaluations per run

| Problem | Strategy | Evals/sec | Bytes/eval | Median fitness | Best fitness | Median evals | Stopped by |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| tsp-standard | SIMULATED_ANNEALING | 12,633 | 1,251 | 0.0000 | 0.0000 | 6 | finished |
| tsp-standard | GLOBAL_SIMULATED_ANNEALING | 15,400 | 1,732 | 0.0000 | 0.0000 | 39 | finished |
| tsp-standard | HILL_CLIMBING | 6,574 | 3,432 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | GLOBAL_HILL_CLIMBING | 5,934 | 4,906 | 0.0000 | 0.0000 | 15 | finished |
| tsp-standard | GENETIC_SEARCH | 241 | 4,617,748 | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | CONCURRENT_GENETIC_SEARCH | 274 | n/a | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | ITERATED_LOCAL_SEARCH | 59,515 | 699 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | VARIABLE_NEIGHBORHOOD_SEARCH | 55,346 | 699 | 0.0000 | 0.0000 | 3 | finished |
| tsp-us-capitals | SIMULATED_ANNEALING | 528,277 | 1,050 | 36754.3413 | 30154.8212 | 13,441 | finished |
| tsp-us-capitals | GLOBAL_SIMULATED_ANNEALING | 625,873 | 993 | 28760.7948 | 27052.8957 | 68,205 | finished |
| tsp-us-capitals | HILL_CLIMBING | 89,189 | 6,643 | 98617.7492 | 88206.8986 | 1,151 | finished |
| tsp-us-capitals | GLOBAL_HILL_CLIMBING | 134,210 | 5,660 | 92899.6330 | 84267.9722 | 6,313 | finished |
| tsp-us-capitals | GENETIC_SEARCH | 513,992 | 1,657 | 25512.1398 | 22972.8115 | 655,686 | finished |
| tsp-us-capitals | CONCURRENT_GENETIC_SEARCH | 577,367 | n/a | 25512.1398 | 22972.8115 | 655,686 | finished |
| tsp-us-capitals | ITERATED_LOCAL_SEARCH | 385,063 | 1,217 | 46366.9413 | 44689.3909 | 94,881 | finished |
| tsp-us-capitals | VARIABLE_NEIGHBORHOOD_SEARCH | 324,159 | 1,217 | 46368.4842 | 41995.5052 | 87,291 | finished |
| tour-50 | SIMULATED_ANNEALING | 1,155,608 | 343 | 8.3669 | 7.6767 | 14,001 | finished |
| tour-50 | GLOBAL_SIMULATED_ANNEALING | 1,349,132 | 369 | 8.0496 | 7.0996 | 71,005 | finished |
| tour-50 | HILL_CLIMBING | 137,214 | 6,764 | 19.9245 | 19.1931 | 1,129 | finished |
| tour-50 | GLOBAL_HILL_CLIMBING | 109,696 | 5,683 | 18.3689 | 16.6756 | 6,426 | finished |
| tour-50 | GENETIC_SEARCH | 597,121 | 1,359 | 6.5351 | 6.2003 | 626,178 | finished |
| tour-50 | CONCURRENT_GENETIC_SEARCH | 569,100 | n/a | 6.5351 | 6.2003 | 626,178 | finished |
| tour-50 | ITERATED_LOCAL_SEARCH | 623,588 | 1,241 | 11.2871 | 9.8050 | 89,620 | finished |
| tour-50 | VARIABLE_NEIGHBORHOOD_SEARCH | 419,993 | 1,242 | 11.0278 | 10.1265 | 81,518 | finished |
| tour-200 | SIMULATED_ANNEALING | 1,365,343 | 654 | 26.5353 | 24.4351 | 56,001 | finished |
| tour-200 | GLOBAL_SIMULATED_ANNEALING | 797,559 | 666 | 24.7823 | 23.1749 | 281,005 | finished |
| tour-200 | HILL_CLIMBING | 21,353 | 25,852 | 91.0628 | 88.1335 | 1,107 | finished |
| tour-200 | GLOBAL_HILL_CLIMBING | 31,886 | 21,942 | 89.2896 | 87.0760 | 6,691 | finished |
| tour-200 | GENETIC_SEARCH | 218,529 | 4,529 | 34.4515 | 33.6188 | 980,902 | mixed |
| tour-200 | CONCURRENT_GENETIC_SEARCH | 149,265 | n/a | 39.0634 | 36.7969 | 747,032 | time |
| tour-200 | ITERATED_LOCAL_SEARCH | 164,853 | 4,719 | 76.8015 | 74.7946 | 73,416 | finished |
| tour-200 | VARIABLE_NEIGHBORHOOD_SEARCH | 171,475 | 4,720 | 76.1377 | 73.6846 | 68,763 | finished |
| subset-sum | SIMULATED_ANNEALING | 21,335 | 7,512 | 0.0000 | 0.0000 | 362 | finished |
| subset-sum | GLOBAL_SIMULATED_ANNEALING | 35,145 | 6,501 | 0.0000 | 0.0000 | 1,721 | finished |
| subset-sum | HILL_CLIMBING | 48,011 | 9,821 | 0.0000 | 0.0000 | 87 | finished |
| subset-sum | GLOBAL_HILL_CLIMBING | 39,818 | 17,304 | 0.0000 | 0.0000 | 2,287 | finished |
| subset-sum | GENETIC_SEARCH | 61,872 | 6,149 | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | CONCURRENT_GENETIC_SEARCH | 73,911 | n/a | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | ITERATED_LOCAL_SEARCH | 58,981 | 8,156 | 0.0000 | 0.0000 | 77 | finished |
| subset-sum | VARIABLE_NEIGHBORHOOD_SEARCH | 71,591 | 10,625 | 0.0000 | 0.0000 | 618 | finished |
| dominating-set | SIMULATED_ANNEALING | 44,449 | 11,878 | 0.6000 | 0.0000 | 1,731 | finished |
| dominating-set | GLOBAL_SIMULATED_ANNEALING | 74,750 | 11,585 | 0.2000 | 0.0000 | 9,445 | finished |
| dominating-set | HILL_CLIMBING | 50,816 | 24,652 | 1.2000 | 1.0000 | 903 | finished |
| dominating-set | GLOBAL_HILL_CLIMBING | 41,249 | 22,123 | 0.6000 | 0.6000 | 5,511 | finished |
| dominating-set | GENETIC_SEARCH | 65,464 | 13,059 | 0.0000 | 0.0000 | 40,527 | finished |
| dominating-set | CONCURRENT_GENETIC_SEARCH | 70,524 | n/a | 0.0000 | 0.0000 | 39,346 | finished |
| dominating-set | ITERATED_LOCAL_SEARCH | 66,404 | 13,473 | 0.0000 | 0.0000 | 13,555 | finished |
| dominating-set | VARIABLE_NEIGHBORHOOD_SEARCH | 75,726 | 13,672 | 0.0000 | 0.0000 | 5,367 | finished |
