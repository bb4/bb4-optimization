package com.barrybecker4.optimization.parameter.sampling

import com.barrybecker4.optimization.parameter.PermutedParameterArray
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import org.scalatest.FunSuite

import scala.util.Random

class PermutedGlobalSampleSuite extends FunSuite {

  test("sampling when 4 permuted items - 3 permutations") {
    val num = 4
    val rnd = new Random(1)
    val params = for (i <- 0 until num) yield new IntegerParameter(i, 0, num - 1, "p" + i)
    val pa = PermutedParameterArray(params, rnd)
    val sampler = new PermutedGlobalSampler(pa, 3)

    val result: Array[PermutedParameterArray] = sampler.toArray
    assertResult(
      """
        |parameter[0] = p3 = 3.0 [0, 3.0]
        |parameter[1] = p0 = 0 [0, 3.0]
        |parameter[2] = p1 = 1.00 [0, 3.0]
        |parameter[3] = p2 = 2.0 [0, 3.0]
        |
        |parameter[0] = p3 = 3.0 [0, 3.0]
        |parameter[1] = p0 = 0 [0, 3.0]
        |parameter[2] = p2 = 2.0 [0, 3.0]
        |parameter[3] = p1 = 1.00 [0, 3.0]
        |
        |parameter[0] = p0 = 0 [0, 3.0]
        |parameter[1] = p2 = 2.0 [0, 3.0]
        |parameter[2] = p3 = 3.0 [0, 3.0]
        |parameter[3] = p1 = 1.00 [0, 3.0]
        |""".stripMargin.replaceAll("\r\n", "\n")) { result.mkString("") }
  }

  test("sampling when 3 permuted items - 1 permutations") {
    val num = 3
    val rnd = new Random(1)
    val params = for (i <- 0 until num) yield new IntegerParameter(i, 0, num - 1, "p" + i)
    val pa = PermutedParameterArray(params, rnd)
    val sampler = new PermutedGlobalSampler(pa, 1)

    val result: Array[PermutedParameterArray] = sampler.toArray
    assertResult(
      """
        |parameter[0] = p1 = 1.00 [0, 2.0]
        |parameter[1] = p2 = 2.0 [0, 2.0]
        |parameter[2] = p0 = 0 [0, 2.0]
        |""".stripMargin.replaceAll("\r\n", "\n")) { result.mkString("") }
  }

  // There are only 3 permutation for 3 items
  test("sampling 4 permutations from 3 permuted items. Will get only 3") {
    val num = 3
    val rnd = new Random(1)
    val params = for (i <- 0 until num) yield new IntegerParameter(i, 0, num - 1, "p" + i)
    val pa = PermutedParameterArray(params, rnd)
    val sampler = new PermutedGlobalSampler(pa, 4)


    val result: Array[PermutedParameterArray] = sampler.toArray
    assertResult(3) {result.length}
  }
}
