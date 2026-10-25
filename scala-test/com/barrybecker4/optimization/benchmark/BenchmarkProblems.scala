// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.optimizee.optimizees.problems.*

/**
  * A problem to benchmark. `create` makes a fresh problem for the seed, so that repeated runs start from the same place.
  * @param isConstraint true when the problem has a solution that is known to exist and the goal is to reach it. Such a
  *                     problem is judged by how many runs solve it and how quickly, and a run stops when it does.
  */
case class BenchmarkProblem(name: String, create: Long => OptimizeeProblem, isConstraint: Boolean = false)

/** The problems that are benchmarked. The tours of increasing length show how a strategy scales with permutation size. */
object BenchmarkProblems {

  val ALL: Seq[BenchmarkProblem] = Seq(
    BenchmarkProblem("tsp-standard", seed => { resetSharedRandom(seed); new TravelingSalesmanProblem(TSP_STANDARD) }),
    BenchmarkProblem("tsp-us-capitals", seed => { resetSharedRandom(seed); new TravelingSalesmanProblem(TSP_US_CAPITALS) }),
    BenchmarkProblem("tour-50", seed => new RandomTourProblem(50, seed)),
    BenchmarkProblem("tour-200", seed => new RandomTourProblem(200, seed)),
    BenchmarkProblem("subset-sum", seed => new SubsetSumProblem(TYPICAL_SS)),
    BenchmarkProblem("dominating-set", seed => new DominatingSetProblem(TYPICAL_DS))
  )

  /**
    * Problems where the goal is to find a solution that is known to exist. They are run on their own (see the
    * `--solve-rate` option), with more seeds, since a solve rate needs more samples than a median does.
    * The sizes are ones where the existing strategies solve some runs and not others in a couple of seconds.
    */
  val CONSTRAINT: Seq[BenchmarkProblem] = Seq(
    BenchmarkProblem("queens-100", seed => new NQueensProblem(100, seed), isConstraint = true),
    BenchmarkProblem("queens-400", seed => new NQueensProblem(400, seed), isConstraint = true),
    BenchmarkProblem("coloring-200", seed => new GraphColoringProblem(200, 4, 7.0, seed), isConstraint = true),
    BenchmarkProblem("all-interval-14", seed => new AllIntervalProblem(14, seed), isConstraint = true)
  )

  /** @return the problems with these names, or all of the ordinary ones if there are no names */
  def named(names: Seq[String]): Seq[BenchmarkProblem] =
    if (names.isEmpty) ALL else names.map(n => (ALL ++ CONSTRAINT).find(_.name == n).getOrElse(throw new IllegalArgumentException(
      s"Unknown problem '$n'. The problems are ${(ALL ++ CONSTRAINT).map(_.name).mkString(", ")}")))

  /** The TSP variations pick their initial guess from one shared random generator, so make it start the same. */
  private def resetSharedRandom(seed: Long): Unit = TravelingSalesmanVariation.RANDOM.setSeed(seed)
}
