package edu.kit.kastel.vads.compiler.ir.node;

public final class JumpNode extends Node {
    public JumpNode(Block block, Node... predecessors) {
        super(block, predecessors);
    }
}
