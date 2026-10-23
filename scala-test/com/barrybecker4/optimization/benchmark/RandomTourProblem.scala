// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, ParameterArrayWithFitness, PermutedParameterArray, Swap, SwapSequence}

import scala.util.Random

object RandomTourProblem {
  /** Past this many swaps, finding the touched positions costs more than evaluating the whole tour. */
  val MAX_SWAPS_FOR_DELTA = 8
}

/**
  * A traveling salesman problem on cities placed at random in the unit square, with as many cities as are asked for.
  * It is the benchmark's way to see how a strategy scales with the length of the permutation.
  * Evaluating a tour reads every parameter, like the real puzzles do, so the cost of an evaluation grows with the
  * number of cities. The optimal tour is not known.
  * @param numCities the number of cities to visit
  * @param seed decides where the cities are and the order in the initial guess
  */
class RandomTourProblem(numCities: Int, seed: Long) extends OptimizeeProblem {
  import RandomTourProblem.MAX_SWAPS_FOR_DELTA


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

  /** A swap changes at most four legs of the tour, and a few swaps only a few more, so the new length is worked out from those. */
  override def evaluateMove(tour: ParameterArray, tourLength: Double, move: Move): Double = move match {
    case Swap(first, second) => tourLength + lengthChangeOfSwap(tour, first, second)
    case SwapSequence(swaps) if swaps.size <= MAX_SWAPS_FOR_DELTA => tourLength + lengthChangeOfSwaps(tour, swaps)
    case _ => super.evaluateMove(tour, tourLength, move)
  }

  private def lengthChangeOfSwap(tour: ParameterArray, first: Int, second: Int): Double =
    if (first == second) 0.0
    else legsLength(tour, first, second, swapped = true) - legsLength(tour, first, second, swapped = false)

  /**
    * The total length of the legs that start or end at either position (the leg from a position goes to the next one),
    * in the tour as it is or with the two positions swapped. There are four such legs, but next to each other the
    * positions share one, which must be counted once. This allocates nothing, because it runs once per proposal.
    */
  private def legsLength(tour: ParameterArray, first: Int, second: Int, swapped: Boolean): Double = {
    def leg(start: Int) = legLength(tour, start, first, second, swapped)
    var total = leg(first) + leg(second)
    if (before(first) != second) total += leg(before(first))
    if (before(second) != first) total += leg(before(second))
    total
  }

  /** A few swaps: only the legs at the positions they touch can change. */
  private def lengthChangeOfSwaps(tour: ParameterArray, swaps: IndexedSeq[Swap]): Double = {
    val swapped = new SwappedTour(tour, swaps)
    val starts = swapped.positions.flatMap(p => Array(p, before(p))).distinct
    starts.map(start => swapped.legLength(start)).sum - starts.map(start => unswappedLegLength(tour, start)).sum
  }

  private def unswappedLegLength(tour: ParameterArray, start: Int): Double =
    distances(tour.get(start).getValue.toInt)(tour.get((start + 1) % numCities).getValue.toInt)

  /** The tour after some swaps, which remembers only the cities at the positions that the swaps touch. */
  private final class SwappedTour(tour: ParameterArray, swaps: IndexedSeq[Swap]) {
    val positions: Array[Int] = swaps.flatMap(s => Array(s.first, s.second)).distinct.toArray
    private val cities: Array[Int] = positions.map(p => tour.get(p).getValue.toInt)
    swaps.foreach(s => swapCities(positions.indexOf(s.first), positions.indexOf(s.second)))

    private def swapCities(a: Int, b: Int): Unit = {
      val saved = cities(a)
      cities(a) = cities(b)
      cities(b) = saved
    }

    private def cityAt(position: Int): Int = {
      val k = positions.indexOf(position)
      if (k >= 0) cities(k) else tour.get(position).getValue.toInt
    }

    def legLength(start: Int): Double = distances(cityAt(start))(cityAt((start + 1) % numCities))
  }

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
