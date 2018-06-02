package com.barrybecker4.optimization.viewer1;

import com.barrybecker4.optimization.OptimizationListener1;
import com.barrybecker4.optimization.optimizee1.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.strategy1.OptimizationStrategyType;

/**
 * Classes that can show a visualization of an optimization should implement this interface.
 *
 * @author Barry Becker
 */
public interface OptimizationViewable extends OptimizationListener1, NavigationListener {

    void showOptimization(OptimizationStrategyType strategy, OptimizeeProblem testProblem, String logFile);
}
