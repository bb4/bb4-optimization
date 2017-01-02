// Copyright by Barry G. Becker, 2000-2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer;

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Controls at the top of the OptimizerEvalFrame that allow setting the strategy and optimizee problem.
 *
 * @author Barry Becker
 */
public class TopControls extends JPanel implements ActionListener {

    private OptimizationViewable viewable;
    private JComboBox strategyDropDown;
    private JComboBox testProblemDropDown;
    private String logFile;
    private OptimizeeProblem testProblem;


    /**
     * Constructor
     */
    public TopControls(String logFile, OptimizeeProblem[] testProblems, OptimizationViewable viewable) {

        this.logFile = logFile;
        this.viewable = viewable;
        this.testProblem = testProblems[0];

        setLayout(new BorderLayout());

        NavigationBar navBar = new NavigationBar(viewable);

        JPanel comboPanel = createStrategyCombo();
        if (testProblems.length > 1) {
            testProblemDropDown = new JComboBox<>(testProblems);
            testProblemDropDown.addActionListener(this);
            comboPanel.add(testProblemDropDown);
        }

        add(navBar, BorderLayout.CENTER);
        add(comboPanel, BorderLayout.EAST);
        showOptimization();
    }

    private JPanel createStrategyCombo() {
        JPanel strategyPanel = new JPanel();

        strategyDropDown = new JComboBox<>(OptimizationStrategyType.values());
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
        viewable.showOptimization(strategy, testProblem, logFile);
    }
}
