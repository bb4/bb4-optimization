package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.parameter.types.Parameter

import scala.collection.mutable.ArrayBuffer
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
  require(fWeight.isFinite, s"fWeight must be finite, got $fWeight")
  require(crossoverRate >= 0.0 && crossoverRate <= 1.0, s"crossoverRate must be in [0, 1], got $crossoverRate")

  /**
   * Runs the Differential Evolution search until convergence or iteration limits.
   * If the evaluation budget runs out, the best individual evaluated so far is returned.
   */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    require(!optimizee.evaluateByComparison,
      "DifferentialEvolutionStrategy needs an absolute fitness; optimizees with evaluateByComparison == true are not supported")
    val template = params match {
      case numeric: NumericParameterArray => numeric
      case other => throw new IllegalArgumentException(
        "DifferentialEvolutionStrategy only supports NumericParameterArray, but got " + other.getClass.getSimpleName)
    }
    require(template.size > 0, "DifferentialEvolutionStrategy needs at least one parameter to optimize")
    new Evolution(template).execute().getOrElse(ParameterArrayWithFitness(params, Double.MaxValue))
  }

  /** One run. All the state lives here, so a strategy can be run again without anything left over from before. */
  private final class Evolution(template: NumericParameterArray) {

    private var population = IndexedSeq.empty[NumericParameterArray]
    private var fitnesses = Array.empty[Double]
    private var best: Option[ParameterArrayWithFitness] = None

    /** @return the best individual evaluated, or None if not even one could be evaluated */
    def execute(): Option[ParameterArrayWithFitness] = {
      try {
        seedPopulation()
        notifyOfChange(best.get)
        var iteration = 0
        while (iteration < maxIterations && !isOptimalFitnessReached(best.get)) {
          runGeneration()
          iteration += 1
          notifyOfChange(best.get)
        }
      } catch {
        case _: EvaluationBudgetExceededException => () // fall through and return the best found so far
      }
      best
    }

    /** Individual 0 is the initial guess; the others are sampled uniformly across the parameter bounds. */
    private def seedPopulation(): Unit = {
      val individuals = ArrayBuffer[NumericParameterArray](template)
      val scores = ArrayBuffer[Double]()
      scores += evaluate(template)
      while (individuals.size < populationSize) {
        val individual = uniformSample(template)
        individuals += individual
        scores += evaluate(individual)
      }
      population = individuals.toIndexedSeq
      fitnesses = scores.toArray
    }

    /** Builds one trial per individual from the old generation; each replaces its target if it is no worse. */
    private def runGeneration(): Unit = {
      val nextPopulation = new Array[NumericParameterArray](populationSize)
      val nextFitnesses = new Array[Double](populationSize)
      for (i <- 0 until populationSize) {
        val trial = createTrialVector(i, population(i))
        val trialFitness = evaluate(trial)
        val trialWins = trialFitness <= fitnesses(i)
        nextPopulation(i) = if (trialWins) trial else population(i)
        nextFitnesses(i) = if (trialWins) trialFitness else fitnesses(i)
      }
      population = nextPopulation.toIndexedSeq
      fitnesses = nextFitnesses
    }

    /** Scores an individual, and remembers it if it is the best seen so far. NaN counts as the worst possible. */
    private def evaluate(individual: NumericParameterArray): Double = {
      val fitness = optimizee.evaluateFitness(individual)
      val score = if (fitness.isNaN) Double.PositiveInfinity else fitness
      if (best.forall(score < _.fitness)) best = Some(ParameterArrayWithFitness(individual, score))
      score
    }

    /** Uniform point inside each parameter's inclusive bounds. Uses this strategy's [[rnd]]. */
    private def uniformSample(template: NumericParameterArray): NumericParameterArray = {
      val sampled = (0 until template.size).map { j =>
        val p = template.get(j)
        p.setValue(if (p.isIntegerOnly) randomInteger(p) else p.minValue + rnd.nextDouble() * (p.maxValue - p.minValue))
      }
      NumericParameterArray(sampled, template.numSteps, template.rnd)
    }

    private def randomInteger(p: Parameter): Double = {
      val lo = p.minValue.round.toInt
      val hi = p.maxValue.round.toInt
      (lo + rnd.nextInt(math.max(1, hi - lo + 1))).toDouble
    }

    /**
     * Generates a mutant vector using DE/rand/1 strategy:
     * v = r1 + F * (r2 - r3), followed by binomial crossover with target vector.
     * Parameters are immutable (`setValue` returns a new instance); the trial is a new array.
     */
    private def createTrialVector(targetIndex: Int, target: NumericParameterArray): NumericParameterArray = {
      val donor1 = drawDonor(targetIndex)
      val donor2 = drawDonor(targetIndex, donor1)
      val donor3 = drawDonor(targetIndex, donor1, donor2)
      val (r1, r2, r3) = (population(donor1), population(donor2), population(donor3))

      val forcedIndex = rnd.nextInt(target.size) // at least one parameter always comes from the mutant
      val trialParams = (0 until target.size).map { j =>
        val targetParam = target.get(j)
        if (rnd.nextDouble() < crossoverRate || j == forcedIndex) mutatedParameter(r1, r2, r3, targetParam, j)
        else targetParam
      }
      NumericParameterArray(trialParams, target.numSteps, target.rnd)
    }

    /** A population index that is none of `taken`. (Fewer than 4 individuals are rejected by the constructor.) */
    private def drawDonor(taken: Int*): Int =
      Iterator.continually(rnd.nextInt(populationSize)).find(candidate => !taken.contains(candidate)).get

    /** The mutant value, kept inside the bounds and rounded for integer parameters (`setValue` would truncate). */
    private def mutatedParameter(r1: ParameterArray, r2: ParameterArray, r3: ParameterArray,
                                 targetParam: Parameter, j: Int): Parameter = {
      val mutatedVal = r1.get(j).getValue + fWeight * (r2.get(j).getValue - r3.get(j).getValue)
      val clampedVal = math.max(targetParam.minValue, math.min(targetParam.maxValue, mutatedVal))
      targetParam.setValue(if (targetParam.isIntegerOnly) math.rint(clampedVal) else clampedVal)
    }
  }
}
