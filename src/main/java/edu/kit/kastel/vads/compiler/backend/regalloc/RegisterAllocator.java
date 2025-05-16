package edu.kit.kastel.vads.compiler.backend.regalloc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import edu.kit.kastel.vads.compiler.backend.liveness.InterferenceGraph;
import edu.kit.kastel.vads.compiler.backend.liveness.Node;

public class RegisterAllocator {

    public static List<String> AVAILABLE_REGS = Arrays.asList("r12", "r13", "r14", "r15");

    public static String SPILLING_REG_1 = "r10";
    public static String SPILLING_REG_2 = "r11";

    // Sets concrete registers / stack positions at the TempRegs.
    // Returns the amount of spilled registers. 
    public static int performRegisterAllocation(InterferenceGraph graph) {
        int highestColor = applyGreedyColoring(graph, getSEOrdering(graph));
        if(highestColor == -1) return 0; // no registers needed
        // map available registers to lowest colors, then spill to stack
        int stackOffset = -8;
        int amountSpilled = 0;
        Register[] colorToReg = new Register[highestColor + 1];
        for(int i = 0; i <= highestColor; i++){
            if(i < AVAILABLE_REGS.size()) {
                colorToReg[i] = new Register(AVAILABLE_REGS.get(i));
            } else {
                colorToReg[i] = new Register(stackOffset);
                stackOffset -= 8;
                amountSpilled++;
            }
        }
        // set colors at nodes
        for(Node node : graph.nodes) {
            node.reg.setRegister(colorToReg[node.getColor()]);
        }
        return amountSpilled;
    }

    // Calculates a simplical (S) elimination (E) ordering for 
    // a given intereference graph via maximum cardinality search.
    private static Node[] getSEOrdering(InterferenceGraph graph) {
        Node[] ordering = new Node[graph.nodes.length];
        // use priority queue to easily pop element with highest weight
        PriorityQueue<Node> priorityQ = new PriorityQueue<>();
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

    // Applies greedy coloring to a list of nodes whose order is a 
    // simplicial elimination ordering
    private static int applyGreedyColoring(InterferenceGraph graph, Node[] orderedNodes) {
        int highestUsedColor = -1;
        for (Node node : orderedNodes) {
            int color = getLowestFreeColor(graph, node);
            highestUsedColor = Math.max(highestUsedColor, color);
            node.setColor(color);
        }
        return highestUsedColor;
    }

    private static int getLowestFreeColor(InterferenceGraph graph, Node node) {
        // get all used colors among neighbours of provided node
        Set<Integer> neighbours = graph.adjacencyList[node.reg.id]; 
        Set<Integer> usedColors = new HashSet<>();
        for(int neighbour : neighbours) {
            Node n = graph.nodes[neighbour];
            if(n.isColored()) usedColors.add(n.getColor());
        }
        // calculate lowest unused color (starting from 0)
        int lowestFreeColor = 0;
        while(usedColors.contains(lowestFreeColor)) lowestFreeColor++;
        return lowestFreeColor;
    }
}
