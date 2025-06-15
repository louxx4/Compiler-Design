package edu.kit.kastel.vads.compiler.ir.node;

public final class JumpNode extends Node {
    public static final int IN = 0;

    public JumpNode(Block block, Node... predecessors) {
        super(block, predecessors);
    }

    public static Node getPredecessor(JumpNode node) {
        if(node.predecessors().isEmpty()) {
            return node.block().predecessor(0);
        } else {
            return node.predecessor(JumpNode.IN);
        }
    }
}
