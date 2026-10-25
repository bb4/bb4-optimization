// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.optimizees.{
  AckleyProblem, DiscreteGrid2DDemoProblem, LineGraphDemoProblem, OptimizeeProblem,
  RastriginProblem, RosenbrockProblem, SphereProblem
}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import OptimizationStrategyType.*
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.time.{Seconds, Span}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import StrategyScoreboardSuite.*

/**
  * Headless strategy × problem scoreboard.
  *
  * Runs every [[OptimizationStrategyType]] on sphere, Ackley, Rastrigin, Rosenbrock,
  * and two integer lattices (the 1D line and the 2D grid), for seeds 1, 42, and 99.
  * `./gradlew build` (the CI workflow) includes this suite via the default `test` task.
  *
  * Bars are above the worst fitness measured on these seeds, and tight enough that the
  * old differential-evolution no-op (sphere fitness around 4–18) and the old brute-force
  * single-sample grid (fitness 0.57, or a non-terminating 1D sampler) fail them.
  * Every successful run must also finish at least as good as its start.
  */
class StrategyScoreboardSuite extends AnyFunSuite with TimeLimitedTests {

  override val timeLimit: Span = Span(90, Seconds)

  private val Seeds = Seq(1, 42, 99)

  /**
    * Stops a runaway search. Legitimate runs here stay under ~380_000 evaluations; the most expensive is
    * GLOBAL_HILL_CLIMBING on rosenbrock, where each of the 5 seeds (the 5 best, well separated samples, all near the
    * curved valley) crawls along it with ever smaller steps.
    */
  private val EvaluationBudget = 500000

  private val Problems: Seq[(String, () => OptimizeeProblem)] = Seq(
    "sphere" -> (() => SphereProblem(dimensions = 2)),
    "ackley" -> (() => AckleyProblem(dimensions = 2)),
    "rastrigin" -> (() => RastriginProblem(dimensions = 2)),
    "rosenbrock" -> (() => RosenbrockProblem(dimensions = 2)),
    "line" -> (() => new LineGraphDemoProblem),
    "grid" -> (() => new DiscreteGrid2DDemoProblem)
  )

  private val Near = 1.0e-2
  private val Exact = 1.0e-9

  test("every strategy stays inside its fitness bar on every seed") {
    val strategies = OptimizationStrategyType.values.toIndexedSeq
    val rows = ArrayBuffer.empty[Row]
    for (strategy <- strategies; (problemName, factory) <- Problems; seed <- Seeds)
      rows += runOne(strategy, problemName, factory, seed)

    val table = formatTable(rows.toSeq)
    println(table)

    val failures = ArrayBuffer.empty[String]
    rows.foreach(check(_, failures))
    assert(failures.isEmpty, "\n" + table + "\n" + failures.mkString("\n"))
  }

  private def runOne(strategy: OptimizationStrategyType, problemName: String,
                     factory: () => OptimizeeProblem, seed: Int): Row = {
    MathUtil.RANDOM.setSeed(seed.toLong)
    val problem = factory()
    val guess = problem.getInitialGuess
    val initial = problem.evaluateFitness(guess)
    val optimizer = new Optimizer(problem)
    val started = System.nanoTime()
    try {
      val result = optimizer.doOptimization(
        strategy, guess, problem.getFitnessRange, new Random(seed), Some(EvaluationBudget))
      val ms = (System.nanoTime() - started) / 1000000L
      Row(strategy, problemName, seed, result.status.toString, result.best.fitness, initial,
        result.evaluationsUsed.getOrElse(-1), ms, None)
    } catch {
      case e: IllegalArgumentException =>
        val ms = (System.nanoTime() - started) / 1000000L
        Row(strategy, problemName, seed, "threw", Double.NaN, initial, 0, ms, Some(e))
    }
  }

  private def check(row: Row, failures: ArrayBuffer[String]): Unit =
    expectation(row.strategy, row.problem) match {
      case Refuse(snippet) =>
        row.thrown match {
          case Some(e) if Option(e.getMessage).exists(_.contains(snippet)) => ()
          case Some(e) =>
            failures += s"${row.label} threw ${e.getClass.getSimpleName}: ${e.getMessage}; expected a refusal containing \"$snippet\""
          case None =>
            failures += s"${row.label} returned fitness ${fmt(row.fitness)} but this pair must refuse the search (message containing \"$snippet\")"
        }
      case Reach(maxFitness, maxEvaluations) =>
        row.thrown match {
          case Some(e) =>
            failures += s"${row.label} threw ${e.getClass.getSimpleName}: ${e.getMessage}"
          case None =>
            if (!row.fitness.isFinite)
              failures += s"${row.label} fitness is not finite"
            if (row.fitness > row.initial + 1.0e-6)
              failures += s"${row.label} fitness ${fmt(row.fitness)} is worse than the start ${fmt(row.initial)}"
            if (row.fitness > maxFitness)
              failures += s"${row.label} fitness ${fmt(row.fitness)} is above the bar $maxFitness (optimum is 0)"
            if (row.status == OptimizationStatus.BudgetExhausted.toString)
              failures += s"${row.label} exhausted the $EvaluationBudget evaluation budget at fitness ${fmt(row.fitness)}"
            maxEvaluations.foreach { cap =>
              if (row.evals > cap)
                failures += s"${row.label} used ${row.evals} evaluations; cap for this pair is $cap"
            }
        }
    }

  /** What this pair is required to do on every seed. */
  private def expectation(strategy: OptimizationStrategyType, problem: String): Expectation = {
    val continuous = problem != "line" && problem != "grid"
    (strategy, problem) match {
      case (STATE_SPACE_SEARCH, _) if continuous => Refuse("DiscreteStateSpace")
      case (BRUTE_FORCE, _) if continuous => Refuse("continuous")
      case (STATE_SPACE_SEARCH | BRUTE_FORCE, _) => Reach(Exact, Some(500))

      case (DIFFERENTIAL_EVOLUTION | PARTICLE_SWARM | GLOBAL_HILL_CLIMBING, _) =>
        Reach(Near)

      case (GLOBAL_SAMPLING, "rosenbrock") => Reach(1.0)
      case (GLOBAL_SAMPLING, _) => Reach(Near)

      case (GLOBAL_SIMULATED_ANNEALING, "rosenbrock") => Reach(0.1)
      case (GLOBAL_SIMULATED_ANNEALING, _) => Reach(Near)

      case (HILL_CLIMBING, "ackley") => Reach(20.5)
      case (HILL_CLIMBING, "rastrigin") => Reach(40.0)
      case (HILL_CLIMBING, _) => Reach(Near)

      case (SIMULATED_ANNEALING, "rastrigin" | "rosenbrock") => Reach(8.0)
      case (SIMULATED_ANNEALING, _) => Reach(Near)

      case (GENETIC_SEARCH | CONCURRENT_GENETIC_SEARCH, "sphere") => Reach(1.0e-3)
      case (GENETIC_SEARCH | CONCURRENT_GENETIC_SEARCH, "rosenbrock") => Reach(0.02)
      case (GENETIC_SEARCH | CONCURRENT_GENETIC_SEARCH, "rastrigin") => Reach(0.05)
      case (GENETIC_SEARCH | CONCURRENT_GENETIC_SEARCH, "ackley") => Reach(2.0)
      case (GENETIC_SEARCH | CONCURRENT_GENETIC_SEARCH, _) => Reach(Exact)

      case (ITERATED_LOCAL_SEARCH, "line" | "grid") => Reach(Exact)
      case (ITERATED_LOCAL_SEARCH, "sphere") => Reach(0.02)
      case (ITERATED_LOCAL_SEARCH, "rosenbrock") => Reach(0.2)
      case (ITERATED_LOCAL_SEARCH, "ackley") => Reach(4.0)
      case (ITERATED_LOCAL_SEARCH, "rastrigin") => Reach(3.0)

      case (VARIABLE_NEIGHBORHOOD_SEARCH, "line" | "grid") => Reach(Exact)
      case (VARIABLE_NEIGHBORHOOD_SEARCH, "sphere") => Reach(0.02)
      case (VARIABLE_NEIGHBORHOOD_SEARCH, "rosenbrock") => Reach(0.2)
      case (VARIABLE_NEIGHBORHOOD_SEARCH, "ackley") => Reach(3.0)
      case (VARIABLE_NEIGHBORHOOD_SEARCH, "rastrigin") => Reach(2.0)

      case (TABU_SEARCH, _) if continuous => Refuse("discrete")
      case (MIN_CONFLICTS, _) if continuous => Refuse("local moves")
      case (TABU_SEARCH | MIN_CONFLICTS, _) => Reach(Exact, Some(500))

      case _ =>
        throw new IllegalStateException(s"No scoreboard bar for $strategy on $problem")
    }
  }

  private def formatTable(rows: Seq[Row]): String = {
    val header = "strategy\tproblem\tseed\tstatus\tfitness\tinitial\tevals\tms"
    val body = rows.map { r =>
      s"${r.strategy}\t${r.problem}\t${r.seed}\t${r.status}\t${fmt(r.fitness)}\t${fmt(r.initial)}\t${r.evals}\t${r.ms}"
    }
    (header +: body).mkString("\n")
  }

  private def fmt(v: Double): String =
    if (!v.isFinite) "" else if (v == 0.0) "0" else f"$v%.6g"

  private case class Row(
      strategy: OptimizationStrategyType,
      problem: String,
      seed: Int,
      status: String,
      fitness: Double,
      initial: Double,
      evals: Int,
      ms: Long,
      thrown: Option[IllegalArgumentException]
  ) {
    def label: String = s"$strategy on $problem seed $seed"
  }
}

object StrategyScoreboardSuite {
  private[optimization] sealed trait Expectation
  private[optimization] final case class Reach(maxFitness: Double, maxEvaluations: Option[Int] = None) extends Expectation
  private[optimization] final case class Refuse(messageSnippet: String) extends Expectation
}
