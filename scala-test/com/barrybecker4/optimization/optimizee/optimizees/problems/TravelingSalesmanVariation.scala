// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems

import com.barrybecker4.optimization.optimizee.optimizees.{ErrorTolerances, ProblemVariation}
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness, PermutedParameterArray}
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import ErrorTolerances._
import TravelingSalesmanVariation.RANDOM

import scala.util.Random


object TravelingSalesmanVariation {
  val RANDOM = new Random(1)
  val VALUES = IndexedSeq(TSP_SIMPLE) //, TSP_STANDARD, TSP_US_CAPITALS)
}

sealed trait TravelingSalesmanVariation  extends ProblemVariation {

  /** @return the number of cities to visit */
  def getNumCities: Int

  /** Some random initial permutation of the cities */
  def getInitialGuess: ParameterArray = {
    val num = this.getNumCities
    val params = for (i <- 0 until num) yield new IntegerParameter(i, 0, num - 1, "p" + i)
    val guess = PermutedParameterArray(params, RANDOM)
    //guess.setFitness(evaluateFitness(guess))
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
  protected def computeCostFromAdjMatrix(params: ParameterArray, matrix: Array[Array[Double]]): Double = {
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

  /** We assume that the parameter array contains 0 based integers
    * @param params parameter array
    * @param coords coordinate locations of the cities
    * @return the total cost of the path represented by param.
    */
  protected def computeCostFromCoords(params: ParameterArray, coords: Array[Array[Double]]): Double = {
    var totalCost: Double = 0
    var lastLocation = params.get(0)
    for (i <- 1 until params.size) {
      val currentLocation = params.get(i)
      totalCost += dist(coords(lastLocation.getValue.toInt), coords(currentLocation.getValue.toInt))
      lastLocation = currentLocation
    }
    // and back home again
    totalCost += dist(coords(lastLocation.getValue.toInt), coords(params.get(0).getValue.toInt))
    totalCost
  }

  private def dist(p1: Array[Double], p2: Array[Double]): Double = {
    val dx = p1(0) - p2(0)
    val dy = p1(1) - p2(1)
    Math.sqrt(dx * dx + dy * dy)
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
    PermutedParameterArray(params, RANDOM)
  }
}


/** Trivial example.
  * There are 4 cities, A, B, C, D. Edge costs given by the adjacency cost matrix.
  */
case object TSP_SIMPLE extends TravelingSalesmanVariation {

  private val COST_MATRIX = Array(
    Array(0.0, 3.0, 2.0, 1.0),
    Array(3.0, 0.0, 1.0, 2.0),
    Array(2.0, 1.0, 0.0, 3.0),
    Array(1.0, 2.0, 3.0, 0.0)
  )

  val errorTolerances =
    ErrorTolerances(GLOB_SAMP_TOL, BASE_TOLERANCE, BASE_TOLERANCE, 0.04, 0.042, 0.042, BASE_TOLERANCE)

  override def getNumCities = 4
  override def getShortestPathLength = 6.0

  override def getExactSolution: ParameterArrayWithFitness = {
    val solution = createSolution(Array[Int](0, 2, 1, 3))
    ParameterArrayWithFitness(solution, 0)
  }

  override def getFitnessRange = 9.0

  override def evaluateFitness(paramArray: ParameterArray): Double = {
    val c = computeCostFromAdjMatrix(paramArray, COST_MATRIX) - getShortestPathLength
    //println("cost for " + paramArray + " is " + c)
    c
  }
}


/**
  * This version is a bit more realistic.
  * See http://www.tilburguniversity.edu/research/institutes-and-research-groups/center/staff/haemers/reader10ico.pdf
  * B,  E,  H,  N,  T
  */
case object TSP_STANDARD extends TravelingSalesmanVariation {

  private val COST_MATRIX: Array[Array[Double]] = Array(
    Array(0d, 54d, 48d, 92d, 24d),
    Array(54d, 0d, 32d, 61d, 35d),
    Array(48d, 32d, 0d, 45d, 23d),
    Array(92d, 61d, 45d, 0d, 67d),
    Array(24d, 35d, 23d, 67d, 0d)
  )

  val errorTolerances = ErrorTolerances(GLOB_SAMP_TOL, RELAXED_TOL, 0.01, 0.04, 0.042, 0.042, BASE_TOLERANCE)

  override def getNumCities = 5
  override def getShortestPathLength = 207.0

  override def getExactSolution: ParameterArrayWithFitness = {
    val solution = createSolution(Array[Int](2, 4, 0, 1, 3))
    ParameterArrayWithFitness(solution, 0)
  }

  override def getFitnessRange = 1000.0

  override def evaluateFitness(a: ParameterArray): Double =
    computeCostFromAdjMatrix(a, COST_MATRIX) - getShortestPathLength
}

/** This is a pretty hard case.
  * Find shortest path that visits all US capitals in contiguous US and returns to start.
  * For dataset see http://elib.zib.de/pub/mp-testdata/tsp/tsplib/tsp/att48.tsp
  * For solution, see
  * http://support.sas.com/documentation/cdl/en/ornoaug/67520/HTML/default/viewer.htm#ornoaug_optnet_examples07.htm#ornoaug.optnet.ex_tsp2_out
  * The shortest path is 10,627.75 miles.
  * Alpha list of capitals: https://people.sc.fsu.edu/~jburkardt/datasets/states/state_capitals_xy.txt
  */
case object TSP_US_CAPITALS extends TravelingSalesmanVariation {

  private val CAP_COORDS: Array[Array[Double]] = Array(
    Array(6734d, 1453d),
    Array(2233d, 10d),
    Array(5530d, 1424d),
    Array(401d, 841d),
    Array(3082d, 1644d),
    Array(7608d, 4458d),
    Array(7573d, 3716d),
    Array(7265d, 1268d),
    Array(6898d, 1885d),
    Array(1112d, 2049d),
    Array(5468d, 2606d),
    Array(5989d, 2873d),
    Array(4706d, 2674d),
    Array(4612d, 2035d),
    Array(6347d, 2683d),
    Array(6107d, 669d),
    Array(7611d, 5184d),
    Array(7462d, 3590d),
    Array(7732d, 4723d),
    Array(5900d, 3561d),
    Array(4483d, 3369d),
    Array(6101d, 1110d),
    Array(5199d, 2182d),
    Array(1633d, 2809d),
    Array(4307d, 2322d),
    Array(675d, 1006d),
    Array(7555d, 4819d),
    Array(7541d, 3981d),
    Array(3177d, 756d),
    Array(7352d, 4506d),
    Array(7545d, 2801d),
    Array(3245d, 3305d),
    Array(6426d, 3173d),
    Array(4608d, 1198d),
    Array(23d, 2216d),
    Array(7248d, 3779d),
    Array(7762d, 4595d),
    Array(7392d, 2244d),
    Array(3484d, 2829d),
    Array(6271d, 2135d),
    Array(4985d, 140d),
    Array(1916d, 1569d),
    Array(7280d, 4899d),
    Array(7509d, 3239d),
    Array(10d, 2676d),
    Array(6807d, 2993d),
    Array(5185d, 3258d),
    Array(3023d, 1942d)
  )

  val errorTolerances = ErrorTolerances(GLOB_SAMP_TOL, RELAXED_TOL, 0.01, 0.04, 0.042, 0.042, BASE_TOLERANCE)

  override def getNumCities = 48
  override def getShortestPathLength = 10627.75

  override def getExactSolution: ParameterArrayWithFitness = {
    val solution = createSolution(Array[Int](2, 4)) // not known
    ParameterArrayWithFitness(solution, 0)
  }

  override def getFitnessRange = 120000.0

  override def evaluateFitness(a: ParameterArray): Double =
    computeCostFromCoords(a, CAP_COORDS) - getShortestPathLength
}
