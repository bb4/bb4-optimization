// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{
  NumericParameterArray, ParameterArray, ParameterArrayWithFitness, PermutedParameterArray, VariableLengthIntSet
}

import scala.util.Random


object GlobalHillClimbingStrategy {
  /** Passed to [[GlobalSampleStrategy.setSamplingRate]] as the global numeric sample budget (see NumericGlobalSampler). */
  private val NUM_SAMPLES = 1000

  /** Number of best global samples from which to run local hill climbing. */
  val DEFAULT_MULTISTART_COUNT: Int = 5
}

/**
  * Hybrid strategy: global exploration (grid-style sampling via [[GlobalSampleStrategy]]), then local
  * [[HillClimbingStrategy]] from each of the best `multistartCount` samples; returns the best local optimum.
  *
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class GlobalHillClimbingStrategy(optimizee: Optimizee) extends OptimizationStrategy(optimizee) {

  private var multistartCount: Int = GlobalHillClimbingStrategy.DEFAULT_MULTISTART_COUNT

  /** How many top global samples seed local hill climbing (clamped to at least 1 at runtime). */
  def setMultistartCount(k: Int): Unit = {
    assert(k >= 1, "multistart count must be positive")
    multistartCount = k
  }

  /**
    * Perform the optimization of the optimizee.
    * @param params parameter array
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return optimized params
    */
  override def doOptimization(params: ParameterArray,
                              fitnessRange: Double): ParameterArrayWithFitness = {
    val gsStrategy = new GlobalSampleStrategy(optimizee)
    gsStrategy.setListener(listener)
    gsStrategy.setSamplingRate(GlobalHillClimbingStrategy.NUM_SAMPLES)
    val seeds = gsStrategy.doOptimizationTopK(params, fitnessRange, multistartCount)
    if (seeds.isEmpty) {
      val initial =
        if (optimizee.evaluateByComparison) ParameterArrayWithFitness(params, Double.MaxValue)
        else ParameterArrayWithFitness(params, optimizee.evaluateFitness(params))
      runHillClimbingFrom(initial, fitnessRange)
    } else {
      val results = seeds.zipWithIndex.map { case (seed, idx) =>
        val start = if (idx == 0) seed else parameterArrayWithForkedRnd(seed, idx)
        runHillClimbingFrom(start, fitnessRange)
      }
      results.minBy(_.fitness)
    }
  }

  /**
    * Extra multistart seeds often share the same [[Random]] as the initial guess; hill climbing mutates that RNG.
    * Fork per seed (after the primary) so the first local run matches legacy single-start behavior and later starts
    * do not depend on how many draws earlier seeds consumed.
    */
  private def parameterArrayWithForkedRnd(s: ParameterArrayWithFitness, forkIdx: Int): ParameterArrayWithFitness = {
    val salt = s.pa.## * 31L + forkIdx.toLong * 1103515245L
    val rnd = new Random(salt)
    val newPa = s.pa match {
      case v: VariableLengthIntSet =>
        val paramsSeq = (0 until v.size).map(v.get).toIndexedSeq
        VariableLengthIntSet.createInstance(paramsSeq, v.fullSeq, rnd)
      case n: NumericParameterArray =>
        NumericParameterArray(n.params, n.numSteps, rnd)
      case p: PermutedParameterArray =>
        PermutedParameterArray(p.params, rnd)
      case other => other
    }
    ParameterArrayWithFitness(newPa, s.fitness)
  }

  private def runHillClimbingFrom(seed: ParameterArrayWithFitness, fitnessRange: Double): ParameterArrayWithFitness = {
    val strategy = new HillClimbingStrategy(optimizee)
    strategy.setListener(listener)
    strategy.doOptimization(seed.pa, fitnessRange)
  }
}
