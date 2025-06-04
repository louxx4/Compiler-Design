package edu.kit.kastel.vads.compiler.ir.node;

public final class EqualsNode extends BinaryOperationNode {
    
    public EqualsNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
