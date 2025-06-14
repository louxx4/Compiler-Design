package edu.kit.kastel.vads.compiler.ir.node;

public final class IfEndNode extends Node {
    public IfEndNode(Block block, Node... predecessors) {
        super(block, predecessors);
    }
}
