// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.parameter.*

import scala.util.Random

/**
  * Gives multistart seeds their own [[Random]].
  *
  * Seeds from a global sample often share the RNG of the initial guess, and local searches mutate it. Forking per
  * seed (after the primary) means later starts do not depend on how many draws earlier local runs consumed.
  */
private[strategy] object SeedForking {

  /**
    * Seed for a forked RNG. Derived from the parameter values and fork index, not `hashCode`, which for case-class
    * arrays includes the identity hash of the embedded [[Random]] and so differs from run to run.
    */
  def forkSalt(pa: ParameterArray, forkIdx: Int): Long = {
    val values = (0 until pa.size).map(i => pa.get(i).getValue)
    values.hashCode.toLong * 31L + forkIdx.toLong * 1103515245L
  }

  /** A copy of `s` that has its own deterministic RNG; `other` array types are returned unchanged. */
  def fork(s: ParameterArrayWithFitness, forkIdx: Int): ParameterArrayWithFitness = {
    val forked = new Random(forkSalt(s.pa, forkIdx))
    val newPa = s.pa match {
      case v: VariableLengthIntSet =>
        VariableLengthIntSet.createInstance((0 until v.size).map(v.get), v.fullSeq, forked)
      case n: NumericParameterArray =>
        NumericParameterArray(n.params, n.numSteps, forked)
      case p: PermutedParameterArray =>
        p.forkWithRnd(forked)
      case other => other
    }
    ParameterArrayWithFitness(newPa, s.fitness)
  }
}
