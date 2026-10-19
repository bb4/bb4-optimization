package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.parameter.PermutedParameterArray
import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import com.barrybecker4.optimization.strategy.optimizees.FlatLandscapeOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/**
  * A permutation whose parameters are only an encoding: the candidate it stands for is `tag` as well as the order,
  * like a puzzle path whose ids are ranks within that particular path.
  */
private final class TaggedPermutation(values: IndexedSeq[Parameter], val tag: Int, rnd: Random)
    extends PermutedParameterArray(values, rnd) {

  override def getRandomNeighbor(radius: Double): TaggedPermutation =
    new TaggedPermutation(this.params, rnd.nextInt(1000000), rnd)

  override def candidateIdentity: Any = (tag, super.candidateIdentity)
}

private object TaggedPermutation {
  def apply(tag: Int, rnd: Random = new Random(1)): TaggedPermutation =
    new TaggedPermutation((0 until 4).map(i => IntegerParameter(i, 0, 3, "p")), tag, rnd)
}

class SolutionsSuite extends AnyFunSuite {

  private def ints(values: Int*): PermutedParameterArray =
    PermutedParameterArray(values.map(v => IntegerParameter(v, 0, 3, "p")).toIndexedSeq, new Random(1))

  test("by default two arrays are the same candidate when their parameter values are equal") {
    assert(Solutions.same(ints(0, 1, 2, 3), ints(0, 1, 2, 3)))
    assert(!Solutions.same(ints(0, 1, 2, 3), ints(1, 0, 2, 3)))
    assert(Solutions.key(ints(0, 1, 2, 3)) == Solutions.key(ints(0, 1, 2, 3)))
    assert(Solutions.key(ints(0, 1, 2, 3)) != Solutions.key(ints(3, 2, 1, 0)))
  }

  test("arrays of different classes are never the same candidate") {
    assert(!Solutions.same(ints(0, 1, 2, 3), TaggedPermutation(0)))
  }

  test("an array that overrides candidateIdentity is compared by it, not by its parameter values") {
    val a = TaggedPermutation(1)
    val b = TaggedPermutation(2)
    assert((0 until 4).forall(i => a.get(i).getValue == b.get(i).getValue))
    assert(!Solutions.same(a, b))
    assert(Solutions.key(a) != Solutions.key(b))
    assert(Solutions.same(a, TaggedPermutation(1)))
    assert(Solutions.key(a) == Solutions.key(TaggedPermutation(1)))
  }

  test("neighbors that differ only in what candidateIdentity captures are all kept") {
    val seed = TaggedPermutation(0)
    val neighbors = InitialPopulation.proposeNeighbors(seed, desiredSize = 10, radius = 1.0)
    assert(neighbors.size == 9)
    assert(neighbors.map(Solutions.key).distinct.size == 9)
  }

  test("genetic search can build an initial population from such neighbors") {
    val strategy = new GeneticSearchStrategy(new FlatLandscapeOptimizee, new Random(3))
    val population = strategy.findInitialPopulation(TaggedPermutation(0))
    assert(population.size > 1)
  }
}
