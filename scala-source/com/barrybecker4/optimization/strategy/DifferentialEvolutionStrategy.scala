package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.util.Random

/**
 * Differential Evolution (DE/rand/1/bin) Strategy.
 *
 * Excellent default for continuous, bounded, non-linear parameter optimization.
 * Employs population vector differences for mutation, followed by binomial crossover
 * and one-to-one selection.
 *
 * @param optimizee the optimization target problem
 * @param populationSize number of individuals in the population (NP)
 * @param fWeight differential amplification weight, typically in [0.4, 0.9]
 * @param crossoverRate probability of crossover per parameter (CR), typically in [0.5, 0.9]
 */
class DifferentialEvolutionStrategy(
                optimizee: Optimizee,
                populationSize: Int = 40,
                fWeight: Double = 0.8,
                crossoverRate: Double = 0.9,
                rnd: Random = new Random(1)
              ) extends OptimizationStrategy(optimizee) {

  private var population: IndexedSeq[ParameterArray] = _
  private var fitnesses: Array[Double] = _
  private var bestSolution: ParameterArray = _
  private var bestFitness: Double = Double.MaxValue

  /**
   * Runs the Differential Evolution search until convergence or iteration limits.
   */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    initializePopulation(params)

    var iteration = 0
    val maxIterations = getMaxIterations

    var currentParams =
      if (optimizee.evaluateByComparison) ParameterArrayWithFitness(params, Double.MaxValue)
      else ParameterArrayWithFitness(params, optimizee.evaluateFitness(params))

    while (iteration < maxIterations && !isOptimalFitnessReached(currentParams)) {
      val nextPopulation = new Array[ParameterArray](populationSize)
      val nextFitnesses = new Array[Double](populationSize)

      // Parallelize evaluation of trial vectors if needed; sequentially iterated here
      for (i <- 0 until populationSize) {
        val target = population(i)
        val trial = createTrialVector(i, target)
        val trialFitness = optimizee.evaluateFitness(trial)

        // Greedy selection: accept trial if it improves or matches target fitness
        if (trialFitness <= fitnesses(i)) {
          nextPopulation(i) = trial
          nextFitnesses(i) = trialFitness
          if (trialFitness < bestFitness) {
            bestFitness = trialFitness
            bestSolution = trial
          }
        } else {
          nextPopulation(i) = target
          nextFitnesses(i) = fitnesses(i)
        }
      }

      population = nextPopulation.toIndexedSeq
      fitnesses = nextFitnesses

      iteration += 1
      currentParams = ParameterArrayWithFitness(bestSolution, bestFitness)
      notifyListener(currentParams, iteration)
    }

    currentParams
  }

  /**
   * Seeds the population: individual 0 receives initialGuess, while the remaining
   * individuals are sampled uniformly across the parameter bounding spaces.
   */
  private def initializePopulation(initialGuess: ParameterArray): Unit = {
    val pop = new Array[ParameterArray](populationSize)
    fitnesses = new Array[Double](populationSize)

    pop(0) = initialGuess
    fitnesses(0) = optimizee.evaluateFitness(pop(0))
    bestSolution = pop(0)
    bestFitness = fitnesses(0)

    for (i <- 1 until populationSize) {
      // Use ParameterArray sampling hook to randomize within domain bounds
      val individual = initialGuess.getRandomNeighbor(1.0)
      pop(i) = individual
      fitnesses(i) = optimizee.evaluateFitness(individual)

      if (fitnesses(i) < bestFitness) {
        bestFitness = fitnesses(i)
        bestSolution = individual
      }
    }

    population = pop.toIndexedSeq
  }

  /**
   * Generates a mutant vector using DE/rand/1 strategy:
   * v = r1 + F * (r2 - r3), followed by binomial crossover with target vector.
   */
  private def createTrialVector(targetIndex: Int, target: ParameterArray): ParameterArray = {
    // Select three distinct random candidates different from targetIndex
    val indices = selectThreeDistinct(targetIndex)
    val r1 = population(indices(0))
    val r2 = population(indices(1))
    val r3 = population(indices(2))

    val numDimensions = target.size
    val trial = target
    val forcedIndex = rnd.nextInt(numDimensions)

    for (j <- 0 until numDimensions) {
      if (rnd.nextDouble() < crossoverRate || j == forcedIndex) {
        val p1 = r1.get(j)
        val p2 = r2.get(j)
        val p3 = r3.get(j)
        val targetParam = target.get(j)

        // Mutated value: x_r1 + F * (x_r2 - x_r3)
        val mutatedVal = p1.getValue + fWeight * (p2.getValue - p3.getValue)

        // Clamp to parameter domain boundaries
        val clampedVal = math.max(targetParam.minValue, math.min(targetParam.maxValue, mutatedVal))
        trial.get(j).setValue(clampedVal)
      }
    }

    trial
  }

  /**
   * Selects three distinct indices from [0, populationSize) that are not equal to excludedIndex.
   */
  private def selectThreeDistinct(excludedIndex: Int): Array[Int] = {
    val selected = new Array[Int](3)
    var count = 0
    while (count < 3) {
      val candidate = rnd.nextInt(populationSize)
      if (candidate != excludedIndex && !selected.take(count).contains(candidate)) {
        selected(count) = candidate
        count += 1
      }
    }
    selected
  }

  private def getMaxIterations: Int = 1000

  private def notifyListener(solution: ParameterArrayWithFitness, iteration: Int): Unit = {
    if (listener.nonEmpty) {
      listener.get.optimizerChanged(solution)
    }
  }
}