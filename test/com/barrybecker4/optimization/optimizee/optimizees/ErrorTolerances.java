package com.barrybecker4.optimization.optimizee.optimizees;

import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

import java.util.HashMap;
import java.util.Map;

import static com.barrybecker4.optimization.strategy.OptimizationStrategyType.*;

/**
 * @author Barry Becker
 */
public class ErrorTolerances {

    private Map<OptimizationStrategyType, Double> percentValues = new HashMap<>();

    public ErrorTolerances(double globalSampling, double globalHillClimbing,
                           double hillClimbing, double simAnnealing, double tabuSearch,
                           double geneticSearch, double concGenSearch, double stateSpace)  {
        percentValues.put(GLOBAL_SAMPLING, globalSampling);
        percentValues.put(GLOBAL_HILL_CLIMBING, globalHillClimbing);
        percentValues.put(HILL_CLIMBING, hillClimbing);
        percentValues.put(SIMULATED_ANNEALING, simAnnealing);
        percentValues.put(TABU_SEARCH, tabuSearch);
        percentValues.put(GENETIC_SEARCH, geneticSearch);
        percentValues.put(CONCURRENT_GENETIC_SEARCH, concGenSearch);
        percentValues.put(STATE_SPACE, stateSpace);
    }

    protected double getErrorTolerancePercent(OptimizationStrategyType opt) {

        return percentValues.get(opt);
        /**
        double percent = 0;
        switch (opt) {
            case GLOBAL_SAMPLING : percent = percentValues[0]; break;
            case GLOBAL_HILL_CLIMBING : percent = percentValues[1]; break;
            case HILL_CLIMBING : percent = percentValues[2]; break;
            case SIMULATED_ANNEALING : percent = percentValues[3]; break;
            case TABU_SEARCH: percent = percentValues[4]; break;
            case GENETIC_SEARCH : percent = percentValues[5]; break;
            case CONCURRENT_GENETIC_SEARCH : percent = percentValues[6]; break;
            case STATE_SPACE: percent = percentValues[7]; break;
        }
        return percent;
         */
    }
}
