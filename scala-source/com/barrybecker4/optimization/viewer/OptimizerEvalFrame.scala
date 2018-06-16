// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer

import java.awt.BorderLayout
import com.barrybecker4.optimization.optimizee.optimizees.{OptimizeeProblem, TrivialProblem}
import com.barrybecker4.ui.application.ApplicationFrame
import javax.swing.JPanel


object OptimizerEvalFrame {
  /**
    * demonstrate with a trivial one parameter problem
    */
  def main(args: Array[String]): Unit = {
    val testProblem = new TrivialProblem
    new OptimizerEvalFrame("test/temp.txt", testProblem)
  }
}

/**
  * Show iteration steps to the 2d solution.
  * @param logFile where logs will go
  * @author Barry Becker
  */
class OptimizerEvalFrame(var logFile: String, val testProblems: Seq[OptimizeeProblem])

  extends ApplicationFrame("Optimization Animation") {
  this.setSize(OptimizerEvalPanel.SIZE)
  this.getContentPane.add(createContent(testProblems))
  this.pack()
  this.setVisible(true)

  def this(logFile: String, testProblem: OptimizeeProblem) {
    this(logFile, Array[OptimizeeProblem](testProblem))
  }

  private def createContent(testProblems: Seq[OptimizeeProblem]) = {
    val mainPanel = new JPanel(new BorderLayout)
    val evalPanel = new OptimizerEvalPanel
    val topControls = new TopControls(logFile, testProblems, evalPanel)
    mainPanel.add(topControls, BorderLayout.NORTH)
    mainPanel.add(evalPanel, BorderLayout.CENTER)
    mainPanel
  }
}