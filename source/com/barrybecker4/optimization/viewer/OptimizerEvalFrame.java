/** Copyright by Barry G. Becker, 2000-2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.viewer;

import com.barrybecker4.optimization.Optimizer;
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizee.optimizees.TrivialProblem;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;
import com.barrybecker4.ui.application.ApplicationFrame;

import javax.swing.*;
import javax.vecmath.Point2d;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Show iteration steps to the 2d solution.
 *
 * @author Barry Becker
 */
public class OptimizerEvalFrame extends ApplicationFrame implements ActionListener {

    private OptimizerEvalPanel evalPanel;
    private JComboBox strategyDropDown;
    private JComboBox testProblemDropDown;

    private OptimizeeProblem testProblem;
    private OptimizationStrategyType initialStrategy;
    private String logFile;

    /**
     * Constructor
     * @param logFile where logs will go
     */
    public OptimizerEvalFrame(String logFile, OptimizationStrategyType initialStrategy,
                              OptimizeeProblem testProblem) {

        this(logFile, initialStrategy, new OptimizeeProblem[] {testProblem});
    }

    /**
     * Constructor
     * @param logFile where logs will go
     */
    public OptimizerEvalFrame(String logFile, OptimizationStrategyType initialStrategy,
                              OptimizeeProblem[] testProblems) {

        super("Optimization Animation");
        this.logFile = logFile;
        this.setSize(OptimizerEvalPanel.SIZE);


        this.testProblem = testProblems[0];
        this.initialStrategy = initialStrategy;

        this.getContentPane().add(createContent(testProblems));
        this.showOptimization();

        this.pack();
        this.setVisible(true);
    }

    private JPanel createContent(OptimizeeProblem[] testProblems) {
        JPanel mainPanel = new JPanel(new BorderLayout());

        evalPanel = new OptimizerEvalPanel();

        mainPanel.add(createToolbar(testProblems), BorderLayout.NORTH);
        mainPanel.add(evalPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createToolbar(OptimizeeProblem[] testProblems) {
        JPanel toolbarPanel = new JPanel(new BorderLayout());

        NavigationBar navBar = new NavigationBar(evalPanel);

        JPanel strategyPanel = createStrategyCombo();
        if (testProblems.length > 1) {
            testProblemDropDown = new JComboBox<>(testProblems);
            testProblemDropDown.addActionListener(this);
            strategyPanel.add(testProblemDropDown);
        }

        toolbarPanel.add(navBar, BorderLayout.CENTER);
        toolbarPanel.add(strategyPanel, BorderLayout.EAST);

        return toolbarPanel;
    }

    private JPanel createStrategyCombo() {
        JPanel strategyPanel = new JPanel();

        strategyDropDown = new JComboBox<>(OptimizationStrategyType.values());
        strategyDropDown.setSelectedItem(initialStrategy);
        strategyPanel.add(strategyDropDown);

        strategyDropDown.addActionListener(this);
        return strategyPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == strategyDropDown) {
            System.out.println("changed strategy to " + strategyDropDown.getSelectedItem());
            showOptimization();
        }
        if (e.getSource() == testProblemDropDown) {
            testProblem = (OptimizeeProblem) testProblemDropDown.getSelectedItem();
            System.out.println("changed test problem to " + testProblem);
            showOptimization();
        }
    }

    public void showOptimization() {

        OptimizationStrategyType strategy = (OptimizationStrategyType) strategyDropDown.getSelectedItem();
        ParameterArray params = testProblem.getExactSolution();
        double xVal = params.get(0).getValue();
        double yVal = (params.size() > 1) ? params.get(1).getValue() : xVal;
        Point2d solutionPosition = new Point2d(xVal, yVal);

        Optimizer optimizer = new Optimizer(testProblem, logFile);
        optimizer.setListener(evalPanel);
        evalPanel.doTest(strategy, optimizer, solutionPosition,
                         testProblem.getInitialGuess(), testProblem.getFitnessRange());
        repaint();
    }

    /**
     * demonstrate with a trivial one parameter problem
     */
    public static void main(String[] args) {

        OptimizeeProblem testProblem = new TrivialProblem();
        OptimizationStrategyType strategy = OptimizationStrategyType.GLOBAL_SAMPLING;

        new OptimizerEvalFrame("test/temp.txt", strategy, testProblem);
    }
}
