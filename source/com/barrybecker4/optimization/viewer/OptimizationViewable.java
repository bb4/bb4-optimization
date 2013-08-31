package com.barrybecker4.optimization.viewer;

import com.barrybecker4.optimization.OptimizationListener;
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

/**
 * Classes that can show a visualization of an optimization should implement this interface.
 *
 * @author Barry Becker
 */
public interface OptimizationViewable extends OptimizationListener, NavigationListener {

    void showOptimization(OptimizationStrategyType strategy, OptimizeeProblem testProblem, String logFile);
}
