package edu.kit.kastel.vads.compiler.ir.node;

public final class SmallerNode extends BinaryOperationNode {

    public SmallerNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
