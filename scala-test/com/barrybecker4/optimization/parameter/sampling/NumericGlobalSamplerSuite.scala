package com.barrybecker4.optimization.parameter.sampling

import com.barrybecker4.optimization.parameter.NumericParameterArray
import org.scalatest.FunSuite
import scala.util.Random


class NumericGlobalSamplerSuite extends FunSuite {

  test("1d sampling") {
    val params = new NumericParameterArray(Array(0.5), Array(0.0), Array(1.0), Array("p"), new Random(1))
    val sampler = new NumericGlobalSampler(params, 4)

    val result: Array[NumericParameterArray] = sampler.toArray
    assertResult(
      """
        |parameter[0] = p = 0.125 [0, 1.00]
        |fitness = 0.0
        |parameter[0] = p = 0.375 [0, 1.00]
        |fitness = 0.0
        |parameter[0] = p = 0.625 [0, 1.00]
        |fitness = 0.0
        |parameter[0] = p = 0.875 [0, 1.00]
        |fitness = 0.0""".stripMargin.replaceAll("\r\n", "\n")) { result.mkString("")}

    assertResult("0.125, 0.375, 0.625, 0.875") {
      result.map(p => p.get(0).getValue).mkString(", ")
    }
  }

  test("2d sampling - 4 samples requested") {
    val params = new NumericParameterArray(
      Array(0.5, 0.4),
      Array(0.0, 0.0),
      Array(1.0, 2.0),
      Array("p1", "p2"), new Random(1)
    )
    val sampler = new NumericGlobalSampler(params, 4)

    val result: Array[NumericParameterArray] = sampler.toArray

    assertResult("0.25 0.5 , 0.25 1.5 , 0.75 0.5 , 0.75 1.5 ") {
      result.map(p => p.asVector.toString()).mkString(", ")
    }
  }

  test("2d sampling - 9 samples requested") {
    val params = new NumericParameterArray(
      Array(0.5, 0.4),
      Array(0.0, 0.0),
      Array(1.0, 2.0),
      Array("p1", "p2"), new Random(1)
    )
    val sampler = new NumericGlobalSampler(params, 16)

    val result: Array[NumericParameterArray] = sampler.toArray

    assertResult("0.125 0.25 , 0.125 0.75 , 0.125 1.25 , 0.125 1.75 , 0.375 0.25 , 0.375 0.75 , " +
      "0.375 1.25 , 0.375 1.75 , 0.625 0.25 , 0.625 0.75 , 0.625 1.25 , 0.625 1.75 , 0.875 0.25 , " +
      "0.875 0.75 , 0.875 1.25 , 0.875 1.75 ") {
      result.map(p => p.asVector.toString()).mkString(", ")
    }
  }
}
