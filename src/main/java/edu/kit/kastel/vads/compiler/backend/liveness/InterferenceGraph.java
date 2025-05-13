package edu.kit.kastel.vads.compiler.backend.liveness;

import java.util.Set;

public class InterferenceGraph {

    public final Set<Integer>[] adjacencyList;
    public final Node[] nodes;

    public InterferenceGraph(Set<Integer>[] adjacencyList, Node[] nodes) {
        this.adjacencyList = adjacencyList;
        this.nodes = nodes;
    }

}