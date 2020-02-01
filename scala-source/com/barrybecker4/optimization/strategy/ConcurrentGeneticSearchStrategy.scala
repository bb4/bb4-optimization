// Copyright by Barry G. Becker, 2001-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.collection.parallel.CollectionConverters._

/**
  * Concurrent (i.e. parallelized) Genetic Algorithm (evolutionary) optimization strategy.
  * Many different strategies are possible to alter the population for each successive iteration.
  * The 2 primary ones that I use here are unary mutation and cross-over.
  * See Chapter 6 in "How to Solve it: Modern Heuristics" for more info.
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class ConcurrentGeneticSearchStrategy(optimizee: Optimizee, rnd: Random = MathUtil.RANDOM)
  extends GeneticSearchStrategy(optimizee, rnd) {

  /**
    * Note: this method assigns a fitness value to each member of the population.
    *
    * Evaluate the members of the population - either directly, or by
    * comparing them against the initial params value passed in (including params).
    *
    * Create a thread for each evaluation and don't continue until they are all done (countDown latch or gate)
    *
    * @param population   the population to evaluate
    * @param previousBest the best solution from the previous iteration
    * @return the new best solution.
    */
  override protected def evaluatePopulation(population: ArrayBuffer[ParameterArray],
                                            previousBest: ParameterArray): ParameterArrayWithFitness = {
    var bestFitness = ParameterArrayWithFitness(previousBest, Double.MaxValue)
    val workers = population.map(candidate => new EvaluationWorker(candidate, previousBest))

    workers.par.foreach(x => x.run()) // run workers in parallel

    for (worker <- workers) {
      val eworker = worker.asInstanceOf[EvaluationWorker]
      if (eworker.getResult < bestFitness.fitness)
        bestFitness = eworker.getCandidateWithFitness
    }
    bestFitness
  }

  /** Does the evaluation for each candidate in a different thread. */
  private class EvaluationWorker(var candidate: ParameterArray, var params: ParameterArray) extends Runnable {
    private var fitness = .0

    override def run(): Unit = {
      if (optimizee.evaluateByComparison) fitness = optimizee.compareFitness(candidate, params)
      else fitness = optimizee.evaluateFitness(candidate)
    }

    private[strategy] def getResult = fitness
    private[strategy] def getCandidateWithFitness = ParameterArrayWithFitness(candidate, fitness)
  }
}
