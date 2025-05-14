package edu.kit.kastel.vads.compiler.backend.liveness;

import java.util.Set;

public class InterferenceGraph {

    // Adjacency list holds each node at index of their register id
    // and the nodes neighbours respectively as set of their register ids
    public final Set<Integer>[] adjacencyList;
    // Array of nodes with each node at index of its register id
    public final Node[] nodes;

    public InterferenceGraph(Set<Integer>[] adjacencyList, Node[] nodes) {
        this.adjacencyList = adjacencyList;
        this.nodes = nodes;
    }

}