package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public final class JumpNode extends Node {
    public JumpNode(Block block, Node... predecessors) {
        super(block, predecessors);
    }

    public JumpNode(IrGraph graph) {
        super(graph);
    }
}
