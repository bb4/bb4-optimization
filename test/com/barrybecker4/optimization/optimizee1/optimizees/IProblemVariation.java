package com.barrybecker4.optimization.optimizee1.optimizees;

import com.barrybecker4.optimization.parameter1.ParameterArray;
import com.barrybecker4.optimization.strategy1.OptimizationStrategyType;

/**
 * A variation on an {@code OptimizeeProblem}
 *
 * @author Barry Becker
 */
public interface IProblemVariation {

    /**
     * @return An optimal ordering of the cities to visit such that cost is minimized.
     */
    ParameterArray getExactSolution();


    /**
     * @param opt the strategy type to get the expected error tolerance for.
     * @return the error tolerance percent for a specific optimization strategy
     */
    double getErrorTolerancePercent(OptimizationStrategyType opt);
}
