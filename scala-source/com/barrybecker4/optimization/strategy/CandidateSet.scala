// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.strategy.genetic.Solutions

import scala.collection.mutable

/**
  * A set of parameter arrays where two are the same member when they are the same candidate, as told by
  * [[ParameterArray.candidateIdentity]]. A strategy uses it to skip candidates it has already tried.
  *
  * A plain hash set of arrays would hash and compare every parameter object of the case class for each lookup. This one
  * hashes the primitive values of the identity once per candidate, which is much cheaper on long arrays.
  */
class CandidateSet extends mutable.AbstractSet[ParameterArray] {

  private val members = mutable.HashMap[Solutions.Key, ParameterArray]()

  override def contains(candidate: ParameterArray): Boolean = members.contains(Solutions.key(candidate))

  override def addOne(candidate: ParameterArray): this.type = {
    members.getOrElseUpdate(Solutions.key(candidate), candidate)
    this
  }

  override def subtractOne(candidate: ParameterArray): this.type = {
    members.remove(Solutions.key(candidate))
    this
  }

  override def iterator: Iterator[ParameterArray] = members.valuesIterator

  override def size: Int = members.size

  override def clear(): Unit = members.clear()
}
