package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public final class IfNode extends Node {
    public IfNode(IrGraph graph) {
        super(graph);
    }

    public IfNode(Block block, Node... predecessors) {
        super(block, predecessors);
    }
}
