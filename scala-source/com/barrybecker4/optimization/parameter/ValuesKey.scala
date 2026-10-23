// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import java.util.Arrays

/**
  * A hashable, comparable snapshot of the values in a parameter array: equal exactly when the values are equal.
  * It holds one primitive array, so making one and hashing it allocates no boxed numbers or strings, which matters
  * when a search checks every candidate against the ones it has seen.
  */
final class ValuesKey private (private val values: Array[Double]) {

  private lazy val cachedHash: Int = Arrays.hashCode(values)

  override def hashCode: Int = cachedHash

  override def equals(other: Any): Boolean = other match {
    case that: ValuesKey => (this eq that) || Arrays.equals(values, that.values)
    case _ => false
  }

  override def toString: String = values.mkString("ValuesKey(", ", ", ")")
}

object ValuesKey {

  /** @return the key for the values of the given array, in order. */
  def of(pa: ParameterArray): ValuesKey = {
    val values = new Array[Double](pa.size)
    var i = 0
    while (i < values.length) {
      // `+ 0.0` turns -0.0 into 0.0, which compare equal as numbers but not as bits.
      values(i) = pa.get(i).getValue + 0.0
      i += 1
    }
    new ValuesKey(values)
  }
}
