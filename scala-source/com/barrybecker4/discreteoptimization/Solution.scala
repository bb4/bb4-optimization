package com.barrybecker4.discreteoptimization

object Solution {
  def main(args: Array[String]): Unit = {
    val input = scala.io.StdIn.readInt()
    val result = processInput(input)
    println(result)
  }

  def processInput(input: Int): Int = {
    // Process the input and return the result
    input + 1
  }
}