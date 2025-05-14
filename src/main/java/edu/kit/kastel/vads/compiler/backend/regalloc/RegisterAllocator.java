package edu.kit.kastel.vads.compiler.backend.regalloc;

import java.util.List;
import java.util.PriorityQueue;

import edu.kit.kastel.vads.compiler.backend.liveness.InterferenceGraph;
import edu.kit.kastel.vads.compiler.backend.liveness.Node;

public class RegisterAllocator {

    // Calculates a simplical (S) elimination (E) ordering for 
    // a given intereference graph via maximum cardinality search.
    public static Node[] getSEOrdering(InterferenceGraph graph) {
        Node[] ordering = new Node[graph.nodes.length];
        // use priority queue to easily pop element with highest weight
        PriorityQueue<Node> priorityQ = new PriorityQueue<Node>();
        priorityQ.addAll(List.of(graph.nodes));
        // extract ordering
        for(int i = 0; i < graph.nodes.length; i++) {
            Node v = priorityQ.poll(); // node with highest weight
            ordering[i] = v;
            for(int neighbour : graph.adjacencyList[v.reg.id]) {
                graph.nodes[neighbour].incrWt(); //wt(neighbour)++
            }
        }
        return ordering;
    }
}
