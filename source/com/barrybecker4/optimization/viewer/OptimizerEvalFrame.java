/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.viewer;

import com.barrybecker4.optimization.Optimizer;
import com.barrybecker4.optimization.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.parameter.NumericParameterArray;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

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
public class OptimizerEvalFrame extends JFrame implements ActionListener {

    private OptimizerEvalPanel evalPanel;
    private JComboBox strategyDropDown;

    private Optimizer optimizer;
    private OptimizeeProblem testProblem;
    private OptimizationStrategyType initialStrategy;
    /**
     * Constructor
     * @param optimizer to show iterations of
     * @param solutionPosition  may be null if unknown.
     */
    public OptimizerEvalFrame(Optimizer optimizer, Point2d solutionPosition,
                              OptimizationStrategyType initialStrategy,
                              OptimizeeProblem testProblem) {

        this.setTitle("Optimization Animation of " + optimizer.getOptimizee().getName());
        this.setSize(OptimizerEvalPanel.SIZE);

        this.optimizer = optimizer;
        this.testProblem = testProblem;
        this.initialStrategy = initialStrategy;
        this.testProblem = testProblem;

        this.getContentPane().add(createContent(solutionPosition));
        this.doTest(initialStrategy);

        this.pack();
        this.setVisible(true);
    }

    private JPanel createContent(Point2d solutionPosition) {
        JPanel mainPanel = new JPanel(new BorderLayout());

        evalPanel = new OptimizerEvalPanel(solutionPosition);
        optimizer.setListener(evalPanel);

        mainPanel.add(createToolbar(), BorderLayout.NORTH);
        mainPanel.add(evalPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createToolbar() {
        JPanel toolbarPanel = new JPanel(new BorderLayout());

        NavigationBar navBar = new NavigationBar(evalPanel);
        JPanel strategyPanel = createStrategyCombo();

        toolbarPanel.add(navBar, BorderLayout.CENTER);
        toolbarPanel.add(strategyPanel, BorderLayout.EAST);

        return toolbarPanel;
    }

    private JPanel createStrategyCombo() {
        JPanel strategyPanel = new JPanel();

        strategyDropDown = new JComboBox(OptimizationStrategyType.values());
        strategyDropDown.setSelectedItem(initialStrategy);
        strategyPanel.add(strategyDropDown);

        strategyDropDown.addActionListener(this);
        return strategyPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == strategyDropDown) {
            System.out.println("changed strategy to " + strategyDropDown.getSelectedItem());
            OptimizationStrategyType strategy = (OptimizationStrategyType) strategyDropDown.getSelectedItem();
            doTest(strategy);
        }
    }

    public void doTest(OptimizationStrategyType strategy) {

        evalPanel.doTest(strategy, optimizer, testProblem.getInitialGuess(), testProblem.getFitnessRange());
        repaint();
    }

    /**
     * demonstrate with a trivial one parameter problem
     */
    public static void main(String[] args) {

        final double SOLUTION_VALUE = 0.4;

        OptimizeeProblem testProblem = new OptimizeeProblem() {

            private final ParameterArray EXACT_SOLUTION =  new NumericParameterArray(
                        new double[] {SOLUTION_VALUE},
                        new double[] {0.0},
                        new double[] {1.0},
                    new String[] {"param1"});

            @Override
            public ParameterArray getExactSolution() {
                return EXACT_SOLUTION;
            }

            @Override
            public ParameterArray getInitialGuess() {
                return new NumericParameterArray(
                        new double[] {0.5},
                        new double[] {0.0},
                        new double[] {1.0},
                        new String[] {"param1"});
            }

            @Override
            public double getFitnessRange() {
                return 1.0;
            }

            @Override
            public String getName() {
                return "Trivial Test Problem";
            }

            @Override
            public boolean evaluateByComparison() {
                return false;
            }

            @Override
            public double evaluateFitness(ParameterArray params) {
                return 1 - EXACT_SOLUTION.distance(params);
            }

            @Override
            public double compareFitness(ParameterArray params1, ParameterArray params2) {
                return 0;
            }
        };

        Optimizer optimizer = new Optimizer(testProblem, "test/temp.txt");

        Point2d solutionPosition = new Point2d(SOLUTION_VALUE, SOLUTION_VALUE);
        OptimizationStrategyType strategy = OptimizationStrategyType.GLOBAL_SAMPLING;

        new OptimizerEvalFrame(optimizer, solutionPosition, strategy, testProblem);
    }
}
