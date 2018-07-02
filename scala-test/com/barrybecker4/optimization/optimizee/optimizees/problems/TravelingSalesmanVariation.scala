// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems

import com.barrybecker4.optimization.optimizee.optimizees.{ErrorTolerances, ProblemVariation}
import com.barrybecker4.optimization.parameter.{ParameterArray, PermutedParameterArray}
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import ErrorTolerances._
import TravelingSalesmanVariation.RANDOM
import scala.util.Random


object TravelingSalesmanVariation {
  val RANDOM = new Random(1)
  val VALUES = IndexedSeq(TSP_SIMPLE, TSP_STANDARD)//, TSP_US_CAPITALS)
}

sealed trait TravelingSalesmanVariation  extends ProblemVariation {

  /** @return the number of cities to visit */
  def getNumCities: Int

  /** Some random initial permutation of the cities */
  def getInitialGuess: ParameterArray = {
    val num = this.getNumCities
    val params = for (i <- 0 until num) yield new IntegerParameter(i, 0, num - 1, "p" + i)
    val guess = new PermutedParameterArray(params.toArray, RANDOM)
    guess.setFitness(evaluateFitness(guess))
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
    new PermutedParameterArray(params.toArray, RANDOM)
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
    Array(6734, 1453),
    Array(2233, 10),
    Array(5530, 1424),
    Array(401, 841),
    Array(3082, 1644),
    Array(7608, 4458),
    Array(7573, 3716),
    Array(7265, 1268),
    Array(6898, 1885),
    Array(1112, 2049),
    Array(5468, 2606),
    Array(5989, 2873),
    Array(4706, 2674),
    Array(4612, 2035),
    Array(6347, 2683),
    Array(6107, 669),
    Array(7611, 5184),
    Array(7462, 3590),
    Array(7732, 4723),
    Array(5900, 3561),
    Array(4483, 3369),
    Array(6101, 1110),
    Array(5199, 2182),
    Array(1633, 2809),
    Array(4307, 2322),
    Array(675, 1006),
    Array(7555, 4819),
    Array(7541, 3981),
    Array(3177, 756),
    Array(7352, 4506),
    Array(7545, 2801),
    Array(3245, 3305),
    Array(6426, 3173),
    Array(4608, 1198),
    Array(23, 2216),
    Array(7248, 3779),
    Array(7762, 4595),
    Array(7392, 2244),
    Array(3484, 2829),
    Array(6271, 2135),
    Array(4985, 140),
    Array(1916, 1569),
    Array(7280, 4899),
    Array(7509, 3239),
    Array(10, 2676),
    Array(6807, 2993),
    Array(5185, 3258),
    Array(3023, 1942)
  )

  val errorTolerances = ErrorTolerances(GLOB_SAMP_TOL, RELAXED_TOL, 0.01, 0.04, RELAXED_TOL, 0.042, 0.042, BASE_TOLERANCE)

  override def getNumCities = 48
  override def getShortestPathLength = 10627.75

  override def getExactSolution: PermutedParameterArray = {
    val solution = createSolution(Array[Int](2, 4)) // not knoen
    solution.setFitness(0)
    solution
  }

  override def getFitnessRange = 120000.0

  override def evaluateFitness(a: ParameterArray): Double =
    computeCostFromCoords(a, CAP_COORDS) - getShortestPathLength
}
