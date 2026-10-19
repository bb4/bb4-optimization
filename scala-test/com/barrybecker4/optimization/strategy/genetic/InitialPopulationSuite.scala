package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.parameter.NumericParameterArray
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class InitialPopulationSuite extends AnyFunSuite {

  test("proposes enough distinct neighbors to fill the population, none equal to the seed") {
    val seed = NumericParameterArraySuiteLike.twoDoubleParams(1.0, 2.0)
    val neighbors = InitialPopulation.proposeNeighbors(seed, desiredSize = 10, radius = 1.0)
    assert(neighbors.size == 9) // the seed is the tenth member
    assert(neighbors.map(Solutions.key).distinct.size == 9)
    assert(!neighbors.exists(Solutions.same(_, seed)))
  }

  test("a desired size of one needs no neighbors") {
    val seed = NumericParameterArraySuiteLike.twoDoubleParams(1.0, 2.0)
    assert(InitialPopulation.proposeNeighbors(seed, 1, 1.0).isEmpty)
  }

  test("terminates with fewer neighbors when the search space is tiny") {
    val rnd = new Random(3)
    val seed = NumericParameterArray(IndexedSeq(new IntegerParameter(1, 0, 2, "i")), rnd = rnd)
    val neighbors = InitialPopulation.proposeNeighbors(seed, desiredSize = 50, radius = 1.0)
    assert(neighbors.size <= 2, s"only 2 other values exist, got ${neighbors.size}")
    assert(neighbors.map(Solutions.key).distinct.size == neighbors.size)
  }

  test("neighbors stay within the parameter bounds") {
    val rnd = new Random(5)
    val seed = NumericParameterArray(IndexedSeq(DoubleParameter(9.5, -10.0, 10.0, "x")), rnd = rnd)
    val neighbors = InitialPopulation.proposeNeighbors(seed, 30, 1.0)
    assert(neighbors.forall(n => n.get(0).getValue >= -10.0 && n.get(0).getValue <= 10.0))
  }
}
