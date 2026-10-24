// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}

import scala.util.Random

/**
  * Color the nodes of a graph with a few colors so that no edge joins two nodes of the same color. The graph is made
  * around a hidden coloring: the colors are chosen first and only edges between nodes of different colors are added, so
  * a solution is known to exist. The fitness is the number of edges whose ends have the same color, and 0 is a solution.
  *
  * It is the example of a problem with integer variables rather than a permutation, and the case min-conflicts was
  * made for: a node with a clashing edge can simply be given a color that clashes less.
  * @param numNodes the number of nodes
  * @param numColors the number of colors available
  * @param averageDegree how many edges each node has on average; more makes the problem harder
  * @param seed decides the graph, the hidden coloring and the initial guess
  */
class GraphColoringProblem(numNodes: Int, numColors: Int, averageDegree: Double, seed: Long) extends OptimizeeProblem {

  require(numNodes >= 2 && numColors >= 2, s"Need 2 or more nodes and colors, but got $numNodes and $numColors")
  require(averageDegree > 0 && averageDegree < numNodes * (numColors - 1.0) / numColors,
    s"The average degree must be positive and leave room for the edges, but got $averageDegree")

  private val random = new Random(seed)
  private val hiddenColoring: IndexedSeq[Int] = IndexedSeq.fill(numNodes)(random.nextInt(numColors))
  private val edges: IndexedSeq[(Int, Int)] = createEdges()

  override def getName: String = s"Coloring $numNodes nodes with $numColors colors"

  override def evaluateByComparison: Boolean = false

  /** The number of edges that join two nodes of the same color. */
  override def evaluateFitness(coloring: ParameterArray): Double =
    edges.count { case (a, b) => colorOf(coloring, a) == colorOf(coloring, b) }.toDouble

  private def colorOf(coloring: ParameterArray, node: Int): Int = coloring.get(node).getValue.toInt

  override def getInitialGuess: ParameterArray = {
    val initialRandom = new Random(seed + 1)
    coloringOf(IndexedSeq.fill(numNodes)(initialRandom.nextInt(numColors)), seed + 2)
  }

  override def getExactSolution: ParameterArrayWithFitness = {
    val coloring = coloringOf(hiddenColoring, seed)
    ParameterArrayWithFitness(coloring, evaluateFitness(coloring))
  }

  /** A random coloring clashes on about one edge in `numColors`. */
  override def getFitnessRange: Double = edges.size.toDouble / numColors

  def numEdges: Int = edges.size

  private def coloringOf(colors: IndexedSeq[Int], randomSeed: Long): ParameterArray = {
    val params: IndexedSeq[Parameter] = colors.indices.map(node => IntegerParameter(colors(node), 0, numColors - 1, "n" + node))
    NumericParameterArray(params, rnd = new Random(randomSeed))
  }

  /** Random pairs of nodes with different hidden colors, until there are enough edges; the same edge is added once. */
  private def createEdges(): IndexedSeq[(Int, Int)] = {
    val wanted = (numNodes * averageDegree / 2).toInt
    val chosen = scala.collection.mutable.LinkedHashSet.empty[(Int, Int)]
    while (chosen.size < wanted) {
      val (a, b) = (random.nextInt(numNodes), random.nextInt(numNodes))
      if (hiddenColoring(a) != hiddenColoring(b)) chosen += ((Math.min(a, b), Math.max(a, b)))
    }
    chosen.toIndexedSeq
  }
}
