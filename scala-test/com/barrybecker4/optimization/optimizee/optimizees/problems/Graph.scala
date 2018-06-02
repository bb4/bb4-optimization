// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems


/**
  * A simple undirected graph representation.
  * Only connections between nodes are recorded, not the distance between them.
  * The graph connections are defined by a list of lists.
  * Assume that each node in the graph is assigned an integer number.
  * Each list entry corresponds to a node with a number that matches the index in the array.
  * The list entry contains the numbers of the nodes that are connected to it.
  * @author Barry Becker
  */
class Graph @SafeVarargs private[problems](val nodeNeighbors: Seq[List[Integer]]) {

  def size: Int = nodeNeighbors.length

  /** @return the number of nodes that are more than one edge link away from the specified marked vertices */
  private[problems] def getNumNotWithinOneHop(marked: Array[Int]) = {
    var total = 0
    var i = 0
    val markedSet = marked.toSet
    for (i <- nodeNeighbors.indices) {
      if (!markedSet.contains(i)) {
        val v: Int = if (isNodeOneHopAway(i, markedSet)) 0 else 1
        total += v
      }
    }
    println("out of " + nodeNeighbors.size + " nodes, " + total + " are not within one hop from " + marked)
    total
  }

  /** @param i node to start searching from
    * @param marked set of marked nodes
    * @return true if node i is only one hop from one of the marked nodes
    */
  private def isNodeOneHopAway(i: Int, marked: Set[Int]): Boolean = {
    val nbrs = nodeNeighbors(i)
    marked.exists(nbrs.contains)
  }
}