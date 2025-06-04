package edu.kit.kastel.vads.compiler.ir.node;

public final class NotNode extends UnaryOperationNode {

    public NotNode(Block block, Node right) {
        super(block, right);
    }
}
