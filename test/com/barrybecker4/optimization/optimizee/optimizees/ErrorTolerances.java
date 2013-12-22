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
        // the error for brute force should always be 0.
        percentValues.put(BRUTE_FORCE, 0.0);
    }

    protected double getErrorTolerancePercent(OptimizationStrategyType opt) {

        return percentValues.get(opt);
    }
}
