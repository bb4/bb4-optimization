package com.barrybecker4.optimization.optimizee

import com.barrybecker4.math.MathUtil
import com.barrybecker4.common.util.FileUtil
import com.barrybecker4.optimization.Optimizer
import com.barrybecker4.optimization.optimizee.optimizees.{OptimizeeProblem, ProblemVariation}
import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.strategy._
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/**
  * @author Barry Becker
  */
object OptimizerTestSuite {

  /** Where the log files will go */
  val LOG_FILE_HOME: String = FileUtil.getHomeDir + "scala-test/performance/test_optimizer/"

  /** If the error is this percent (or more) less than the error threshold, let the user know */
  private val THRESHOLD_SLACK_WARNING = 0.1

  /** If the error is this percent less than the error threshold, give an error so the test can be updated */
  private val THRESHOLD_SLACK_ERROR = 0.2

  /** If the error is less than this, then it is considered acceptable, even if off by a lot from the thresh */
  private val ACCEPTABLE_ERROR = 0.002

  /** Give an error if not withing errorThresh of the exact solution. */
  def verifyTest(optType: OptimizationStrategyType, problem: OptimizeeProblem,
                           initialGuess: ParameterArray, optimizer: Optimizer, fitnessRange: Double,
                           errorThresh: Double, title: String): Unit = {
    System.out.println(title + "\nabout to apply " + optType + " to " + problem.getName +
      " with initial guess = " + initialGuess + ".")
    val solution = optimizer.doOptimization(optType, initialGuess, fitnessRange, new Random(1))
    val error = problem.getError(solution)
    assert(error <= errorThresh,
      s"*** $title ***\nAllowable error ($errorThresh) was exceeded using $optType" +
      ". \nError = " + error + "\n The Test Solution was " + solution +
      "\n but we expected to get something very close to the exact solution:\n " +
      problem.getExactSolution)

    if (error < (1.0 - THRESHOLD_SLACK_WARNING) * errorThresh && error > ACCEPTABLE_ERROR) {
      val message = s"The error threshold of $errorThresh for $optType running on ${problem.getName} " +
        s" is a bit slack. It could be reduced to $error."
      System.out.println(message)
      assert(error >= (1.0 - THRESHOLD_SLACK_ERROR) * errorThresh, message)
    }
    println("\n************************************************************************")
    println("The solution to the Problem using " + optType + " is :\n" + solution + "\nWhich evaluates to: " +
      solution.fitness + " with error = " + error)
  }
}

abstract class OptimizerTestSuite extends AnyFunSuite with BeforeAndAfter {
  before {
    MathUtil.RANDOM.setSeed(0)
  }

  test("GlobalSampling") {
    doTest(GLOBAL_SAMPLING)
  }
  test("SimulatedAnnealing") {
    doTest(SIMULATED_ANNEALING)
  }
  test("GeneticSearch") {
    doTest(GENETIC_SEARCH)
  }
  test("ConcurrentGeneticSearch") {
    doTest(CONCURRENT_GENETIC_SEARCH)
  }
  test("HillClimbing") {
    doTest(HILL_CLIMBING)
  }
  test("GlobalHillClimbing") {
    doTest(GLOBAL_HILL_CLIMBING)
  }

  /** Run test for given optimization type
    * @param optType the optimization type to use.
    */
  protected def doTest(optType: OptimizationStrategyType): Unit

  protected def verifyProblem(problem: OptimizeeProblem,
                              variation: ProblemVariation, optType: OptimizationStrategyType): Unit = {
    val logFile = OptimizerTestSuite.LOG_FILE_HOME + "analytic_" + variation + "_optimization.txt"
    val optimizer = new Optimizer(problem, Some(logFile))
    val initialGuess = problem.getInitialGuess
    val percent = variation.getErrorTolerancePercent(optType)
    OptimizerTestSuite.verifyTest(optType, problem, initialGuess, optimizer, problem.getFitnessRange,
      percent, variation.toString)
  }
}