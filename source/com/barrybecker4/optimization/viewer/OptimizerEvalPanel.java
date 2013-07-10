/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.viewer;

import com.barrybecker4.optimization.OptimizationListener;
import com.barrybecker4.optimization.Optimizer;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;
import com.barrybecker4.optimization.viewer.model.PointsList;

import javax.swing.*;
import javax.vecmath.Point2d;
import java.awt.*;

/**
 * Panel for showing the optimization visually.
 * TODO: add pan and zoom capability based on right click menu.
 * @author Barry Becker
 */
public class OptimizerEvalPanel extends JPanel implements OptimizationListener, NavigationListener {

    private static final int EDGE_SIZE = 1000;
    static final Dimension SIZE = new Dimension(EDGE_SIZE, EDGE_SIZE);
    private static final Color BG_COLOR = new Color(240, 241, 242);

    private Point2d solutionPosition;
    private PointsList pointsList;
    private PointsListRenderer renderer;

    /**
     * Constructor
     * @param solutionPosition where we hope to wind up at.
     */
    public OptimizerEvalPanel(Point2d solutionPosition) {
        this.solutionPosition = solutionPosition;

        this.setPreferredSize(SIZE);
        renderer = new PointsListRenderer();
    }

    public void doTest(OptimizationStrategyType optType, Optimizer optimizer,
                       ParameterArray initialGuess, double fitnessRange) {

        pointsList = new PointsList(solutionPosition, EDGE_SIZE);
        ParameterArray solution = null;
        try {
            solution = optimizer.doOptimization(optType, initialGuess, fitnessRange);
        } catch(AbstractMethodError e) {
            // allow continuing if the strategy has simply not been implemented yet.
            e.printStackTrace();
        }

        System.out.println( "\n************************************************************************" );
        System.out.println( "The solution to the ("
                + optimizer.getOptimizee().getName() + ") Polynomial Test Problem using "
                + optType + " is :\n" + solution);
        System.out.println( "Which evaluates to: "+ optimizer.getOptimizee().evaluateFitness(solution));
        System.out.println( "We expected to get exactly p1 = "+ solutionPosition.x
                + " and p2 = " + solutionPosition.x );
    }

    /**
     * Called whenever the optimizer strategy moves incrementally toward the solution.
     * @param params we assume there is only two.
     */
    public void optimizerChanged(ParameterArray params) {
        pointsList.addPoint(params);
    }

    @Override
    public void paintComponent(Graphics  g) {

        super.paintComponents( g );

        Graphics2D g2 = (Graphics2D) g;
        Dimension dim = this.getSize();
        g2.setColor(BG_COLOR);
        g2.fillRect(0, 0, (int) dim.getWidth(), (int) dim.getHeight());

        renderer.render(pointsList, g2);
    }

    public void pan(Direction direction) {
        pointsList.pan(direction);
        repaint();
    }

    public void zoomIn() {
        pointsList.zoomIn();
        repaint();
    }

    public void zoomOut() {
        pointsList.zoomOut();
        repaint();
    }
}