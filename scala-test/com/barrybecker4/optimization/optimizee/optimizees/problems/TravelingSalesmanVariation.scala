// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems

import com.barrybecker4.optimization.optimizee.optimizees.{ErrorTolerances, ProblemVariation}
import com.barrybecker4.optimization.parameter.{ParameterArray, PermutedParameterArray}
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import ErrorTolerances._


object TravelingSalesmanVariation {
  val VALUES = IndexedSeq(SIMPLE, STANDARD)
}

sealed trait TravelingSalesmanVariation  extends ProblemVariation {

  /** @return the number of cities to visit */
  def getNumCities: Int

  /** Some random initial permutation of the cities */
  def getInitialGuess: ParameterArray = {
    val num = this.getNumCities
    val params = for (i <- 0 until num) yield new IntegerParameter(i, 0, num - 1, "p" + i)
    val guess = new PermutedParameterArray(params.toArray)
    guess.setFitness(10000000)
    guess
  }

  /** Approximate value of maxCost - minCost */
  def getFitnessRange: Double
  def getShortestPathLength: Double

  /** Evaluate fitness for the analytics function.
    * @param a the position on the parabolic surface given the specified values of p1 and p2
    * @return fitness value
    */
  def evaluateFitness(a: ParameterArray): Double

  /** We assume that the parameter array contains 0 based integers
    * @param params parameter array
    * @param matrix adjacency matrix
    * @return the total cost of the path represented by param.
    */
  protected def computeCost(params: ParameterArray, matrix: Array[Array[Double]]): Double = {
    var totalCost: Double = 0
    var lastLocation = params.get(0)
    for (i <- 1 until params.size) {
      val currentLocation = params.get(i)
      totalCost += matrix(lastLocation.getValue.toInt)(currentLocation.getValue.toInt)
      lastLocation = currentLocation
    }
    // and back home again
    totalCost += matrix(lastLocation.getValue.toInt)(params.get(0).getValue.toInt)
    totalCost
  }

  /** @return the error tolerance percent for a specific optimization strategy */
  override def getErrorTolerancePercent(opt: OptimizationStrategyType): Double =
    errorTolerances.getErrorTolerancePercent(opt)

  /** Error tolerance for each search strategy and variation of the problem.
    * @return error tolerance percent
    */
  protected def errorTolerances: ErrorTolerances

  /** Create the solution based on the ordered list of cities.
    * @param cityList optimal ordering of city indices.
    * @return optimal solution (to compare against at the end of the test).
    */
  protected def createSolution(cityList: Array[Int]): PermutedParameterArray = {
    val numCities = cityList.length
    val params = for (i <- cityList.indices) yield
      new IntegerParameter(cityList(i), 0, numCities - 1, "p" + i)
    new PermutedParameterArray(params.toArray)
  }
}


/** Trivial example.
  * There are 4 cities, A, B, C, C. This is the adjacency cost matrix.
  */
case object SIMPLE extends TravelingSalesmanVariation {

  private val COST_MATRIX = Array(
    Array(0.0, 3.0, 2.0, 1.0),
    Array(3.0, 0.0, 1.0, 2.0),
    Array(2.0, 1.0, 0.0, 3.0),
    Array(1.0, 2.0, 3.0, 0.0)
  )

  val errorTolerances =
    ErrorTolerances(GLOB_SAMP_TOL, BASE_TOLERANCE, BASE_TOLERANCE, 0.04, RELAXED_TOL, 0.042, 0.042, BASE_TOLERANCE)

  override def getNumCities = 4
  override def getShortestPathLength = 6.0

  override def getExactSolution: PermutedParameterArray = {
    val solution = createSolution(Array[Int](0, 2, 1, 3))
    solution.setFitness(0)
    solution
  }

  override def getFitnessRange = 9.0

  override def evaluateFitness(paramArray: ParameterArray): Double = {
    val c = computeCost(paramArray, COST_MATRIX) - getShortestPathLength
    println("cost for " + paramArray + " is " + c)
    c
  }
}


/**
  * This version is a bit more realistic.
  * See http://www.tilburguniversity.edu/research/institutes-and-research-groups/center/staff/haemers/reader10ico.pdf
  * B,  E,  H,  N,  T
  */
case object STANDARD extends TravelingSalesmanVariation {

  private val COST_MATRIX: Array[Array[Double]] = Array(
    Array(0, 54, 48, 92, 24),
    Array(54, 0, 32, 61, 35),
    Array(48, 32, 0, 45, 23),
    Array(92, 61, 45, 0, 67),
    Array(24, 35, 23, 67, 0)
  )

  val errorTolerances = ErrorTolerances(GLOB_SAMP_TOL, RELAXED_TOL, 0.01, 0.04, RELAXED_TOL, 0.042, 0.042, BASE_TOLERANCE)

  override def getNumCities = 5
  override def getShortestPathLength = 207.0

  override def getExactSolution: PermutedParameterArray = {
    val solution = createSolution(Array[Int](2, 4, 0, 1, 3))
    solution.setFitness(0)
    solution
  }

  override def getFitnessRange = 1000.0

  override def evaluateFitness(a: ParameterArray): Double =
    computeCost(a, COST_MATRIX) - getShortestPathLength
}
