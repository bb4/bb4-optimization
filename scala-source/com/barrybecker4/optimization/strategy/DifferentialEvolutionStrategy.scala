package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.parameter.types.Parameter

import scala.util.Random

/**
 * Differential Evolution (DE/rand/1/bin) Strategy.
 *
 * Excellent default for continuous, bounded, non-linear parameter optimization.
 * Employs population vector differences for mutation, followed by binomial crossover
 * and one-to-one selection.
 *
 * @param optimizee the optimization target problem
 * @param populationSize number of individuals in the population (NP). Must be at least 4.
 * @param fWeight differential amplification weight, typically in [0.4, 0.9]
 * @param crossoverRate probability of crossover per parameter (CR), typically in [0.5, 0.9]
 * @param rnd random source for crossover and for the initial uniform population
 * @param maxIterations generation limit
 */
class DifferentialEvolutionStrategy(
                optimizee: Optimizee,
                populationSize: Int = 40,
                fWeight: Double = 0.8,
                crossoverRate: Double = 0.9,
                rnd: Random = new Random(1),
                maxIterations: Int = 1000
              ) extends OptimizationStrategy(optimizee) {

  require(populationSize >= 4,
    s"DifferentialEvolutionStrategy needs at least 4 individuals to pick three distinct donors, got $populationSize")
  require(maxIterations >= 0, s"maxIterations must not be negative, got $maxIterations")

  private var population: IndexedSeq[ParameterArray] = IndexedSeq.empty
  private var fitnesses: Array[Double] = Array.empty
  private var bestSolution: ParameterArray = null
  private var bestFitness: Double = Double.MaxValue

  /**
   * Runs the Differential Evolution search until convergence or iteration limits.
   */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    require(!optimizee.evaluateByComparison,
      "DifferentialEvolutionStrategy needs an absolute fitness; optimizees with evaluateByComparison == true are not supported")
    val template = params match {
      case numeric: NumericParameterArray => numeric
      case other => throw new IllegalArgumentException(
        "DifferentialEvolutionStrategy only supports NumericParameterArray, but got " + other.getClass.getSimpleName)
    }

    try {
      initializePopulation(template)

      var iteration = 0
      var currentParams = ParameterArrayWithFitness(bestSolution, bestFitness)
      notifyOfChange(currentParams)

      while (iteration < maxIterations && !isOptimalFitnessReached(currentParams)) {
        val nextPopulation = new Array[ParameterArray](populationSize)
        val nextFitnesses = new Array[Double](populationSize)

        var i = 0
        while (i < populationSize) {
          val target = population(i)
          val trial = createTrialVector(i, target)
          val trialFitness = optimizee.evaluateFitness(trial)

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
          i += 1
        }

        population = nextPopulation.toIndexedSeq
        fitnesses = nextFitnesses
        iteration += 1
        currentParams = ParameterArrayWithFitness(bestSolution, bestFitness)
        notifyOfChange(currentParams)
      }
      currentParams
    } catch {
      case _: EvaluationBudgetExceededException =>
        if (bestSolution == null) ParameterArrayWithFitness(params, Double.MaxValue)
        else ParameterArrayWithFitness(bestSolution, bestFitness)
    }
  }

  /**
   * Seeds the population: individual 0 receives initialGuess, while the remaining
   * individuals are sampled uniformly across the parameter bounds.
   */
  private def initializePopulation(initialGuess: NumericParameterArray): Unit = {
    val pop = new Array[ParameterArray](populationSize)
    fitnesses = new Array[Double](populationSize)

    pop(0) = initialGuess
    fitnesses(0) = optimizee.evaluateFitness(pop(0))
    bestSolution = pop(0)
    bestFitness = fitnesses(0)

    var i = 1
    while (i < populationSize) {
      val individual = uniformSample(initialGuess)
      pop(i) = individual
      fitnesses(i) = optimizee.evaluateFitness(individual)
      if (fitnesses(i) < bestFitness) {
        bestFitness = fitnesses(i)
        bestSolution = individual
      }
      i += 1
    }
    population = pop.toIndexedSeq
  }

  /** Uniform point inside each parameter's inclusive bounds. Uses this strategy's [[rnd]]. */
  private def uniformSample(template: NumericParameterArray): NumericParameterArray = {
    val sampled = (0 until template.size).map { j =>
      val p = template.get(j)
      val proposed =
        if (p.isIntegerOnly) {
          val lo = p.minValue.round.toInt
          val hi = p.maxValue.round.toInt
          val span = math.max(1, hi - lo + 1)
          (lo + rnd.nextInt(span)).toDouble
        } else p.minValue + rnd.nextDouble() * (p.maxValue - p.minValue)
      p.setValue(proposed)
    }
    NumericParameterArray(sampled, template.numSteps, template.rnd)
  }

  /**
   * Generates a mutant vector using DE/rand/1 strategy:
   * v = r1 + F * (r2 - r3), followed by binomial crossover with target vector.
   * Parameters are immutable (`setValue` returns a new instance); the trial is a new array.
   */
  private def createTrialVector(targetIndex: Int, target: ParameterArray): ParameterArray = {
    val indices = selectThreeDistinct(targetIndex)
    val r1 = population(indices(0))
    val r2 = population(indices(1))
    val r3 = population(indices(2))

    val numDimensions = target.size
    val forcedIndex = if (numDimensions == 0) 0 else rnd.nextInt(numDimensions)
    val trialParams = (0 until numDimensions).map { j =>
      val targetParam = target.get(j)
      if (rnd.nextDouble() < crossoverRate || j == forcedIndex) mutatedParameter(r1, r2, r3, targetParam, j)
      else targetParam
    }
    rebuild(target, trialParams)
  }

  private def mutatedParameter(r1: ParameterArray, r2: ParameterArray, r3: ParameterArray,
                               targetParam: Parameter, j: Int): Parameter = {
    val mutatedVal = r1.get(j).getValue + fWeight * (r2.get(j).getValue - r3.get(j).getValue)
    val clampedVal = math.max(targetParam.minValue, math.min(targetParam.maxValue, mutatedVal))
    targetParam.setValue(clampedVal)
  }

  private def rebuild(template: ParameterArray, params: IndexedSeq[Parameter]): ParameterArray =
    template match {
      case n: NumericParameterArray => NumericParameterArray(params, n.numSteps, n.rnd)
      case other =>
        throw new IllegalArgumentException(
          "DifferentialEvolutionStrategy only supports NumericParameterArray, but got " + other.getClass.getSimpleName)
    }

  /**
   * Selects three distinct indices from [0, populationSize) that are not equal to excludedIndex.
   */
  private def selectThreeDistinct(excludedIndex: Int): Array[Int] = {
    val selected = new Array[Int](3)
    var count = 0
    var guard = 0
    val guardLimit = populationSize * 20
    while (count < 3) {
      guard += 1
      if (guard > guardLimit)
        throw new IllegalStateException(
          s"Could not pick 3 donors distinct from $excludedIndex in a population of $populationSize")
      val candidate = rnd.nextInt(populationSize)
      if (candidate != excludedIndex && !selected.take(count).contains(candidate)) {
        selected(count) = candidate
        count += 1
      }
    }
    selected
  }
}
