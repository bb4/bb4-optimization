// Copyright by Barry G. Becker, 2000-2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer;

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizee.optimizees.TrivialProblem;
import com.barrybecker4.ui.application.ApplicationFrame;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Show iteration steps to the 2d solution.
 *
 * @author Barry Becker
 */
public class OptimizerEvalFrame extends ApplicationFrame {

    private String logFile;

    /**
     * Constructor
     * @param logFile where logs will go
     */
    public OptimizerEvalFrame(String logFile, OptimizeeProblem testProblem) {

        this(logFile,  new OptimizeeProblem[]{testProblem});
    }

    /**
     * Constructor
     * @param logFile where logs will go
     */
    public OptimizerEvalFrame(String logFile, OptimizeeProblem[] testProblems) {

        super("Optimization Animation");
        this.logFile = logFile;
        this.setSize(OptimizerEvalPanel.SIZE);

        this.getContentPane().add(createContent(testProblems));

        this.pack();
        this.setVisible(true);
    }

    private JPanel createContent(OptimizeeProblem[] testProblems) {
        JPanel mainPanel = new JPanel(new BorderLayout());

        OptimizerEvalPanel evalPanel = new OptimizerEvalPanel();
        TopControls topControls = new TopControls(logFile, testProblems, evalPanel);

        mainPanel.add(topControls, BorderLayout.NORTH);
        mainPanel.add(evalPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    /**
     * demonstrate with a trivial one parameter problem
     */
    public static void main(String[] args) {

        OptimizeeProblem testProblem = new TrivialProblem();

        new OptimizerEvalFrame("test/temp.txt",  testProblem);
    }
}
