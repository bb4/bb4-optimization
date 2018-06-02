package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.strategy.{GLOBAL_SAMPLING, OptimizationStrategyType}



/**
  * Collects together expected error tolerances for all the optimization strategies.
  * The error rate is 100 * fitness / fitnessRage.
  * @author Barry Becker
  */
case class ErrorTolerances(globalSampling: Double, globalHillClimbing: Double, hillClimbing: Double,
             simAnnealing: Double, tabuSearch: Double,
             geneticSearch: Double, concurrentGeneticSearch: Double,
             stateSpace: Double = 0, bruteForce: Double = 0) {

  private val percentValues = Map[OptimizationStrategyType, Double](
    GLOBAL_SAMPLING -> globalSampling,
    //GLOBAL_HILL_CLIMBING -> globalHillClimbing,
    //HILL_CLIMBING -> hillClimbing,
    //SIMULATED_ANNEALING -> simAnnealing,
    //TABU_SEARCH -> tabuSearch,
    //GENETIC_SEARCH -> geneticSearch,
    //CONCURRENT_GENETIC_SEARCH -> concurrentGeneticSearch,
    // STATE_SPACE -> stateSpace,
    // the error for brute force should always be 0.
    //BRUTE_FORCE-> bruteForce
  )


  def getErrorTolerancePercent(opt: OptimizationStrategyType): Double = percentValues(opt)
}
