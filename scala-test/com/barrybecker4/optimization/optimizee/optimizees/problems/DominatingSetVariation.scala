// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems

import com.barrybecker4.optimization.optimizee.optimizees.ErrorTolerances
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness, VariableLengthIntArray}
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import com.barrybecker4.optimization.optimizee.optimizees.ProblemVariation
import DominatingSetVariation._

import scala.util.Random


object DominatingSetVariation {
  val VALUES = IndexedSeq(SIMPLE_DS, PENTAGRAM_DS, TYPICAL_DS)
  val ONE_HOP_WEIGHT = 0.6  // amount to penalize vertices not one hop from the cover
  val RND = new Random(1)
}

/**
  * An enum for different sorts of dominating set problems.
  * http://en.wikipedia.org/wiki/Dominating_set
  * @author Barry Becker
  */
sealed trait DominatingSetVariation extends ProblemVariation {

  private lazy val exactSolutionCost: Double = computeCost(getExactSolution.pa)

  /** @return an array of the node ineices in the graph */
  def getAllNodes: Set[Int] = (0 until adjacencies.size).toSet

  /** The graph containing the node adjacency information */
  protected def adjacencies: Graph

  /** Some random initial set of marked nodes.
    * One half or one third of the nodes is probably a good starting point.
    */
  def getInitialGuess: ParameterArray = {
    val num = getAllNodes.size
    // just add some of the nodes
    val params: Seq[IntegerParameter] =
      for (i <- 0 until num by 3) yield new IntegerParameter(i, 0, num - 1, "p" + i)

    val pa = VariableLengthIntArray.createInstance(params.toIndexedSeq, getAllNodes, RND)
    //ParameterArrayWithFitness(pa, getScore(getMarked(pa)))
    pa
  }

  private def getMarked(pa: ParameterArray): Array[Int] =
    (for (i <- 0 until pa.size) yield pa.get(i).getValue.toInt).toArray


  /** Evaluate fitness for the candidate solution to the dominating set problem.
    * @param pa param array
    * @return fitness value. The closer it is to 0 the better. When 0 it is the exact cover.
    */
  def evaluateFitness(pa: ParameterArray): Double = computeCost(pa) - exactSolutionCost

  /** Approximate value of maxCost - minCost */
  def getFitnessRange: Double

  /** Assume that the parameter array contains 0 based integers.
    * @param params last best guess at dominating set.
    * @return the total cost of the candidate vertex cover.
    *         It is defined as the number of nodes in the cover + the number of nodes not within 1 hop from that set.
    */
  protected def computeCost(params: ParameterArray): Double = {
    getScore(getMarked(params))
  }

  private def getScore(marked: Array[Int]) = {
    val numNot1hop = adjacencies.getNumNotWithinOneHop(marked)
    // println("num marked = " + marked.length + " num not within 1 hop = " + numNot1hop)
    marked.length + ONE_HOP_WEIGHT * numNot1hop
  }

  /** @return the error tolerance percent for a specific optimization strategy */
  override def getErrorTolerancePercent(opt: OptimizationStrategyType): Double =
    errorTolerances.getErrorTolerancePercent(opt)

  /** Error tolerance for each search strategy and variation of the problem.
    * @return error tolerance percent
    */
  protected def errorTolerances: ErrorTolerances

  /** Create the solution based on the ordered list of cities.
    * @param nodeList optimal dominating set of marked nodes. May not be unique.
    * @return optimal solution (to compare against at the end of the test).
    */
  def createSolution(nodeList: Int*): ParameterArrayWithFitness = {
    val allNodes = getAllNodes
    val params =
      for (i <- 0 until nodeList.length)
        yield new IntegerParameter(nodeList(i), 0, allNodes.size - 1, "p" + i)

    ParameterArrayWithFitness(VariableLengthIntArray.createInstance(params, allNodes, RND), 0)
  }
}


/**
  * Trivial example.
  * There are three nodes, A, B, C. This list of lists defines the connectivity of the graph.
  */
case object SIMPLE_DS extends DominatingSetVariation {

  val adjacencies = new Graph(Seq(List(1, 2), List(0, 2), List(0, 1)))
  val errorTolerances = ErrorTolerances(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

  override def getExactSolution: ParameterArrayWithFitness = createSolution(0)
  override def getFitnessRange = 7.0
}

/**
  * This graph looks like a 5 point star inside of a pentagon.
  * See http://mathworld.wolfram.com/DominatingSet.html
  * Also known as the Peterson graph.
  */
case object PENTAGRAM_DS extends DominatingSetVariation {
  val adjacencies = new Graph(Seq(
    List(2, 3, 5), List(3, 4, 6), List(0, 4, 7), List(0, 1, 8), List(1, 2, 9),
    List(0, 6, 9), List(1, 5, 7), List(2, 6, 8), List(3, 7, 9), List(4, 5, 8)
  ))

  val errorTolerances = ErrorTolerances(4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0)

  /** This is one of several possible solutions that gives an optimal fitness of 0 */
  override def getExactSolution: ParameterArrayWithFitness = createSolution(0, 1, 8)
  override def getFitnessRange = 15.0
}

case object TYPICAL_DS extends DominatingSetVariation {

  val adjacencies = new Graph(Seq(
    List(15, 21, 25), List(2, 4, 7), List(1, 3, 5, 7), List(2, 5, 8, 9), List(1, 6, 12), // 4
    List(2, 3, 8, 13), List(4, 10, 11, 12), List(1, 2, 12, 13), List(3, 5, 9, 14), // 8
    List(3, 8, 15), List(6, 11, 18), List(6, 10, 16), List(4, 6, 7, 16, 17), // 12
    List(5, 7, 14, 17), List(8, 13, 15, 17), List(0, 9, 14, 21), List(11, 12, 19), // 16
    List(12, 13, 14, 20, 21), List(10, 19, 22, 24), List(16, 18, 20), List(17, 19, 22, 23), // 20
    List(0, 15, 17, 23), List(18, 20, 23, 24), List(20, 21, 22, 25), List(18, 22, 25), // 24
    List(0, 23, 24)
  ))

  val errorTolerances = ErrorTolerances(16.0, 2.01, 1.21, 1.2, 1.2, 1.2, 0)

  /** This is one of several possible solutions that gives an optimal fitness of 0 */
  override def getExactSolution: ParameterArrayWithFitness = createSolution(6, 7, 8, 19, 21, 24)
  override def getFitnessRange = 50.0
}
