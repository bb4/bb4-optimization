// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.strategy.*

/** Some common tolerances to use in tests */
object ErrorTolerances {
  val BASE_TOLERANCE = 0.0004
  val RELAXED_TOL = 0.0032
  /** Really relax this one because we do not expect it to ever get that close */
  val GLOB_SAMP_TOL = 0.03
}

/**
  * Collects together expected error tolerances for all the optimization strategies.
  * The error rate is 100 * fitness / fitnessRage.
  * @author Barry Becker
  */
case class ErrorTolerances(globalSampling: Double, globalHillClimbing: Double, hillClimbing: Double,
             simAnnealing: Double, geneticSearch: Double, concurrentGeneticSearch: Double,
             stateSpace: Double = 0, bruteForce: Double = 0) {

  private val percentValues = Map[OptimizationStrategyType[? < ParameterArray], Double](
    GLOBAL_SAMPLING -> globalSampling,
    GLOBAL_HILL_CLIMBING -> globalHillClimbing,
    HILL_CLIMBING -> hillClimbing,
    SIMULATED_ANNEALING -> simAnnealing,
    GENETIC_SEARCH -> geneticSearch,
    CONCURRENT_GENETIC_SEARCH -> concurrentGeneticSearch,
    // STATE_SPACE -> stateSpace,
    // the error for brute force should always be 0.
    BRUTE_FORCE-> bruteForce
  )


  def getErrorTolerancePercent(opt: OptimizationStrategyType[? < ParameterArray]): Double = percentValues(opt)
}
