package com.barrybecker4.optimization.parameter.sampling

import com.barrybecker4.optimization.parameter.PermutedParameterArray
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import org.scalatest.FunSuite

class PermutedGlobalSampleSuite extends FunSuite {

  test("sampling when 4 permuted items - 3 permutations") {
    val num = 4
    val params = for (i <- 0 until num) yield new IntegerParameter(i, 0, num - 1, "p" + i)
    val pa = new PermutedParameterArray(params.toArray)
    val sampler = new PermutedGlobalSampler(pa, 3)

    val result: Array[PermutedParameterArray] = sampler.toArray
    assertResult(
      """
        |parameter[0] = p3 = 3.0 [0, 3.0]
        |parameter[1] = p0 = 0 [0, 3.0]
        |parameter[2] = p1 = 1.00 [0, 3.0]
        |parameter[3] = p2 = 2.0 [0, 3.0]
        |fitness = 0.0
        |parameter[0] = p3 = 3.0 [0, 3.0]
        |parameter[1] = p0 = 0 [0, 3.0]
        |parameter[2] = p2 = 2.0 [0, 3.0]
        |parameter[3] = p1 = 1.00 [0, 3.0]
        |fitness = 0.0
        |parameter[0] = p0 = 0 [0, 3.0]
        |parameter[1] = p2 = 2.0 [0, 3.0]
        |parameter[2] = p3 = 3.0 [0, 3.0]
        |parameter[3] = p1 = 1.00 [0, 3.0]
        |fitness = 0.0""".stripMargin.replaceAll("\r\n", "\n")) { result.mkString("") }
  }

  test("sampling when 3 permuted items - 1 permutations") {
    val num = 3
    val params = for (i <- 0 until num) yield new IntegerParameter(i, 0, num - 1, "p" + i)
    val pa = new PermutedParameterArray(params.toArray)
    val sampler = new PermutedGlobalSampler(pa, 1)

    val result: Array[PermutedParameterArray] = sampler.toArray
    assertResult(
      """
        |parameter[0] = p2 = 2.0 [0, 2.0]
        |parameter[1] = p0 = 0 [0, 2.0]
        |parameter[2] = p1 = 1.00 [0, 2.0]
        |fitness = 0.0""".stripMargin.replaceAll("\r\n", "\n")) { result.mkString("") }
  }

  test("sampling when 3 permuted items - 4 permutations") {
    val num = 3
    val params = for (i <- 0 until num) yield new IntegerParameter(i, 0, num - 1, "p" + i)
    val pa = new PermutedParameterArray(params.toArray)
    val sampler = new PermutedGlobalSampler(pa, 4)

    assertThrows[IllegalStateException] {
      // runs out of samples
      val result: Array[PermutedParameterArray] = sampler.toArray
    }
  }
}
