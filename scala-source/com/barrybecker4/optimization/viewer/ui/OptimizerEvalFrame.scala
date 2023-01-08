// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.ui

import com.barrybecker4.optimization.optimizee.optimizees.{OptimizeeProblem, TrivialProblem}
import com.barrybecker4.optimization.viewer.ui.{OptimizerEvalPanel, TopControls}
import com.barrybecker4.ui.application.ApplicationFrame

import java.awt.BorderLayout
import javax.swing.JPanel


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

  def this(logFile: String, testProblem: OptimizeeProblem) = {
    this(logFile, IndexedSeq[OptimizeeProblem](testProblem))
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
