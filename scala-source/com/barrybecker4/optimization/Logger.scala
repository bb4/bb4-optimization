// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.optimization.parameter.ParameterArray
import java.io.{File, FileWriter, IOException}
import Logger.SEPARATOR


object Logger {
  val SEPARATOR = ",\t"
}

/**
  * Logger for use with optimizer algorithms.
  * @author Barry Becker
  */
class Logger(var sLogFile: String) {
  assert(sLogFile != null)

  /** Create and init the log file.
    * @param params used to determine param names.
    */
  def initialize(params: ParameterArray): Unit = {
    try {
      createDirIfNeeded(sLogFile)

      // create the log file (destroying it if it already existed)
      val logFile = new FileWriter(sLogFile, false)
      logFile.write("iteration" + SEPARATOR)
      logFile.write("fitness" + SEPARATOR)
      logFile.write("jumpSize" + SEPARATOR)
      logFile.write("dotprod" + SEPARATOR)
      for (i <- 0 until params.size) {
        logFile.write(params.get(i).name + SEPARATOR)
      }
      logFile.write("comment ")
      logFile.write('\n')
      logFile.close()
    } catch {
      case ioe: IOException =>
        ioe.printStackTrace()
    }
  }

  private def createDirIfNeeded(sLogFile: String): Unit = {
    val parentDirName = sLogFile.substring(0, sLogFile.lastIndexOf("/"))
    val parentDir = new File(parentDirName)
    if (!parentDir.exists()) {
      try {
        val success = parentDir.mkdir()
        println(s"Creation of $parentDir was success = $success")
      }
      catch {
        case ioe: IOException => println("IOException creating " + parentDir + ". " + ioe.getMessage)
        case t: Throwable => throw new IllegalStateException(t)
      }
    }
  }

  /** Write a row to the file and close it again.
    * That way if we terminate, we still have something in the file.
    *
    * @param iteration the current iteration.
    * @param fitness   the current fitness level. Or increase if fitness if in comparison mode.
    * @param jumpSize  the distance we moved in parameter space since the last iteration.
    * @param params    the params to write.
    */
  final def write(iteration: Int, fitness: Double, jumpSize: Double,
    distance: Double, params: ParameterArray, comment: String): Unit = {
    val sep = SEPARATOR
    val rowText = s"""$iteration$sep${FormatUtil.formatNumber(fitness)}
      $sep${FormatUtil.formatNumber(jumpSize)}
      $sep${FormatUtil.formatNumber(distance)}
      $sep${params.toCSVString}$sep$comment"""
    if (sLogFile == null) {
      println("<no logfile>: " + rowText)
      return
    }
    try { // append to existing log file.
      val logFile = new FileWriter(sLogFile, true)
      logFile.write(rowText + '\n')
      logFile.flush()
      logFile.close()
    } catch {
      case ioe: IOException =>
        ioe.printStackTrace()
    }
  }
}
