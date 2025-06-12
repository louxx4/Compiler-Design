package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.parser.type.JumpType;

public final class LoopJumpNode extends Node {
    private final JumpType type;
    public LoopJumpNode(JumpType type, Block block, Node... predecessors) {
        super(block, predecessors);
        this.type = type;
    }
}
