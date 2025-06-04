package edu.kit.kastel.vads.compiler.ir.node;

public final class NotBWNode extends UnaryOperationNode {

    public NotBWNode(Block block, Node right) {
        super(block, right);
    }
}
