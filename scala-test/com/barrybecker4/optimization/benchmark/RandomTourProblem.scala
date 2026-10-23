// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, ParameterArrayWithFitness, PermutedParameterArray, Swap}

import scala.util.Random

/**
  * A traveling salesman problem on cities placed at random in the unit square, with as many cities as are asked for.
  * It is the benchmark's way to see how a strategy scales with the length of the permutation.
  * Evaluating a tour reads every parameter, like the real puzzles do, so the cost of an evaluation grows with the
  * number of cities. The optimal tour is not known.
  * @param numCities the number of cities to visit
  * @param seed decides where the cities are and the order in the initial guess
  */
class RandomTourProblem(numCities: Int, seed: Long) extends OptimizeeProblem {

  require(numCities >= 3, s"A tour needs at least 3 cities, but got $numCities")

  private val cityRandom = new Random(seed)
  private val xs = Array.fill(numCities)(cityRandom.nextDouble())
  private val ys = Array.fill(numCities)(cityRandom.nextDouble())
  private val distances = Array.tabulate(numCities, numCities)((i, j) => Math.hypot(xs(i) - xs(j), ys(i) - ys(j)))

  override def getName: String = s"Random tour of $numCities cities"

  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(tour: ParameterArray): Double = {
    var total = 0.0
    var previous = tour.get(numCities - 1).getValue.toInt
    for (i <- 0 until numCities) {
      val city = tour.get(i).getValue.toInt
      total += distances(previous)(city)
      previous = city
    }
    total
  }

  /** A swap changes at most four legs of the tour, so its new length is worked out from those. Other moves use the default. */
  override def evaluateMove(tour: ParameterArray, tourLength: Double, move: Move): Double = move match {
    case Swap(first, second) => tourLength + lengthChangeOfSwap(tour, first, second)
    case _ => super.evaluateMove(tour, tourLength, move)
  }

  private def lengthChangeOfSwap(tour: ParameterArray, first: Int, second: Int): Double = {
    if (first == second) return 0.0
    val legs = legsAround(first, second)
    legs.map(leg => legLength(tour, leg, first, second, swapped = true)).sum -
      legs.map(leg => legLength(tour, leg, first, second, swapped = false)).sum
  }

  /** The legs, each named by the position it starts at, that start or end at either position. Each leg is listed once. */
  private def legsAround(first: Int, second: Int): Array[Int] =
    Array(before(first), first, before(second), second).distinct

  private def before(position: Int): Int = (position + numCities - 1) % numCities

  /** The length of the leg from `start` to the next position, in the tour as it is or with the two positions swapped. */
  private def legLength(tour: ParameterArray, start: Int, first: Int, second: Int, swapped: Boolean): Double = {
    val next = (start + 1) % numCities
    distances(cityAt(tour, start, first, second, swapped))(cityAt(tour, next, first, second, swapped))
  }

  private def cityAt(tour: ParameterArray, position: Int, first: Int, second: Int, swapped: Boolean): Int =
    if (swapped && position == first) tour.get(second).getValue.toInt
    else if (swapped && position == second) tour.get(first).getValue.toInt
    else tour.get(position).getValue.toInt

  /** The cities in a random order that depends on the seed. */
  override def getInitialGuess: ParameterArray = {
    val order = new Random(seed + 1).shuffle((0 until numCities).toList)
    val params = order.map(city => new IntegerParameter(city, 0, numCities - 1, "p" + city))
    PermutedParameterArray(params.toIndexedSeq, new Random(seed + 2))
  }

  /** The best tour is not known, so this is the cities in order, which is only a fair tour. */
  override def getExactSolution: ParameterArrayWithFitness = {
    val inOrder = (0 until numCities).map(city => new IntegerParameter(city, 0, numCities - 1, "p" + city))
    val tour = PermutedParameterArray(inOrder, new Random(seed))
    ParameterArrayWithFitness(tour, evaluateFitness(tour))
  }

  /** A random tour is about half as long as it would be if every hop crossed the square. */
  override def getFitnessRange: Double = numCities * 0.5
}
