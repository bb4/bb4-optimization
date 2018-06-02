// Copyright by Barry G. Becker, 2000-2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer1;

import com.barrybecker4.optimization.OptimizationListener1;
import com.barrybecker4.optimization.Optimizer1;
import com.barrybecker4.optimization.optimizee1.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.parameter1.ParameterArray;
import com.barrybecker4.optimization.strategy1.OptimizationStrategyType;
import com.barrybecker4.optimization.viewer1.model.PointsList;
import com.barrybecker4.optimization.viewer1.projectors.IProjector;
import com.barrybecker4.optimization.viewer1.projectors.SimpleProjector;

import javax.swing.JPanel;
import javax.vecmath.Point2d;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Panel for showing the optimization visually.
 * To zoom, click buttons at the top.
 * To pan, simply click and drag.
 * @author Barry Becker
 */
public class OptimizerEvalPanel extends JPanel
        implements OptimizationListener1, OptimizationViewable, MouseListener, MouseMotionListener {

    private static final int EDGE_SIZE = 1000;
    static final Dimension SIZE = new Dimension(EDGE_SIZE, EDGE_SIZE);
    private static final Color BG_COLOR = new Color(240, 245, 250);

    private PointsList pointsList;
    private PointsListRenderer renderer;

    private Point dragStartPosition;

    private IProjector projector = new SimpleProjector();

    /**
     * Constructor
     */
    public OptimizerEvalPanel() {

        this.setPreferredSize(SIZE);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        renderer = new PointsListRenderer();
    }

    public void doTest(OptimizationStrategyType optType, Optimizer1 optimizer, Point2d solutionPosition,
                       ParameterArray initialGuess, double fitnessRange) {

        pointsList = new PointsList(solutionPosition, EDGE_SIZE, projector);
        ParameterArray solution = null;
        try {
            solution = optimizer.doOptimization(optType, initialGuess, fitnessRange);
        } catch(AbstractMethodError e) {
            // allow continuing if the strategy has simply not been implemented yet.
            e.printStackTrace();
        }
        this.repaint();

        System.out.println( "\n************************************************************************" );
        System.out.println( "The solution to the ("
                + optimizer.getOptimizee().getName() + ") Polynomial Test Problem using "
                + optType + " is :\n" + solution);
        System.out.println( "Which evaluates to: "+ optimizer.getOptimizee().evaluateFitness(solution));
        System.out.println( "We expected to get exactly p1 = "+ solutionPosition.x
                + " and p2 = " + solutionPosition.y );
    }

    /**
     * Called whenever the optimizer strategy moves incrementally toward the solution.
     * @param params we assume there is only two.
     */
    public void optimizerChanged(ParameterArray params) {
        pointsList.addPoint(params);
    }

    /**
     * show the optimization results in the ui.
     */
    public void showOptimization(OptimizationStrategyType strategy, OptimizeeProblem testProblem, String logFile) {

        ParameterArray params = testProblem.getExactSolution();

        // have strategy for projecting n-dimensions down to two.
        Point2d solutionPosition = projector.project(params);

        Optimizer1 optimizer = new Optimizer1(testProblem, logFile);
        optimizer.setListener(this);

        doTest(strategy, optimizer, solutionPosition,
               testProblem.getInitialGuess(), testProblem.getFitnessRange());
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

    public void pan(Point2d offset) {
        pointsList.pan(offset);
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

    @Override
    public void mouseDragged(MouseEvent e) {
        doPan(e.getPoint());
    }

    @Override
    public void mouseMoved(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {

        dragStartPosition = e.getPoint();
        System.out.println("mouse pressed at "+ dragStartPosition);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        doPan(e.getPoint());
    }

    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    private void doPan(Point currentPos) {
        if (!dragStartPosition.equals(currentPos)) {
            double xOffset = (dragStartPosition.getX() - currentPos.getX()) / getWidth();
            double yOffset = (dragStartPosition.getY() - currentPos.getY()) / getHeight();
            Point2d offset = new Point2d(xOffset, yOffset);
            pointsList.pan(offset);
            this.repaint();
        }

        dragStartPosition = currentPos;
    }
}