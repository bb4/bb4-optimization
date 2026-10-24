# Benchmark results

Written by `./gradlew benchmark`; see [improvement-plan.md](improvement-plan.md). Use `git diff` to see what
a change did, and only compare runs made on the same machine.

Java 21.0.10, 14 processors, 5 seeds, 5000 ms or 5000000 evaluations per run

| Problem | Strategy | Evals/sec | Bytes/eval | Median fitness | Best fitness | Median evals | Stopped by |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| tsp-standard | SIMULATED_ANNEALING | 29,758 | 1,251 | 0.0000 | 0.0000 | 6 | finished |
| tsp-standard | GLOBAL_SIMULATED_ANNEALING | 38,491 | 1,738 | 0.0000 | 0.0000 | 39 | finished |
| tsp-standard | HILL_CLIMBING | 19,215 | 3,048 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | GLOBAL_HILL_CLIMBING | 14,520 | 4,528 | 0.0000 | 0.0000 | 15 | finished |
| tsp-standard | GENETIC_SEARCH | 1,202 | 4,570,095 | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | CONCURRENT_GENETIC_SEARCH | 989 | n/a | 0.0000 | 0.0000 | 11 | finished |
| tsp-standard | ITERATED_LOCAL_SEARCH | 244,888 | 741 | 0.0000 | 0.0000 | 3 | finished |
| tsp-standard | VARIABLE_NEIGHBORHOOD_SEARCH | 269,687 | 741 | 0.0000 | 0.0000 | 3 | finished |
| tsp-us-capitals | SIMULATED_ANNEALING | 1,909,312 | 1,022 | 36754.3413 | 30154.8212 | 13,441 | finished |
| tsp-us-capitals | GLOBAL_SIMULATED_ANNEALING | 1,703,179 | 1,130 | 28760.7948 | 27052.8957 | 68,205 | finished |
| tsp-us-capitals | HILL_CLIMBING | 320,423 | 6,603 | 98617.7492 | 88206.8986 | 1,151 | finished |
| tsp-us-capitals | GLOBAL_HILL_CLIMBING | 462,117 | 5,502 | 92899.6330 | 84267.9722 | 6,313 | finished |
| tsp-us-capitals | GENETIC_SEARCH | 1,721,754 | 1,647 | 25512.1398 | 22972.8115 | 655,686 | finished |
| tsp-us-capitals | CONCURRENT_GENETIC_SEARCH | 842,145 | n/a | 25512.1398 | 22972.8115 | 655,686 | finished |
| tsp-us-capitals | ITERATED_LOCAL_SEARCH | 1,170,970 | 1,216 | 46366.9413 | 44689.3909 | 94,881 | finished |
| tsp-us-capitals | VARIABLE_NEIGHBORHOOD_SEARCH | 1,045,658 | 1,216 | 46368.4842 | 41995.5052 | 87,291 | finished |
| tour-50 | SIMULATED_ANNEALING | 1,891,537 | 336 | 8.3669 | 7.6767 | 14,001 | finished |
| tour-50 | GLOBAL_SIMULATED_ANNEALING | 3,531,789 | 360 | 8.0496 | 7.0996 | 71,005 | finished |
| tour-50 | HILL_CLIMBING | 583,354 | 6,647 | 19.9245 | 19.1931 | 1,129 | finished |
| tour-50 | GLOBAL_HILL_CLIMBING | 527,497 | 5,655 | 18.3689 | 16.6756 | 6,426 | finished |
| tour-50 | GENETIC_SEARCH | 1,757,280 | 1,329 | 6.5351 | 6.2003 | 626,178 | finished |
| tour-50 | CONCURRENT_GENETIC_SEARCH | 868,980 | n/a | 6.5351 | 6.2003 | 626,178 | finished |
| tour-50 | ITERATED_LOCAL_SEARCH | 1,140,087 | 1,240 | 11.2871 | 9.8050 | 89,620 | finished |
| tour-50 | VARIABLE_NEIGHBORHOOD_SEARCH | 1,343,250 | 1,242 | 11.0278 | 10.1265 | 81,518 | finished |
| tour-200 | SIMULATED_ANNEALING | 4,657,241 | 644 | 26.5353 | 24.4351 | 56,001 | finished |
| tour-200 | GLOBAL_SIMULATED_ANNEALING | 3,031,364 | 660 | 24.7823 | 23.1749 | 281,005 | finished |
| tour-200 | HILL_CLIMBING | 139,800 | 25,610 | 91.0628 | 88.1335 | 1,107 | finished |
| tour-200 | GLOBAL_HILL_CLIMBING | 102,712 | 22,047 | 89.2896 | 87.0760 | 6,691 | finished |
| tour-200 | GENETIC_SEARCH | 584,354 | 4,405 | 34.4515 | 33.6188 | 982,267 | finished |
| tour-200 | CONCURRENT_GENETIC_SEARCH | 315,947 | n/a | 34.4515 | 33.6188 | 982,267 | finished |
| tour-200 | ITERATED_LOCAL_SEARCH | 407,342 | 4,719 | 76.8015 | 74.7946 | 73,416 | finished |
| tour-200 | VARIABLE_NEIGHBORHOOD_SEARCH | 417,725 | 4,720 | 76.1377 | 73.6846 | 68,763 | finished |
| subset-sum | SIMULATED_ANNEALING | 104,058 | 7,644 | 0.0000 | 0.0000 | 362 | finished |
| subset-sum | GLOBAL_SIMULATED_ANNEALING | 155,542 | 6,238 | 0.0000 | 0.0000 | 1,721 | finished |
| subset-sum | HILL_CLIMBING | 159,256 | 9,447 | 0.0000 | 0.0000 | 87 | finished |
| subset-sum | GLOBAL_HILL_CLIMBING | 114,031 | 17,308 | 0.0000 | 0.0000 | 2,287 | finished |
| subset-sum | GENETIC_SEARCH | 314,901 | 6,141 | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | CONCURRENT_GENETIC_SEARCH | 212,014 | n/a | 0.0000 | 0.0000 | 451 | finished |
| subset-sum | ITERATED_LOCAL_SEARCH | 328,884 | 8,185 | 0.0000 | 0.0000 | 77 | finished |
| subset-sum | VARIABLE_NEIGHBORHOOD_SEARCH | 235,459 | 10,653 | 0.0000 | 0.0000 | 618 | finished |
| dominating-set | SIMULATED_ANNEALING | 159,403 | 11,848 | 0.6000 | 0.0000 | 1,731 | finished |
| dominating-set | GLOBAL_SIMULATED_ANNEALING | 189,225 | 11,610 | 0.2000 | 0.0000 | 9,445 | finished |
| dominating-set | HILL_CLIMBING | 123,210 | 24,655 | 1.2000 | 1.0000 | 903 | finished |
| dominating-set | GLOBAL_HILL_CLIMBING | 116,837 | 22,136 | 0.6000 | 0.6000 | 5,511 | finished |
| dominating-set | GENETIC_SEARCH | 196,316 | 13,625 | 0.0000 | 0.0000 | 29,872 | finished |
| dominating-set | CONCURRENT_GENETIC_SEARCH | 169,953 | n/a | 0.0000 | 0.0000 | 61,572 | finished |
| dominating-set | ITERATED_LOCAL_SEARCH | 201,191 | 13,539 | 0.0000 | 0.0000 | 8,747 | finished |
| dominating-set | VARIABLE_NEIGHBORHOOD_SEARCH | 198,718 | 13,624 | 0.0000 | 0.0000 | 8,906 | finished |
