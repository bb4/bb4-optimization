package com.barrybecker4.optimization.parameter.sampling

import com.barrybecker4.optimization.parameter.VariableLengthIntSet
import com.barrybecker4.optimization.parameter.VariableLengthIntSet.createParam
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class VariableLengthGlobalSamplerSuite extends AnyFunSuite {

  /** fullSeq length 4 gives 2^4 - 1 = 15 nonempty subset configurations; request fewer than that. */
  test("iterator stops after numSamples when numSamples is below totalConfigurations") {
    val rnd = new Random(1)
    val fullSeq = IndexedSeq(2, -1, 3, -4)
    val params = fullSeq.map(createParam)
    val vl = VariableLengthIntSet.createInstance(params, fullSeq, rnd)
    val requested = 3L
    val iter = vl.findGlobalSamples(requested)
    assert(iter.take(3).length == 3)
    assertThrows[NoSuchElementException] {
      iter.next()
    }
  }
}
