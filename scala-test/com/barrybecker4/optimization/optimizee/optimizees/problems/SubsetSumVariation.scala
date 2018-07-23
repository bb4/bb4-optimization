// Copyright by Barry G. Becker, 2013-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems

import com.barrybecker4.optimization.optimizee.optimizees.{ErrorTolerances, ProblemVariation}
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness, VariableLengthIntSet}
import com.barrybecker4.optimization.parameter.distancecalculators.MagnitudeDistanceCalculator
import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType

import scala.util.Random


object SubsetSumVariation {
  val VALUES = IndexedSeq(SIMPLE_SS, TYPICAL_SS, NO_SOLUTION)
}

sealed trait SubsetSumVariation extends ProblemVariation {

  /** @return the number of nodes in the graph */
  def getNumElements: Int = getNumberSeq.size

  /** The graph containing the node adjacency information */
  protected def getNumberSeq: IndexedSeq[Int]

  /** Some random initial set of marked nodes.
    * One half or one third of the nodes is probably a good starting point.
    */
  def getInitialGuess: ParameterArray = {
    val num = this.getNumElements
    val numSet = this.getNumberSeq
    val params: IndexedSeq[Parameter] = for (i <- 0 until num by 3) yield createParam(numSet(i))
    val pa = new VariableLengthIntSet(
      params, getNumberSeq, new MagnitudeDistanceCalculator, new Random(1))
    pa
  }

  /** Evaluate fitness for the analytics function.
    * @param pa param array
    * @return fitness value
    */
  def evaluateFitness(pa: ParameterArray): Double = {
    computeCost(pa)
  }

  /** Approximate value of maxCost - minCost */
  def getFitnessRange: Double

  /** Assume that the parameter array contains 0 based integers.
    * Penalize the case when there is only 1 value (and it is not 0).
    * @param params last best guess at subset.
    * @return the total cost of the subset represented by param.  In this case the absolute sum of the marked values.
    */
  protected def computeCost(params: ParameterArray): Double = {
    var sum: Double = 0
    for (i <- 0 until params.size) {
      val node = params.get(i)
      sum += node.getValue.toInt
    }
    val absSum = Math.abs(sum)
    if (absSum != 0.0) {
      val maxLen = params.asInstanceOf[VariableLengthIntSet].getMaxLength.toDouble
      (1.0 + (maxLen - params.size) / maxLen) * absSum
    } else absSum
  }

  /** @return the error tolerance percent for a specific optimization strategy */
  override def getErrorTolerancePercent(opt: OptimizationStrategyType): Double =
    errorTolerances.getErrorTolerancePercent(opt)

  /** Error tolerance for each search strategy and variation of the problem.
    * @return error tolerance percent
    */
  protected def errorTolerances: ErrorTolerances

  /** Create the solution based on the ordered list of cities.
    * @param numberList optimal dominating set of marked nodes. May not be unique.
    * @return optimal solution (to compare against at the end of the test).
    */
  protected def createSolution(numberList: Int*): ParameterArrayWithFitness = {
    val numNodes = numberList.length
    assert(numNodes > 0, "There must be some values in a valid solution.")
    val params = for (i <- 0 until numNodes) yield createParam(i)

    ParameterArrayWithFitness(
      new VariableLengthIntSet(params, getNumberSeq, new MagnitudeDistanceCalculator, new Random(1)),
      0)
  }

  private def createParam(i: Int) = {
    val min = if (i < 0) i
    else 0
    val max = if (i >= 0) i
    else 0
    new IntegerParameter(i, min, max, "p" + i)
  }
}


case object SIMPLE_SS extends SubsetSumVariation {
  val errorTolerances =
    ErrorTolerances(0.0, 0.0, 8.0, 6.4, 7.3, 7.3, 0.0)

  override protected def getNumberSeq = Array(-7, -3, -2, 5, 8)

  override def getExactSolution: ParameterArrayWithFitness = createSolution(-3, -2, 5)

  override def getFitnessRange = 22.0
}


case object TYPICAL_SS extends SubsetSumVariation {
  val errorTolerances = ErrorTolerances(0.0, 0.5, 0.5, 1.25, 0.5, 0.5, 0.0)

  override protected def getNumberSeq =
    Array(-7, -33, -21, 5, 83, -29, -78, 213, 123, -34, -37, -41, 91, -8, -17)

  // This is one of several possible solutions that gives an optimal fitness of 0
  override def getExactSolution: ParameterArrayWithFitness =
    createSolution(-33, -21, 5, -29, 123, -37, -8)

  override def getFitnessRange = 400.0
}


case object NO_SOLUTION extends  SubsetSumVariation {
  // none of the errors will be 0 because there is no solution that sums to 0.
  val errorTolerances = ErrorTolerances(1.62, 0.7, 1.3, 0.7, 0.7, 0.7, 0.7, 0.63)

  override protected def getNumberSeq =
    Array(-7, -33, -21, 5, -83, -29, -78, -113, -23, -34, -37, -41, -91, -9, -17)

  /** There is no solution - i.e. no values that sum to 0. */
  override def getExactSolution: ParameterArrayWithFitness = createSolution(-7)

  override def getFitnessRange = 600.0
}