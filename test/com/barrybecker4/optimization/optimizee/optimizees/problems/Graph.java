// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple undirected graph representation.
 * Only connections between nodes are recorded, not the distance between them.
 * The graph connections are defined by a list of lists.
 * Assume that each node in the graph is assigned an integer number.
 * Each list entry corresponds to a node with a number that matches the index in the array.
 * The list entry contains the numbers of the nodes that are connected to it.
 *
 * @author Barry Becker
 */
class Graph extends ArrayList<List<Integer>> {

    @SafeVarargs
    Graph(List<Integer>... nodeNeighbors) {
         super(Arrays.asList(nodeNeighbors));
    }

    /** @return the number of nodes that are more than one edge link away from the specified vertex */
    int getNumNotWithinOneHop(List<Integer> marked) {
        int total = 0;
        for (int i = 0; i < size(); i++) {
            if (!marked.contains(i)) {
                 total += isNodeOneHopAway(i, marked) ? 0 : 1;
            }
        }
        //System.out.println("out of " + size() + " nodes, " + total + " are not within one hop from " + marked);
        return total;
    }

    /**
     * @param i node to start searching from
     * @param marked list of marked nodes
     * @return true if node i is only one hop from one of the marked nodes
     */
    private boolean isNodeOneHopAway(int i, List<Integer> marked) {
        List<Integer> nbrs = get(i);
        for (int j : marked) {
            if (nbrs.contains(j)) {
                return true;
            }
        }
        return false;
    }
}
