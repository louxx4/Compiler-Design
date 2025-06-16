package edu.kit.kastel.vads.compiler.ir.node;

public final class JumpNode extends Node {
    public static final int IN = 0;

    public JumpNode(Block block, Node... predecessors) {
        super(block, predecessors);
    }

    public boolean isPureJump() {
        return (this.predecessors().isEmpty());
    }
}
